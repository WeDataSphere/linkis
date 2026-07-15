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

package org.apache.linkis.gateway.authentication.service.impl

import org.apache.linkis.common.utils.{Logging, Utils}
import org.apache.linkis.gateway.authentication.conf.DynamicTokenConfiguration
import org.apache.linkis.gateway.authentication.errorcode.LinkisGwAuthenticationErrorCodeSummary
import org.apache.linkis.gateway.authentication.errorcode.LinkisGwAuthenticationErrorCodeSummary._
import org.apache.linkis.gateway.authentication.exception.TokenAuthException
import org.apache.linkis.gateway.authentication.service.DynamicTokenService

import org.apache.commons.lang3.StringUtils

import org.springframework.stereotype.Service

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

import java.security.MessageDigest
import java.text.MessageFormat
import java.util.Base64

@Service
class DynamicTokenServiceImpl extends DynamicTokenService with Logging {

  private val HMAC_ALGORITHM = "HmacSHA256"
  private val TOKEN_SEPARATOR = "."
  private val PAYLOAD_SEPARATOR = ":"

  override def generateToken(username: String): (String, Long) = {
    Utils.tryCatch {
      val keys = getActiveKeys()
      if (keys.isEmpty) {
        throw new TokenAuthException(
          DYNAMIC_TOKEN_KEY_NOT_CONFIGURED.getErrorCode,
          DYNAMIC_TOKEN_KEY_NOT_CONFIGURED.getErrorDesc
        )
      }

      val windowStart = computeWindowStart(System.currentTimeMillis() / 1000)
      val payload = username + PAYLOAD_SEPARATOR + windowStart
      val signature = computeHmac(keys.head, payload)

      val token = DynamicTokenConfiguration.DYNAMIC_TOKEN_PREFIX +
        Base64.getUrlEncoder.withoutPadding().encodeToString(payload.getBytes("UTF-8")) +
        TOKEN_SEPARATOR +
        Base64.getUrlEncoder.withoutPadding().encodeToString(signature)

      val expireTime =
        windowStart + DynamicTokenConfiguration.DYNAMIC_TOKEN_EXPIRE_DAYS.getValue * 86400L

      logger.info(
        s"Generated dynamic token for user: $username, windowStart: $windowStart, " +
          s"expireTime: $expireTime, keyFingerprint: ${computeKeyFingerprint(keys.head)}"
      )
      (token, expireTime)
    } { t =>
      logger.error(s"Failed to generate dynamic token for user: $username", t)
      throw new TokenAuthException(
        DYNAMIC_TOKEN_SIGNATURE_INVALID.getErrorCode,
        s"Failed to generate dynamic token: ${t.getMessage}"
      )
    }
  }

  override def validateDynamicToken(token: String, tokenUser: String): Boolean = {
    if (!DynamicTokenConfiguration.DYNAMIC_TOKEN_ENABLED.getValue) {
      throw new TokenAuthException(
        DYNAMIC_TOKEN_NOT_ENABLED.getErrorCode,
        DYNAMIC_TOKEN_NOT_ENABLED.getErrorDesc
      )
    }

    // 1. Strip prefix
    val tokenBody = token.substring(DynamicTokenConfiguration.DYNAMIC_TOKEN_PREFIX.length)

    // 2. Split payload and signature
    val separatorIndex = tokenBody.indexOf(TOKEN_SEPARATOR)
    if (separatorIndex <= 0) {
      throw new TokenAuthException(
        DYNAMIC_TOKEN_FORMAT_ERROR.getErrorCode,
        DYNAMIC_TOKEN_FORMAT_ERROR.getErrorDesc
      )
    }

    val payloadEncoded = tokenBody.substring(0, separatorIndex)
    val signatureEncoded = tokenBody.substring(separatorIndex + 1)

    // 3. Decode payload
    val payload = Utils.tryCatch(new String(Base64.getUrlDecoder.decode(payloadEncoded), "UTF-8")) {
      t =>
        logger.warn(s"Failed to decode dynamic token payload: ${t.getMessage}")
        throw new TokenAuthException(
          DYNAMIC_TOKEN_FORMAT_ERROR.getErrorCode,
          DYNAMIC_TOKEN_FORMAT_ERROR.getErrorDesc
        )
    }

    val parts = payload.split(PAYLOAD_SEPARATOR, 2)
    if (parts.length != 2) {
      throw new TokenAuthException(
        DYNAMIC_TOKEN_FORMAT_ERROR.getErrorCode,
        DYNAMIC_TOKEN_FORMAT_ERROR.getErrorDesc
      )
    }

    val embeddedUsername = parts(0)
    val windowStart = Utils.tryCatch(parts(1).toLong) { t =>
      logger.warn(s"Failed to parse windowStart from payload: ${t.getMessage}")
      throw new TokenAuthException(
        DYNAMIC_TOKEN_FORMAT_ERROR.getErrorCode,
        DYNAMIC_TOKEN_FORMAT_ERROR.getErrorDesc
      )
    }

    // 4. Defense-in-depth: Token-User must match embedded username
    if (!StringUtils.equals(embeddedUsername, tokenUser)) {
      throw new TokenAuthException(
        DYNAMIC_TOKEN_USER_MISMATCH.getErrorCode,
        MessageFormat.format(DYNAMIC_TOKEN_USER_MISMATCH.getErrorDesc, embeddedUsername)
      )
    }

    // 5. Check expiration
    val currentTime = System.currentTimeMillis() / 1000
    val expireTime =
      windowStart + DynamicTokenConfiguration.DYNAMIC_TOKEN_EXPIRE_DAYS.getValue * 86400L
    if (currentTime >= expireTime) {
      throw new TokenAuthException(
        DYNAMIC_TOKEN_EXPIRED.getErrorCode,
        MessageFormat.format(
          DYNAMIC_TOKEN_EXPIRED.getErrorDesc,
          new java.util.Date(expireTime * 1000)
        )
      )
    }

    // 6. Decode signature
    val providedSignature = Utils.tryCatch(Base64.getUrlDecoder.decode(signatureEncoded)) { t =>
      logger.warn(s"Failed to decode signature: ${t.getMessage}")
      throw new TokenAuthException(
        DYNAMIC_TOKEN_FORMAT_ERROR.getErrorCode,
        DYNAMIC_TOKEN_FORMAT_ERROR.getErrorDesc
      )
    }

    // 7. Verify signature with clock skew tolerance and multi-version key fallback
    val keys = getAllKeys()
    val skewWindows = DynamicTokenConfiguration.DYNAMIC_TOKEN_CLOCK_SKEW_WINDOWS.getValue
    val windowMinutes = DynamicTokenConfiguration.DYNAMIC_TOKEN_WINDOW_MINUTES.getValue

    var signatureValid = false
    var triedKeyFingerprint = ""

    // Break out of nested loops when signature is valid
    var breakOuter = false
    for (skew <- -skewWindows to skewWindows if !breakOuter) {
      val adjustedWindowStart = windowStart + skew * windowMinutes * 60L
      val adjustedPayload = embeddedUsername + PAYLOAD_SEPARATOR + adjustedWindowStart

      for (key <- keys if !breakOuter) {
        val computedSignature = computeHmac(key, adjustedPayload)
        if (MessageDigest.isEqual(computedSignature, providedSignature)) {
          signatureValid = true
          triedKeyFingerprint = computeKeyFingerprint(key)
          breakOuter = true
        }
      }
    }

    if (!signatureValid) {
      throw new TokenAuthException(
        DYNAMIC_TOKEN_SIGNATURE_INVALID.getErrorCode,
        DYNAMIC_TOKEN_SIGNATURE_INVALID.getErrorDesc
      )
    }

    logger.debug(
      s"Dynamic token validation succeeded for user: $embeddedUsername, " +
        s"windowStart: $windowStart, keyFingerprint: $triedKeyFingerprint"
    )
    true
  }

  /**
   * Compute the time window start timestamp aligned to the configured window granularity.
   */
  private def computeWindowStart(timestampSec: Long): Long = {
    val windowSec = DynamicTokenConfiguration.DYNAMIC_TOKEN_WINDOW_MINUTES.getValue * 60L
    (timestampSec / windowSec) * windowSec
  }

  /**
   * Compute HMAC-SHA256 signature. Input format: username + ":" + windowStartTimestamp.
   */
  private def computeHmac(key: String, data: String): Array[Byte] = {
    val mac = Mac.getInstance(HMAC_ALGORITHM)
    val secretKey = new SecretKeySpec(key.getBytes("UTF-8"), HMAC_ALGORITHM)
    mac.init(secretKey)
    mac.doFinal(data.getBytes("UTF-8"))
  }

  /**
   * Get active keys (first one for signing, all for verification).
   */
  private def getActiveKeys(): Seq[String] = {
    val keyConfig = DynamicTokenConfiguration.DYNAMIC_TOKEN_HMAC_KEY.getValue
    if (StringUtils.isBlank(keyConfig)) Seq.empty
    else keyConfig.split(",").map(_.trim).filter(_.nonEmpty).toSeq
  }

  /**
   * Get all keys for verification (same as active keys for now).
   */
  private def getAllKeys(): Seq[String] = getActiveKeys()

  /**
   * Compute key fingerprint for logging (SHA-256 of key, not the key itself). Only the first 4
   * bytes of the digest are used for brevity.
   */
  private def computeKeyFingerprint(key: String): String = {
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(key.getBytes("UTF-8"))
    digest.take(4).map("%02x".format(_)).mkString
  }

}
