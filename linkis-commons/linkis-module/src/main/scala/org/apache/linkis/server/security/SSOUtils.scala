/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.linkis.server.security

import org.apache.linkis.common.conf.Configuration
import org.apache.linkis.common.utils.{Logging, RSAUtils, Utils}
import org.apache.linkis.server.conf.{ServerConfiguration, SessionHAConfiguration}
import org.apache.linkis.server.exception.{
  IllegalUserTicketException,
  LoginExpireException,
  NonLoginException
}
import org.apache.linkis.server.ticket.UserTicketService

import org.apache.commons.lang3.time.DateFormatUtils

import javax.servlet.http.Cookie

import java.util.concurrent.{ConcurrentHashMap, TimeUnit}
import java.util.concurrent.atomic.AtomicLong

import scala.collection.JavaConverters._

import com.google.common.cache.{
  Cache,
  CacheBuilder,
  RemovalCause,
  RemovalListener,
  RemovalNotification
}

object SSOUtils extends Logging {

  private[security] val USER_TICKET_ID_STRING =
    ServerConfiguration.LINKIS_SERVER_SESSION_TICKETID_KEY.getValue

  private val sessionTimeout = ServerConfiguration.BDP_SERVER_WEB_SESSION_TIMEOUT.getValue.toLong

  val sslEnable: Boolean = ServerConfiguration.BDP_SERVER_SECURITY_SSL.getValue

  /**
   * Storage abstraction over the session ticket container.
   *
   * Keeps the read/write semantics of the legacy `ConcurrentHashMap` while allowing a bounded Guava
   * cache to be swapped in behind the same surface. See design §3.2 / ADR-003.
   */
  private[security] trait SessionTicketStore {

    def put(ticketId: String, lastAccessTime: Long): Unit

    def contains(ticketId: String): Boolean

    def get(ticketId: String): Option[Long]

    def remove(ticketId: String): Unit

    /** Read-only copy of the current entries (ticketId -> lastAccessTime). */
    def snapshot: Seq[(String, Long)]

    def backendSize: Long

  }

  private[security] class ConcurrentMapTicketStore(map: ConcurrentHashMap[String, Long])
      extends SessionTicketStore {

    override def put(ticketId: String, lastAccessTime: Long): Unit =
      map.put(ticketId, lastAccessTime)

    override def contains(ticketId: String): Boolean = map.containsKey(ticketId)

    override def get(ticketId: String): Option[Long] =
      if (map.containsKey(ticketId)) Option(map.get(ticketId)) else None

    override def remove(ticketId: String): Unit = map.remove(ticketId)

    override def snapshot: Seq[(String, Long)] = map.asScala.toVector

    override def backendSize: Long = map.size().toLong

  }

  // Guava's bounds are declared on java.lang.Long, and scala.Long is not the same type there,
  // so the cache is parameterised with java.lang.Long and boxed/unboxed at the boundary.
  private[security] class GuavaCacheTicketStore(cache: Cache[String, java.lang.Long])
      extends SessionTicketStore {

    override def put(ticketId: String, lastAccessTime: Long): Unit =
      cache.put(ticketId, java.lang.Long.valueOf(lastAccessTime))

    override def contains(ticketId: String): Boolean = cache.getIfPresent(ticketId) != null

    override def get(ticketId: String): Option[Long] =
      Option(cache.getIfPresent(ticketId)).map(_.longValue())

    override def remove(ticketId: String): Unit = cache.invalidate(ticketId)

    override def snapshot: Seq[(String, Long)] =
      cache.asMap().asScala.map { case (k, v) => (k, v.longValue()) }.toVector

    override def backendSize: Long = cache.size()

  }

  /** Default capacity used when `cache.max.size` is misconfigured (<= 0). */
  private val DEFAULT_MAX_SIZE = 100000

  /** Minimum gap between two size-eviction warn logs (FR-SS-006). */
  private val WARN_INTERVAL_MS = 60 * 1000L

  private val evictionCounter = new AtomicLong(0)

  private val lastWarnTs = new AtomicLong(0)

  def getUserTicketIdMap: ConcurrentHashMap[String, Long] = {
    if (SessionHAConfiguration.SsoRedis) {
      return new UserTicketService
    }
    new ConcurrentHashMap[String, Long]
  }

  private def buildTicketStore(): SessionTicketStore = {
    if (SessionHAConfiguration.SsoRedis) {
      // Redis branch is untouched by this feature: TTL/capacity stay under UserTicketService.
      new ConcurrentMapTicketStore(getUserTicketIdMap)
    } else if (ServerConfiguration.SESSION_TICKET_CACHE_BOUNDED_ENABLE.getValue) {
      // The annotation is required: tryCatch would otherwise narrow the success branch to
      // GuavaCacheTicketStore and reject the ConcurrentHashMap fallback.
      Utils.tryCatch[SessionTicketStore] {
        val configuredMaxSize = ServerConfiguration.SESSION_TICKET_CACHE_MAX_SIZE.getValue
        val maxSize = if (configuredMaxSize <= 0) {
          logger.warn(
            s"illegal session ticket cache max size $configuredMaxSize, fallback to $DEFAULT_MAX_SIZE."
          )
          DEFAULT_MAX_SIZE
        } else configuredMaxSize
        val builder = CacheBuilder
          .newBuilder()
          .maximumSize(maxSize.toLong)
          .recordStats()
          .removalListener(new RemovalListener[String, java.lang.Long] {
            override def onRemoval(
                notification: RemovalNotification[String, java.lang.Long]
            ): Unit = {
              // Only SIZE evictions signal capacity pressure; EXPIRED/EXPLICIT/REPLACED are normal.
              if (notification.getCause == RemovalCause.SIZE) {
                val total = evictionCounter.incrementAndGet()
                val now = System.currentTimeMillis
                val last = lastWarnTs.get()
                if (now - last >= WARN_INTERVAL_MS && lastWarnTs.compareAndSet(last, now)) {
                  logger.warn(
                    s"session ticket cache reached maximumSize, evicted $total tickets in total; " +
                      "consider increasing linkis.server.session.ticket.cache.max.size."
                  )
                }
              }
            }
          })
        // Test mode keeps the capacity bound but skips time expiry, so "expired" is never
        // rejected (ADR-004 / AC-7).
        if (!Configuration.IS_TEST_MODE.getValue) {
          builder.expireAfterAccess(sessionTimeout, TimeUnit.MILLISECONDS)
        }
        new GuavaCacheTicketStore(builder.build[String, java.lang.Long]())
      } { t =>
        logger.error(
          "failed to build bounded session ticket cache, fallback to ConcurrentHashMap",
          t
        )
        new ConcurrentMapTicketStore(new ConcurrentHashMap[String, Long])
      }
    } else {
      // Default path: byte-for-byte the previous behaviour.
      new ConcurrentMapTicketStore(getUserTicketIdMap)
    }
  }

  private val ticketStore: SessionTicketStore = buildTicketStore()

  /** True only when the bounded cache is actually in use (not SsoRedis, not fallback). */
  private def isBoundedCacheActive: Boolean =
    !SessionHAConfiguration.SsoRedis &&
      ServerConfiguration.SESSION_TICKET_CACHE_BOUNDED_ENABLE.getValue &&
      ticketStore.isInstanceOf[GuavaCacheTicketStore]

  def decryptLogin(passwordString: String): String = if (sslEnable) {
    new String(RSAUtils.decrypt(passwordString), Configuration.BDP_ENCODING.getValue)
  } else passwordString

  // The periodic sweeper is only useful for the local ConcurrentHashMap path. Two cases skip it:
  //   1) bounded Guava cache: it evicts internally, so the job would just duplicate work;
  //   2) SsoRedis: TTL is owned by UserTicketService/Redis, and iterating `snapshot` there means
  //      a full remote scan on every cycle -- a pure regression. The Redis branch must stay
  //      exactly as it was before this change (no local sweep at all).
  if (!isBoundedCacheActive && !SessionHAConfiguration.SsoRedis) {
    Utils.defaultScheduler.scheduleAtFixedRate(
      new Runnable {

        override def run(): Unit = Utils.tryCatch {
          ticketStore.snapshot
            .filter(System.currentTimeMillis - _._2 > sessionTimeout)
            .foreach { case (k, v) =>
              if (ticketStore.contains(k)) {
                val lastAccessTime = ticketStore.get(k)
                if (lastAccessTime.exists(System.currentTimeMillis - _ > sessionTimeout)) {
                  logger.info(
                    s"remove timeout userTicket $k, since the last access time is ${DateFormatUtils
                      .format(v, "yyyy-MM-dd HH:mm:ss")}."
                  )
                  ticketStore.remove(k)
                }
              }
            }
        } { t =>
          logger.error("failed to do remove", t)
        }

      },
      sessionTimeout,
      sessionTimeout / 10,
      TimeUnit.MILLISECONDS
    )
  }

  private[security] def getUserAndLoginTime(userTicketId: String): Option[(String, Long)] = {
    ServerConfiguration.getUsernameByTicket(userTicketId).map { userAndLoginTime =>
      {
        if (userAndLoginTime.indexOf(",") < 0) {
          throw new IllegalUserTicketException(s"Illegal user token information(非法的用户token信息).")
        }
        val index = userAndLoginTime.lastIndexOf(",")
        (userAndLoginTime.substring(0, index), userAndLoginTime.substring(index + 1).toLong)
      }
    }
  }

  // Determine the unique ID by username and timestamp(通过用户名和时间戳，确定唯一ID)
  private def getUserTicketId(username: String): String = {
    val timeoutUser = username + "," + System.currentTimeMillis
    ServerConfiguration.getTicketByUsername(timeoutUser)
  }

  def setLoginUser(
      addCookie: Cookie => Unit,
      username: String,
      updateSession: Boolean = true
  ): Unit = {
    logger.info(s"add login userTicketCookie for user $username.")
    val userTicketId = getUserTicketId(username)
    if (updateSession) {
      ticketStore.put(userTicketId, System.currentTimeMillis())
    }
    val cookie = new Cookie(USER_TICKET_ID_STRING, userTicketId)
    cookie.setMaxAge(-1)
    if (sslEnable) cookie.setSecure(true)
    cookie.setPath("/")
    addCookie(cookie)
  }

  def setLoginUser(addUserTicketKV: (String, String) => Unit, username: String): Unit = {
    logger.info(s"add login userTicket for user $username.")
    val userTicketId = getUserTicketKV(username)
    ticketStore.put(userTicketId._2, System.currentTimeMillis())
    addUserTicketKV(userTicketId._1, userTicketId._2)
  }

  private[linkis] def getUserTicketKV(username: String): (String, String) = {
    val userTicketId = getUserTicketId(username)
    (USER_TICKET_ID_STRING, userTicketId)
  }

  def removeLoginUser(getCookies: => Array[Cookie]): Unit = {
    val cookies = getCookies
    if (cookies != null) cookies.find(_.getName == USER_TICKET_ID_STRING).foreach { cookie =>
      if (ticketStore.contains(cookie.getValue)) {
        ticketStore.remove(cookie.getValue)
      }
      cookie.setValue(null)
      cookie.setMaxAge(0)
    }
    ProxyUserSSOUtils.removeProxyUser(cookies)
  }

  def removeLoginUserByAddCookie(addEmptyCookie: Cookie => Unit): Unit = {
    val cookie = new Cookie(USER_TICKET_ID_STRING, null)
    cookie.setMaxAge(0)
    cookie.setPath("/")
    if (sslEnable) cookie.setSecure(true)
    addEmptyCookie(cookie)
    ProxyUserSSOUtils.removeLoginUserByAddCookie(addEmptyCookie)
  }

  def removeLoginUser(removeKeyReturnValue: String => Option[String]): Unit =
    removeKeyReturnValue(USER_TICKET_ID_STRING).foreach { t =>
      if (ticketStore.contains(t)) {
        ticketStore.remove(t)
      }
    }

  def getLoginUsername(getCookies: => Array[Cookie]): String = getLoginUser(getCookies).getOrElse(
    throw new NonLoginException(s"You are not logged in, please login first(您尚未登录，请先登录!)")
  )

  def getLoginUser(getCookies: => Array[Cookie]): Option[String] = getLoginUser(_ =>
    Option(getCookies).flatMap(_.find(_.getName == USER_TICKET_ID_STRING).map(_.getValue))
  )

  def getLoginUser(getUserTicketId: String => Option[String]): Option[String] =
    getUserTicketId(USER_TICKET_ID_STRING).map { t =>
      isTimeoutOrNot(t)
      getUserAndLoginTime(t)
        .getOrElse(
          throw new IllegalUserTicketException(s"Illegal user token information(非法的用户token信息).")
        )
        ._1
    }

  def getLoginUsername(getUserTicketId: String => Option[String]): String =
    getLoginUser(getUserTicketId).getOrElse(
      throw new NonLoginException(s"You are not logged in, please login first(您尚未登录，请先登录!)")
    )

  private[security] def getLoginUserIgnoreTimeout(
      getUserTicketId: String => Option[String]
  ): Option[String] =
    getUserTicketId(USER_TICKET_ID_STRING).map(getUserAndLoginTime).flatMap(_.map(_._1))

  def updateLastAccessTime(getCookies: => Array[Cookie]): Unit = updateLastAccessTime(_ =>
    Option(getCookies).flatMap(_.find(_.getName == USER_TICKET_ID_STRING).map(_.getValue))
  )

  def updateLastAccessTime(getUserTicketId: String => Option[String]): Unit =
    getUserTicketId(USER_TICKET_ID_STRING).foreach(isTimeoutOrNot)

  @throws(classOf[LoginExpireException])
  private def isTimeoutOrNot(userTicketId: String): Unit =
    if (!ticketStore.contains(userTicketId)) {
      throw new LoginExpireException("You are not logged in, please login first!(您尚未登录，请先登录!)")
    } else {
      ticketStore.get(userTicketId) match {
        case Some(lastAccessTime) =>
          val now = System.currentTimeMillis
          if (now - lastAccessTime > sessionTimeout && !Configuration.IS_TEST_MODE.getValue) {
            // Double-check before removing: the entry may have been renewed concurrently.
            ticketStore.get(userTicketId) match {
              case Some(renewed) if now - renewed > sessionTimeout =>
                ticketStore.remove(userTicketId)
                throw new LoginExpireException(
                  "Login has expired, please log in again!(登录已过期，请重新登录！)"
                )
              case _ =>
            }
          } else if (now - lastAccessTime >= sessionTimeout * 0.5) {
            ticketStore.put(userTicketId, now)
          }
        case None =>
          // Evicted between contains() and get(); treat as not logged in instead of NPE.
          throw new LoginExpireException("You are not logged in, please login first!(您尚未登录，请先登录!)")
      }
    }

  def getSessionTimeOut(): Long = sessionTimeout

}
