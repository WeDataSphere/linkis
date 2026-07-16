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

package org.apache.linkis.gateway.authentication.conf

import org.apache.linkis.common.conf.CommonVars

object DynamicTokenConfiguration {

  /**
   * Dynamic token feature switch. Default: false (disabled). Enable per cluster gradually.
   */
  val DYNAMIC_TOKEN_ENABLED: CommonVars[Boolean] =
    CommonVars("linkis.gateway.dynamic.token.enable", false)

  /**
   * HMAC shared secret key. Managed by DPM, config file uses placeholder [*key_*]. Supports
   * multi-version keys separated by comma: "key_v2,key_v1". First key is the active key (for
   * signing), rest are historical keys (for verification only).
   */
  val DYNAMIC_TOKEN_HMAC_KEY: CommonVars[String] =
    CommonVars("wds.linkis.gateway.dynamic.token.hmac.key", "")

  /**
   * Time window granularity in minutes. Default: 30 minutes. Tokens issued within the same window
   * are identical (deterministic generation).
   */
  val DYNAMIC_TOKEN_WINDOW_MINUTES: CommonVars[Int] =
    CommonVars("wds.linkis.gateway.dynamic.token.window.minutes", 30)

  /**
   * Token expiration in days from window start. Default: 1 day.
   */
  val DYNAMIC_TOKEN_EXPIRE_DAYS: CommonVars[Int] =
    CommonVars("wds.linkis.gateway.dynamic.token.expire.days", 1)

  /**
   * Clock skew tolerance in number of windows. 0 = only verify current window. 1 = verify current
   * window +- 1 window (3 windows total). Default: 1.
   */
  val DYNAMIC_TOKEN_CLOCK_SKEW_WINDOWS: CommonVars[Int] =
    CommonVars("wds.linkis.gateway.dynamic.token.clock.skew.windows", 1)

  /**
   * Dynamic token prefix for gateway routing recognition.
   */
  val DYNAMIC_TOKEN_PREFIX: String = "dyn-"

}
