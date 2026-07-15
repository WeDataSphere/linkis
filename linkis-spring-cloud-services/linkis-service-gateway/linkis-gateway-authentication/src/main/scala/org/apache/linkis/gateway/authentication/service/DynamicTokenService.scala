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

package org.apache.linkis.gateway.authentication.service

/**
 * Dynamic Token Service - HMAC-SHA256 based token issuance and verification.
 *
 * Core responsibilities:
 *   1. Generate time-limited dynamic tokens (deterministic: same user + same window = same token)
 *      2. Validate dynamic tokens (algorithmic self-verification, no DB/cache dependency) 3.
 *      Multi-version key support for key rotation
 *
 * Design principles:
 *   - Zero storage: pure algorithm, no database/Redis operations
 *   - Deterministic: same input produces same output
 *   - Timing-attack resistant: constant-time signature comparison
 *
 * @author
 *   AI-assisted
 */
trait DynamicTokenService {

  /**
   * Issue a dynamic token.
   *
   * Core logic:
   *   1. Compute current 30-minute window start timestamp 2. Build signing input: username + ":" +
   *      windowStartTimestamp 3. Compute HMAC-SHA256 signature using active key 4. Assemble token:
   *      dyn-Base64(payload).Base64(signature)
   *
   * @param username
   *   the username extracted from existing token auth session
   * @return
   *   (token string, expiration time in seconds)
   * @throws org.apache.linkis.gateway.authentication.exception.TokenAuthException
   *   when HMAC key is not configured
   */
  def generateToken(username: String): (String, Long)

  /**
   * Validate a dynamic token.
   *
   * Core logic:
   *   1. Strip "dyn-" prefix 2. Split payload and signature by "." 3. Base64 decode payload ->
   *      extract username and time window 4. Defense-in-depth: Token-User must match embedded
   *      username 5. Check expiration (window start + configured expire days) 6. Re-compute
   *      HMAC-SHA256 signature and constant-time compare 7. Multi-version key fallback verification
   *
   * @param token
   *   the complete dynamic token string (with dyn- prefix)
   * @param tokenUser
   *   the Token-User from the request (for defense-in-depth check)
   * @return
   *   true if the token is valid
   * @throws org.apache.linkis.gateway.authentication.exception.TokenAuthException
   *   on format/signature/expiry/user mismatch errors
   */
  def validateDynamicToken(token: String, tokenUser: String): Boolean

}
