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

package org.apache.linkis.engineplugin.spark.factory

import java.lang.reflect.{Field, Modifier}

import com.fasterxml.jackson.core.{JsonFactory, StreamReadConstraints}
import org.junit.jupiter.api.{AfterEach, Assertions, BeforeEach, Test}

/**
 * Verifies that SparkEngineConnFactory.overrideStreamReadConstraintsDefaults (private) successfully
 * relaxes the Jackson 2.15 global default StreamReadConstraints via reflection, and that the
 * relaxed value propagates to newly built JsonFactory instances (which is what json4s-jackson /
 * Spark EventLoggingListener rely on). The global DEFAULT is restored after each test to avoid
 * polluting other tests in the suite.
 */
class TestSparkEngineConnFactoryJacksonConstraints {

  private var engineFactory: SparkEngineConnFactory = _
  private var originalDefault: StreamReadConstraints = _

  @BeforeEach
  def before(): Unit = {
    // Capture the true Jackson default once (before any test mutates it) for restore.
    if (originalDefault == null) {
      originalDefault = StreamReadConstraints.defaults()
    }
    engineFactory = new SparkEngineConnFactory
  }

  @AfterEach
  def after(): Unit = {
    // Restore the global DEFAULT so other tests see Jackson's original default.
    if (originalDefault != null) {
      setDefaultConstraints(originalDefault)
    }
  }

  @Test
  def testOriginalDefaultMatchesJacksonConstant(): Unit = {
    // Sanity: the captured original equals Jackson's documented default constant.
    Assertions.assertEquals(
      StreamReadConstraints.DEFAULT_MAX_STRING_LEN,
      originalDefault.getMaxStringLength,
      "captured original maxStringLength should equal Jackson DEFAULT_MAX_STRING_LEN"
    )
    Assertions.assertEquals(
      StreamReadConstraints.DEFAULT_MAX_DEPTH,
      originalDefault.getMaxNestingDepth,
      "captured original maxNestingDepth should equal Jackson DEFAULT_MAX_DEPTH"
    )
  }

  @Test
  def testOverrideRelaxesGlobalDefault(): Unit = {
    val beforeString = StreamReadConstraints.defaults().getMaxStringLength
    val beforeNesting = StreamReadConstraints.defaults().getMaxNestingDepth

    invokeOverride(maxStringLength = 10000000, maxNestingDepth = 2000)

    val afterString = StreamReadConstraints.defaults().getMaxStringLength
    val afterNesting = StreamReadConstraints.defaults().getMaxNestingDepth

    Assertions.assertEquals(
      10000000,
      afterString,
      "maxStringLength should be relaxed to 10,000,000"
    )
    Assertions.assertEquals(2000, afterNesting, "maxNestingDepth should be relaxed to 2000")
    Assertions.assertNotEquals(beforeString, afterString, "maxStringLength must actually change")
    Assertions.assertNotEquals(beforeNesting, afterNesting, "maxNestingDepth must actually change")
  }

  @Test
  def testOverridePropagatesToNewJsonFactory(): Unit = {
    // This is the real-world guarantee: json4s-jackson builds its own JsonFactory, which reads
    // StreamReadConstraints.defaults() (= DEFAULT field) at construction. Proving a fresh
    // JsonFactory sees the relaxed value means Spark EventLoggingListener will too.
    invokeOverride(maxStringLength = 10000000, maxNestingDepth = 2000)

    val factory = new JsonFactory()
    Assertions.assertEquals(
      10000000,
      factory.streamReadConstraints().getMaxStringLength,
      "new JsonFactory should pick up the relaxed maxStringLength"
    )
    Assertions.assertEquals(
      2000,
      factory.streamReadConstraints().getMaxNestingDepth,
      "new JsonFactory should pick up the relaxed maxNestingDepth"
    )
  }

  @Test
  def testRestoreLeavesDefaultUnchangedAcrossTests(): Unit = {
    // Mutate, then restore manually, then assert DEFAULT is back to the captured original.
    invokeOverride(maxStringLength = 10000000, maxNestingDepth = 2000)
    setDefaultConstraints(originalDefault)

    Assertions.assertEquals(
      originalDefault.getMaxStringLength,
      StreamReadConstraints.defaults().getMaxStringLength,
      "after restore, DEFAULT should be back to original"
    )
    Assertions.assertEquals(
      StreamReadConstraints.DEFAULT_MAX_STRING_LEN,
      StreamReadConstraints.defaults().getMaxStringLength,
      "after restore, DEFAULT should match Jackson's documented constant"
    )
  }

  /** Invoke the private overrideStreamReadConstraintsDefaults(Int, Int) via reflection. */
  private def invokeOverride(maxStringLength: Int, maxNestingDepth: Int): Unit = {
    val method = classOf[SparkEngineConnFactory]
      .getDeclaredMethod("overrideStreamReadConstraintsDefaults", classOf[Int], classOf[Int])
    method.setAccessible(true)
    method.invoke(
      engineFactory,
      java.lang.Integer.valueOf(maxStringLength),
      java.lang.Integer.valueOf(maxNestingDepth)
    )
  }

  /** Mirror of the production reflection: replace the static final DEFAULT field. */
  private def setDefaultConstraints(constraints: StreamReadConstraints): Unit = {
    val field = classOf[StreamReadConstraints].getDeclaredField("DEFAULT")
    field.setAccessible(true)
    val modifiersField = classOf[Field].getDeclaredField("modifiers")
    modifiersField.setAccessible(true)
    modifiersField.setInt(field, field.getModifiers & ~Modifier.FINAL)
    field.set(null, constraints)
  }

}
