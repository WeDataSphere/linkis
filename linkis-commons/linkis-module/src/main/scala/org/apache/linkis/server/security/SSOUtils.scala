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

import scala.collection.JavaConverters._

object SSOUtils extends Logging {

  private[security] val USER_TICKET_ID_STRING =
    ServerConfiguration.LINKIS_SERVER_SESSION_TICKETID_KEY.getValue

  private val sessionTimeout = ServerConfiguration.BDP_SERVER_WEB_SESSION_TIMEOUT.getValue.toLong

  private val tokenTimeout =
    ServerConfiguration.SESSION_TICKET_TOKEN_TIMEOUT.getValue.toLong

  /** Whether tickets carry an origin tag and therefore independent lifetimes. */
  private val originEnabled =
    ServerConfiguration.SESSION_TICKET_ORIGIN_ENABLE.getValue

  /**
   * ticketId -> issue/last-refresh time, for tickets issued by token authentication only.
   *
   * Kept as a *side* table rather than folding the origin into the main map's value: under
   * `linkis.session.redis.cache.enabled=true` the main container is [[UserTicketService]], which
   * extends `ConcurrentHashMap[String, Long]` and overrides `put`/`get`/`containsKey`. Widening the
   * value type would break that subtype, so the origin is tracked separately and the main map's
   * representation stays byte-for-byte compatible.
   *
   * Only token-issued tickets are recorded; anything absent from this map is a browser ticket and
   * keeps the full `sessionTimeout`. The timestamps are what bound it: under SsoRedis the main
   * container keeps every entry in Redis and its local map stays empty, so a sweep that walks the
   * main container can never reach a token ticket. The sweep therefore expires this side table
   * directly (see [[expireTokenTickets]]); otherwise it would grow by one entry per token request
   * forever -- the very OOM this feature exists to remove.
   */
  private val tokenTicketIssuedAt = new ConcurrentHashMap[String, Long]()

  private val userTicketIdToLastAccessTime = getUserTicketIdMap

  val sslEnable: Boolean = ServerConfiguration.BDP_SERVER_SECURITY_SSL.getValue

  def getUserTicketIdMap: ConcurrentHashMap[String, Long] = {
    if (SessionHAConfiguration.SsoRedis) {
      return new UserTicketService
    }
    new ConcurrentHashMap[String, Long]
  }

  def decryptLogin(passwordString: String): String = if (sslEnable) {
    new String(RSAUtils.decrypt(passwordString), Configuration.BDP_ENCODING.getValue)
  } else passwordString

  /**
   * Lifetime that applies to `ticketId`.
   *
   * Token-issued tickets expire on `tokenTimeout`; everything else keeps the browser session
   * lifetime. When the feature is off every ticket reports `sessionTimeout`, so the legacy
   * behaviour is bit-for-bit reproduced.
   */
  private def timeoutOf(ticketId: String): Long = {
    if (!originEnabled) sessionTimeout
    else if (tokenTicketIssuedAt.containsKey(ticketId)) tokenTimeout
    else sessionTimeout
  }

  /** Forget the origin tag once the ticket itself is gone, so the side table cannot leak. */
  private def forgetOrigin(ticketId: String): Unit = {
    if (originEnabled) tokenTicketIssuedAt.remove(ticketId)
  }

  /**
   * Record that `ticketId` was minted by token authentication.
   *
   * Called right after the ticket is put into the main container. No-op when the feature is off.
   */
  private[security] def markTokenTicket(ticketId: String): Unit = {
    if (originEnabled) tokenTicketIssuedAt.put(ticketId, System.currentTimeMillis())
  }

  /** Keep the side table in step with the sliding refresh applied to the main container. */
  private def touchTokenTicket(ticketId: String, now: Long): Unit = {
    if (originEnabled && tokenTicketIssuedAt.containsKey(ticketId)) {
      tokenTicketIssuedAt.put(ticketId, now)
    }
  }

  /**
   * Expire token-issued tickets: drop them from the side table and from the main container.
   *
   * Driven by the side table rather than by the main container on purpose. Under SsoRedis the main
   * container is [[UserTicketService]]: `put` only writes Redis, so its local map is always empty
   * and iterating it never visits a token ticket. Sweeping the side table directly is the only way
   * to keep it bounded (and, on the Redis branch, the only thing that actually retires a 5m
   * ticket).
   */
  private[security] def expireTokenTickets(
      now: Long,
      issuedAt: ConcurrentHashMap[String, Long],
      onExpired: String => Unit
  ): Unit = {
    issuedAt.asScala.foreach { case (ticketId, issued) =>
      if (now - issued > tokenTimeout && issuedAt.remove(ticketId, issued)) {
        onExpired(ticketId)
      }
    }
  }

  // Sweep cadence. Token tickets expire on `tokenTimeout` (minutes), so a sweep that only runs
  // every `sessionTimeout / 10` (12 minutes when the session is 2h) would let them linger long
  // past their lifetime. Sweep on the shorter of the two lifetimes instead.
  private val sweepInterval =
    if (originEnabled) math.max(1000L, math.min(sessionTimeout, tokenTimeout) / 10)
    else sessionTimeout / 10

  Utils.defaultScheduler.scheduleAtFixedRate(
    new Runnable {

      override def run(): Unit = Utils.tryCatch {
        userTicketIdToLastAccessTime.asScala.foreach { case (k, v) =>
          if (userTicketIdToLastAccessTime.containsKey(k)) {
            // Resolve the lifetime once for this entry. Recomputing it after the ticket may have
            // been removed would let a token ticket fall back to the browser lifetime and get an
            // extra 2h of grace.
            val limit = timeoutOf(k)
            val now = System.currentTimeMillis
            val latest = userTicketIdToLastAccessTime.get(k)
            if (now - latest > limit) {
              logger.info(
                s"remove timeout userTicket $k, since the last access time is ${DateFormatUtils
                  .format(v, "yyyy-MM-dd HH:mm:ss")}."
              )
              userTicketIdToLastAccessTime.remove(k)
              forgetOrigin(k)
            }
          }
        }
        // Token tickets are expired from the side table, not from the iteration above: under
        // SsoRedis the main container holds nothing locally, so the loop above sees an empty map.
        if (originEnabled) {
          expireTokenTickets(
            System.currentTimeMillis,
            tokenTicketIssuedAt,
            ticketId => userTicketIdToLastAccessTime.remove(ticketId)
          )
        }
      } { t =>
        logger.error("failed to do remove", t)
      }

    },
    sweepInterval,
    sweepInterval,
    TimeUnit.MILLISECONDS
  )

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
      updateSession: Boolean = true,
      fromToken: Boolean = false
  ): Unit = {
    logger.info(s"add login userTicketCookie for user $username.")
    val userTicketId = getUserTicketId(username)
    if (updateSession) {
      userTicketIdToLastAccessTime.put(userTicketId, System.currentTimeMillis())
      if (fromToken) markTokenTicket(userTicketId)
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
    userTicketIdToLastAccessTime.put(userTicketId._2, System.currentTimeMillis())
    addUserTicketKV(userTicketId._1, userTicketId._2)
  }

  /**
   * Same as [[setLoginUser]] but records that the minted ticket came from token authentication, so
   * it carries `tokenTimeout` instead of the browser session lifetime.
   *
   * A distinct name rather than a defaulted parameter: Scala forbids two overloads of one method
   * that both declare default arguments.
   */
  def setLoginUserFromToken(
      addUserTicketKV: (String, String) => Unit,
      username: String,
      updateSession: Boolean = true
  ): Unit = {
    logger.info(s"add login userTicket for user $username.")
    val userTicketId = getUserTicketKV(username)
    if (updateSession) {
      userTicketIdToLastAccessTime.put(userTicketId._2, System.currentTimeMillis())
      markTokenTicket(userTicketId._2)
    }
    addUserTicketKV(userTicketId._1, userTicketId._2)
  }

  private[linkis] def getUserTicketKV(username: String): (String, String) = {
    val userTicketId = getUserTicketId(username)
    (USER_TICKET_ID_STRING, userTicketId)
  }

  def removeLoginUser(getCookies: => Array[Cookie]): Unit = {
    val cookies = getCookies
    if (cookies != null) cookies.find(_.getName == USER_TICKET_ID_STRING).foreach { cookie =>
      if (userTicketIdToLastAccessTime.containsKey(cookie.getValue)) {
        userTicketIdToLastAccessTime.remove(cookie.getValue)
        forgetOrigin(cookie.getValue)
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
      if (userTicketIdToLastAccessTime.containsKey(t)) {
        userTicketIdToLastAccessTime.remove(t)
        forgetOrigin(t)
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
    if (!userTicketIdToLastAccessTime.containsKey(userTicketId)) {
      throw new LoginExpireException("You are not logged in, please login first!(您尚未登录，请先登录!)")
    } else {
      // Token-issued tickets live on `tokenTimeout`; browser tickets keep `sessionTimeout`.
      val effectiveTimeout = timeoutOf(userTicketId)
      val lastAccessTime = userTicketIdToLastAccessTime.get(userTicketId)
      if (
          System.currentTimeMillis - lastAccessTime > effectiveTimeout && !Configuration.IS_TEST_MODE.getValue
      ) {
        if (
            userTicketIdToLastAccessTime.containsKey(
              userTicketId
            ) && System.currentTimeMillis - userTicketIdToLastAccessTime.get(
              userTicketId
            ) > effectiveTimeout
        ) {
          userTicketIdToLastAccessTime.remove(userTicketId)
          forgetOrigin(userTicketId)
          throw new LoginExpireException("Login has expired, please log in again!(登录已过期，请重新登录！)")
        }
      } else if (System.currentTimeMillis - lastAccessTime >= effectiveTimeout * 0.5) {
        val now = System.currentTimeMillis
        userTicketIdToLastAccessTime.put(userTicketId, now)
        touchTokenTicket(userTicketId, now)
      }
    }

  def getSessionTimeOut(): Long = sessionTimeout

}
