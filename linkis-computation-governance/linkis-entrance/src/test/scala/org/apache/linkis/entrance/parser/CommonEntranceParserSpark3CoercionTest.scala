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

package org.apache.linkis.entrance.parser

import org.apache.linkis.entrance.conf.EntranceConfiguration
import org.apache.linkis.entrance.persistence.PersistenceManager
import org.apache.linkis.entrance.utils.EntranceUtils
import org.apache.linkis.manager.label.conf.LabelCommonConfig
import org.apache.linkis.manager.label.constant.LabelKeyConstant
import org.apache.linkis.manager.label.entity.Label
import org.apache.linkis.manager.label.entity.engine.{EngineType, EngineTypeLabel, UserCreatorLabel}
import org.apache.linkis.manager.label.utils.EngineTypeLabelCreator

import java.lang.reflect.{Field, Modifier}
import java.util

import org.junit.jupiter.api.{AfterEach, Assertions, BeforeEach, DisplayName, Test}
import org.mockito.{ArgumentMatchers, Mockito}

/**
 * Unit tests for Spark3 version coercion creator dimension in CommonEntranceParser.
 *
 * Tests the creator-level judgment branch added to the sparkVersionCoercion method. Uses reflection
 * to:
 *   - Modify EntranceConfiguration val fields (switch, creators, users, department)
 *   - Replace EntranceUtils singleton MODULE$ with a mock to prevent RPC calls
 *   - Invoke the private sparkVersionCoercion method
 */
class CommonEntranceParserSpark3CoercionTest {

  private var parser: CommonEntranceParser = null
  private var originalSwitch: Boolean = false
  private var originalCreators: String = ""
  private var originalUsers: String = ""
  private var originalDepartment: String = ""
  private var originalEntranceUtils: AnyRef = null
  private var entranceUtilsModuleField: Field = null
  private var mockEntranceUtils: AnyRef = null

  private val spark2Version: String = LabelCommonConfig.SPARK_ENGINE_VERSION.getValue
  private val spark3Version: String = LabelCommonConfig.SPARK3_ENGINE_VERSION.getValue

  @BeforeEach
  def setUp(): Unit = {
    // Save original EntranceConfiguration values
    originalSwitch = EntranceConfiguration.SPARK3_VERSION_COERCION_SWITCH
    originalCreators = EntranceConfiguration.SPARK3_VERSION_COERCION_CREATORS
    originalUsers = EntranceConfiguration.SPARK3_VERSION_COERCION_USERS
    originalDepartment = EntranceConfiguration.SPARK3_VERSION_COERCION_DEPARTMENT

    // Set default test config: switch on, creators=IDE, users/department empty
    setEntranceConfigField("SPARK3_VERSION_COERCION_SWITCH", java.lang.Boolean.TRUE)
    setEntranceConfigField("SPARK3_VERSION_COERCION_CREATORS", "IDE")
    setEntranceConfigField("SPARK3_VERSION_COERCION_USERS", "")
    setEntranceConfigField("SPARK3_VERSION_COERCION_DEPARTMENT", "")

    // Mock EntranceUtils singleton to prevent RPC calls in department check
    mockEntranceUtilsSingleton()

    // Create parser with mocked PersistenceManager
    val mockPersistenceManager = Mockito.mock(classOf[PersistenceManager])
    parser = new CommonEntranceParser(mockPersistenceManager)
  }

  @AfterEach
  def tearDown(): Unit = {
    // Restore EntranceConfiguration values
    setEntranceConfigField(
      "SPARK3_VERSION_COERCION_SWITCH",
      java.lang.Boolean.valueOf(originalSwitch)
    )
    setEntranceConfigField("SPARK3_VERSION_COERCION_CREATORS", originalCreators)
    setEntranceConfigField("SPARK3_VERSION_COERCION_USERS", originalUsers)
    setEntranceConfigField("SPARK3_VERSION_COERCION_DEPARTMENT", originalDepartment)

    // Restore EntranceUtils singleton
    restoreEntranceUtilsSingleton()
  }

  // ==================== Helper Methods ====================

  /**
   * Uses reflection to modify a final val field on the EntranceConfiguration object. Removes the
   * final modifier, sets the new value, for both Boolean and String fields.
   */
  private def setEntranceConfigField(fieldName: String, value: AnyRef): Unit = {
    val configInstance = EntranceConfiguration
    val field = configInstance.getClass.getDeclaredField(fieldName)
    field.setAccessible(true)
    val modifiersField = classOf[Field].getDeclaredField("modifiers")
    modifiersField.setAccessible(true)
    modifiersField.setInt(field, field.getModifiers & ~Modifier.FINAL)
    field.set(configInstance, value)
  }

  /**
   * Replaces the EntranceUtils$ MODULE$ singleton with a Mockito mock. The mock returns empty
   * string for getUserDepartmentId to prevent RPC calls.
   */
  private def mockEntranceUtilsSingleton(): Unit = {
    val entranceUtilsClass = EntranceUtils.getClass
    entranceUtilsModuleField = entranceUtilsClass.getDeclaredField("MODULE$")
    entranceUtilsModuleField.setAccessible(true)
    originalEntranceUtils = entranceUtilsModuleField.get(null)

    mockEntranceUtils = Mockito.mock(entranceUtilsClass)
    val typedMock = mockEntranceUtils.asInstanceOf[EntranceUtils.type]
    Mockito
      .when(typedMock.getUserDepartmentId(ArgumentMatchers.anyString()))
      .thenReturn("")

    val modifiersField = classOf[Field].getDeclaredField("modifiers")
    modifiersField.setAccessible(true)
    modifiersField.setInt(
      entranceUtilsModuleField,
      entranceUtilsModuleField.getModifiers & ~Modifier.FINAL
    )
    entranceUtilsModuleField.set(null, mockEntranceUtils)
  }

  /**
   * Restores the original EntranceUtils$ MODULE$ singleton.
   */
  private def restoreEntranceUtilsSingleton(): Unit = {
    if (entranceUtilsModuleField != null && originalEntranceUtils != null) {
      entranceUtilsModuleField.set(null, originalEntranceUtils)
    }
  }

  /**
   * Creates a labels map with a Spark2 EngineTypeLabel and a UserCreatorLabel with the specified
   * creator value.
   */
  private def createSpark2Labels(creator: String): util.HashMap[String, Label[_]] = {
    val labels = new util.HashMap[String, Label[_]]()
    labels.put(
      LabelKeyConstant.ENGINE_TYPE_KEY,
      EngineTypeLabelCreator.createEngineTypeLabel(EngineType.SPARK.toString, spark2Version)
    )
    val userCreatorLabel = new UserCreatorLabel()
    userCreatorLabel.setUser("testUser")
    userCreatorLabel.setCreator(creator)
    labels.put(LabelKeyConstant.USER_CREATOR_TYPE_KEY, userCreatorLabel)
    labels
  }

  /**
   * Creates a labels map with a Spark2 EngineTypeLabel but no UserCreatorLabel.
   */
  private def createSpark2LabelsWithoutUserCreator(): util.HashMap[String, Label[_]] = {
    val labels = new util.HashMap[String, Label[_]]()
    labels.put(
      LabelKeyConstant.ENGINE_TYPE_KEY,
      EngineTypeLabelCreator.createEngineTypeLabel(EngineType.SPARK.toString, spark2Version)
    )
    labels
  }

  /**
   * Creates a labels map with a Hive EngineTypeLabel and a UserCreatorLabel.
   */
  private def createHiveLabels(): util.HashMap[String, Label[_]] = {
    val labels = new util.HashMap[String, Label[_]]()
    labels.put(
      LabelKeyConstant.ENGINE_TYPE_KEY,
      EngineTypeLabelCreator.createEngineTypeLabel(EngineType.HIVE.toString, "2.3.3")
    )
    val userCreatorLabel = new UserCreatorLabel()
    userCreatorLabel.setUser("testUser")
    userCreatorLabel.setCreator("IDE")
    labels.put(LabelKeyConstant.USER_CREATOR_TYPE_KEY, userCreatorLabel)
    labels
  }

  /**
   * Invokes the private sparkVersionCoercion method via reflection.
   */
  private def invokeSparkVersionCoercion(
      labels: util.HashMap[String, Label[_]],
      executeUser: String,
      submitUser: String
  ): util.HashMap[String, Label[_]] = {
    val method = classOf[CommonEntranceParser].getDeclaredMethod(
      "sparkVersionCoercion",
      classOf[util.HashMap[_, _]],
      classOf[String],
      classOf[String]
    )
    method.setAccessible(true)
    method
      .invoke(parser, labels, executeUser, submitUser)
      .asInstanceOf[util.HashMap[String, Label[_]]]
  }

  private def assertSpark3(result: util.HashMap[String, Label[_]]): Unit = {
    val engineTypeLabel = result
      .get(LabelKeyConstant.ENGINE_TYPE_KEY)
      .asInstanceOf[EngineTypeLabel]
    Assertions.assertEquals(spark3Version, engineTypeLabel.getVersion)
  }

  private def assertSpark2(result: util.HashMap[String, Label[_]]): Unit = {
    val engineTypeLabel = result
      .get(LabelKeyConstant.ENGINE_TYPE_KEY)
      .asInstanceOf[EngineTypeLabel]
    Assertions.assertEquals(spark2Version, engineTypeLabel.getVersion)
  }

  // ==================== Test Cases ====================

  @Test
  @DisplayName("creator 命中名单 → EngineTypeLabel version 改为 3.4.4")
  def testCreatorHit(): Unit = {
    setEntranceConfigField("SPARK3_VERSION_COERCION_CREATORS", "IDE")
    val labels = createSpark2Labels("IDE")
    val result = invokeSparkVersionCoercion(labels, "testUser", "testUser")
    assertSpark3(result)
  }

  @Test
  @DisplayName("creator 未命中名单 → 保持原 version")
  def testCreatorNotHit(): Unit = {
    setEntranceConfigField("SPARK3_VERSION_COERCION_CREATORS", "IDE")
    val labels = createSpark2Labels("Schedulis")
    val result = invokeSparkVersionCoercion(labels, "testUser", "testUser")
    assertSpark2(result)
  }

  @Test
  @DisplayName("creator 名单为空 → 保持原 version（与增强前一致）")
  def testCreatorListEmpty(): Unit = {
    setEntranceConfigField("SPARK3_VERSION_COERCION_CREATORS", "")
    val labels = createSpark2Labels("IDE")
    val result = invokeSparkVersionCoercion(labels, "testUser", "testUser")
    assertSpark2(result)
  }

  @Test
  @DisplayName("UserCreatorLabel 不存在（labels 无该 key）→ 不抛异常，保持原 version")
  def testUserCreatorLabelNotExists(): Unit = {
    setEntranceConfigField("SPARK3_VERSION_COERCION_CREATORS", "IDE")
    val labels = createSpark2LabelsWithoutUserCreator()
    val result = invokeSparkVersionCoercion(labels, "testUser", "testUser")
    assertSpark2(result)
  }

  @Test
  @DisplayName("creator 为 null/空白 → 不命中，保持原 version")
  def testCreatorBlank(): Unit = {
    setEntranceConfigField("SPARK3_VERSION_COERCION_CREATORS", "IDE")

    // Test with null creator
    val labelsNull = createSpark2Labels(null)
    val resultNull = invokeSparkVersionCoercion(labelsNull, "testUser", "testUser")
    assertSpark2(resultNull)

    // Test with blank creator
    val labelsBlank = createSpark2Labels("")
    val resultBlank = invokeSparkVersionCoercion(labelsBlank, "testUser", "testUser")
    assertSpark2(resultBlank)
  }

  @Test
  @DisplayName("用户级命中时不检查 creator（优先级）")
  def testUserPriorityOverCreator(): Unit = {
    setEntranceConfigField("SPARK3_VERSION_COERCION_USERS", "testUser")
    setEntranceConfigField("SPARK3_VERSION_COERCION_CREATORS", "Schedulis")
    // creator is IDE (not in creators list), but user check should match first
    val labels = createSpark2Labels("IDE")
    val result = invokeSparkVersionCoercion(labels, "testUser", "testUser")
    assertSpark3(result)
  }

  @Test
  @DisplayName("部门级命中时不检查 creator（优先级）")
  def testDepartmentPriorityOverCreator(): Unit = {
    setEntranceConfigField("SPARK3_VERSION_COERCION_USERS", "")
    setEntranceConfigField("SPARK3_VERSION_COERCION_DEPARTMENT", "dept01")
    setEntranceConfigField("SPARK3_VERSION_COERCION_CREATORS", "Schedulis")
    // Configure mock to return matching department ID
    val typedMock = mockEntranceUtils.asInstanceOf[EntranceUtils.type]
    Mockito
      .when(typedMock.getUserDepartmentId(ArgumentMatchers.anyString()))
      .thenReturn("dept01")
    // creator is IDE (not in creators list), but department check should match first
    val labels = createSpark2Labels("IDE")
    val result = invokeSparkVersionCoercion(labels, "testUser", "testUser")
    assertSpark3(result)
  }

  @Test
  @DisplayName("总开关关闭 → creator 分支不执行")
  def testSwitchOff(): Unit = {
    setEntranceConfigField("SPARK3_VERSION_COERCION_SWITCH", java.lang.Boolean.FALSE)
    setEntranceConfigField("SPARK3_VERSION_COERCION_CREATORS", "IDE")
    val labels = createSpark2Labels("IDE")
    val result = invokeSparkVersionCoercion(labels, "testUser", "testUser")
    assertSpark2(result)
  }

  @Test
  @DisplayName("非 Spark 引擎 → 不切换")
  def testNonSparkEngine(): Unit = {
    setEntranceConfigField("SPARK3_VERSION_COERCION_CREATORS", "IDE")
    val labels = createHiveLabels()
    val result = invokeSparkVersionCoercion(labels, "testUser", "testUser")
    val engineTypeLabel = result
      .get(LabelKeyConstant.ENGINE_TYPE_KEY)
      .asInstanceOf[EngineTypeLabel]
    Assertions.assertEquals("2.3.3", engineTypeLabel.getVersion)
  }

  @Test
  @DisplayName("异常降级（模拟 asInstanceOf 类型不匹配）→ 不抛异常，保持原 labels")
  def testExceptionDegradation(): Unit = {
    setEntranceConfigField("SPARK3_VERSION_COERCION_CREATORS", "IDE")
    val labels = new util.HashMap[String, Label[_]]()
    val spark2Label =
      EngineTypeLabelCreator.createEngineTypeLabel(EngineType.SPARK.toString, spark2Version)
    labels.put(LabelKeyConstant.ENGINE_TYPE_KEY, spark2Label)
    // Put a non-UserCreatorLabel under USER_CREATOR_TYPE_KEY to cause ClassCastException
    labels.put(LabelKeyConstant.USER_CREATOR_TYPE_KEY, spark2Label)
    val result = invokeSparkVersionCoercion(labels, "testUser", "testUser")
    assertSpark2(result)
  }

}
