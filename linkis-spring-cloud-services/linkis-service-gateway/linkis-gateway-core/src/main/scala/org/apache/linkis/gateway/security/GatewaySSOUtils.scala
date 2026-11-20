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

package org.apache.linkis.gateway.security

import org.apache.linkis.common.utils.{Logging, Utils}
import org.apache.linkis.gateway.config.GatewayConfiguration
import org.apache.linkis.gateway.http.{GatewayContext, GatewayHttpRequest}
import org.apache.linkis.server.conf.ServerConfiguration
import org.apache.linkis.server.exception.LoginExpireException
import org.apache.linkis.server.security.{ServerSSOUtils, SSOUtils}
import org.apache.linkis.server.security.SecurityFilter._

import org.apache.commons.lang3.StringUtils

import javax.servlet.http.Cookie

import scala.collection.JavaConverters._

object GatewaySSOUtils extends Logging {

  private def getCookies(gatewayContext: GatewayContext): Array[Cookie] =
    gatewayContext.getRequest.getCookies.asScala.flatMap(_._2).toArray

  private val DOMAIN_REGEX = "[a-zA-Z][a-zA-Z0-9\\.]+".r
  private val IP_REGEX = "([^:]+):.+".r

  private val cookieDomainSetupSwitch =
    GatewayConfiguration.GATEWAY_COOKIE_DOMAIN_SETUP_SWITCH.getValue

  /**
   * "dss.com" -> "dss.com" "127.0.0.1" -> "127.0.0.1" "127.0.0.1:8080" -> "127.0.0.1"
   * @param host
   *   the Host in HttpRequest Headers
   * @return
   */
  def getCookieDomain(host: String, level: Int): String = host match {
    case DOMAIN_REGEX() =>
      val domains = host.split("\\.")
      val index =
        if (domains.length > level) level
        else if (domains.length == level) level - 1
        else domains.length
      if (index < 0) {
        return host
      }
      val parsedDomains = domains.takeRight(index)
      if (null == parsedDomains || parsedDomains.length < level) {
        return host
      }
      val domain = parsedDomains.mkString(".")
      if (domains.length >= level) "." + domain else domain
    case IP_REGEX(ip) => ip
    case _ => host
  }

  def getLoginUser(gatewayContext: GatewayContext): Option[String] = {
    val cookies = getCookies(gatewayContext)
    Utils.tryCatch(SSOUtils.getLoginUser(cookies)) {
      case _: LoginExpireException
          if Option(cookies).exists(
            _.exists(c => c.getName == ALLOW_ACCESS_WITHOUT_TIMEOUT && c.getValue == "true")
          ) =>
        ServerSSOUtils
          .getLoginUserIgnoreTimeout(key =>
            Option(cookies).flatMap(_.find(_.getName == key).map(_.getValue))
          )
          .filter(_ != OTHER_SYSTEM_IGNORE_UM_USER)
      case t => throw t
    }
  }

  def getLoginUsername(gatewayContext: GatewayContext): String =
    SSOUtils.getLoginUsername(getCookies(gatewayContext))

  def setLoginUser(gatewayContext: GatewayContext, username: String): Unit = {
    val proxyUser = ProxyUserUtils.getProxyUser(username)
    SSOUtils.setLoginUser(
      c => {
        if (cookieDomainSetupSwitch) {
          val host = gatewayContext.getRequest.getHeaders.get("Host")
          if (host != null && host.nonEmpty) {
            c.setDomain(
              getCookieDomain(host.head, GatewayConfiguration.GATEWAY_DOMAIN_LEVEL.getValue)
            )
          }
        }
        gatewayContext.getResponse.addCookie(c)
      },
      proxyUser
    )
  }

  /** As [[setLoginUser]] but the minted ticket is tagged as token-authentication origin. */
  def setLoginUserFromToken(gatewayContext: GatewayContext, username: String): Unit = {
    val proxyUser = ProxyUserUtils.getProxyUser(username)
    SSOUtils.setLoginUserFromToken(
      (cookieName, ticketId) => {
        val c = new Cookie(cookieName, ticketId)
        c.setMaxAge(-1)
        c.setPath("/")
        if (cookieDomainSetupSwitch) {
          val host = gatewayContext.getRequest.getHeaders.get("Host")
          if (host != null && host.nonEmpty) {
            c.setDomain(
              getCookieDomain(host.head, GatewayConfiguration.GATEWAY_DOMAIN_LEVEL.getValue)
            )
          }
        }
        gatewayContext.getResponse.addCookie(c)
      },
      proxyUser
    )
  }

  def setLoginUser(
      request: GatewayHttpRequest,
      username: String,
      updateSession: Boolean = true
  ): Unit = {
    val proxyUser = ProxyUserUtils.getProxyUser(username)
    SSOUtils.setLoginUser(c => request.addCookie(c.getName, Array(c)), proxyUser, updateSession)
  }

  /** As [[setLoginUser]] but the minted ticket is tagged as token-authentication origin. */
  def setLoginUserFromToken(
      request: GatewayHttpRequest,
      username: String,
      updateSession: Boolean = true
  ): Unit = {
    val proxyUser = ProxyUserUtils.getProxyUser(username)
    SSOUtils.setLoginUserFromToken(
      (cookieName, ticketId) => {
        val c = new Cookie(cookieName, ticketId)
        c.setMaxAge(-1)
        c.setPath("/")
        request.addCookie(c.getName, Array(c))
      },
      proxyUser,
      updateSession
    )
  }

  /**
   * Discard the caller's login session and return the ticket id that was discarded, if the request
   * carried one. Returning it lets the logout response say which session ended; the id is read once
   * here because [[SSOUtils.removeLoginUser]] blanks the cookie it is given on the way out.
   */
  def removeLoginUser(gatewayContext: GatewayContext): Option[String] = {
    val cookies = getCookies(gatewayContext)
    val ticketCookieName = ServerConfiguration.LINKIS_SERVER_SESSION_TICKETID_KEY.getValue
    val discardedTicketId = Option(cookies)
      .flatMap(_.find(_.getName == ticketCookieName).map(_.getValue))
      .filter(StringUtils.isNotBlank)
    SSOUtils.removeLoginUser(cookies)
    SSOUtils.removeLoginUserByAddCookie(c => {
      if (cookieDomainSetupSwitch) {
        val host = gatewayContext.getRequest.getHeaders.get("Host")
        if (host != null && host.nonEmpty) {
          c.setDomain(
            getCookieDomain(host.head, GatewayConfiguration.GATEWAY_DOMAIN_LEVEL.getValue)
          )
        }
      }
      gatewayContext.getResponse.addCookie(c)
    })
    discardedTicketId
  }

  def updateLastAccessTime(gatewayContext: GatewayContext): Unit =
    SSOUtils.updateLastAccessTime(
      gatewayContext.getRequest.getCookies.asScala.flatMap(_._2).toArray
    )

}
