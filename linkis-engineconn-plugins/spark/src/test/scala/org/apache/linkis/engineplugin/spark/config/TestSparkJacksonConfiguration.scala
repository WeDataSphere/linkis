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

package org.apache.linkis.engineplugin.spark.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Test for Spark Jackson StreamReadConstraints configuration defaults. Covers TC-P-001 / TC-P-002
 * of the Jackson overlong-string handling test plan (DPMS 534335).
 */
class TestSparkJacksonConfiguration {

  @Test
  def testJacksonMaxStringLengthDefault(): Unit = {
    val maxStringLength = SparkConfiguration.JACKSON_MAX_STRING_LENGTH.getValue
    assertEquals(
      10000000,
      maxStringLength,
      "JACKSON_MAX_STRING_LENGTH should default to 10,000,000"
    )
  }

  @Test
  def testJacksonMaxNestingDepthDefault(): Unit = {
    val maxNestingDepth = SparkConfiguration.JACKSON_MAX_NESTING_DEPTH.getValue
    assertEquals(2000, maxNestingDepth, "JACKSON_MAX_NESTING_DEPTH should default to 2,000")
  }

}
