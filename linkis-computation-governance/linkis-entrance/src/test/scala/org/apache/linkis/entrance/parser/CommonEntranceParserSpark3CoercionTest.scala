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

import org.apache.linkis.entrance.persistence.PersistenceManager
import org.apache.linkis.manager.label.conf.LabelCommonConfig
import org.apache.linkis.manager.label.constant.LabelKeyConstant
import org.apache.linkis.manager.label.entity.Label
import org.apache.linkis.manager.label.entity.engine.{EngineType, EngineTypeLabel, UserCreatorLabel}
import org.apache.linkis.manager.label.utils.EngineTypeLabelCreator

import java.util

import org.junit.jupiter.api.{Assertions, BeforeEach, DisplayName, Test}
import org.mockito.Mockito

/**
 * Unit tests for Spark3 version coercion in CommonEntranceParser.
 *
 * After the config-service migration, coercion reads its 4 config keys via RPC
 * (fetchSpark3CoercionConfig) and the department id via fetchUserDepartmentId. Both are
 * short-circuited in tests by overriding them in an anonymous subclass (returning a preset
 * configMap / deptId), so tests focus on the judgment logic (user > department > creator) without a
 * live linkis-ps-configuration and without reflecting on the EntranceUtils singleton (which the JVM
 * forbids for static-final fields on JDK 12+).
 */
class CommonEntranceParserSpark3CoercionTest {

  private var parser: CommonEntranceParser = null

  /** Preset config map returned by the overridden fetchSpark3CoercionConfig. */
  private val configMap: util.Map[String, String] = new util.HashMap[String, String]()

  /** Preset department id returned by the overridden fetchUserDepartmentId. */
  private var mockDeptId: String = ""

  private val spark2Version: String = LabelCommonConfig.SPARK_ENGINE_VERSION.getValue
  private val spark3Version: String = LabelCommonConfig.SPARK3_ENGINE_VERSION.getValue

  @BeforeEach
  def setUp(): Unit = {
    configMap.clear()
    // Default config: switch on, creators=IDE, users/department empty
    configMap.put("spark.version.coercion.switch", "true")
    configMap.put("spark.version.coercion.creators", "IDE")
    configMap.put("spark.version.coercion.users", "")
    configMap.put("spark.version.coercion.department.id", "")
    mockDeptId = ""

    val mockPersistenceManager = Mockito.mock(classOf[PersistenceManager])
    parser = new CommonEntranceParser(mockPersistenceManager) {
      override def fetchSpark3CoercionConfig(
          labels: util.HashMap[String, Label[_]],
          executeUser: String
      ): util.Map[String, String] = configMap

      override def fetchUserDepartmentId(user: String): String = mockDeptId
    }
  }

  // ==================== Helper Methods ====================

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
    configMap.put("spark.version.coercion.creators", "IDE")
    val labels = createSpark2Labels("IDE")
    val result = invokeSparkVersionCoercion(labels, "testUser", "testUser")
    assertSpark3(result)
  }

  @Test
  @DisplayName("creator 未命中名单 → 保持原 version")
  def testCreatorNotHit(): Unit = {
    configMap.put("spark.version.coercion.creators", "IDE")
    val labels = createSpark2Labels("Schedulis")
    val result = invokeSparkVersionCoercion(labels, "testUser", "testUser")
    assertSpark2(result)
  }

  @Test
  @DisplayName("creator 名单为空 → 保持原 version（与增强前一致）")
  def testCreatorListEmpty(): Unit = {
    configMap.put("spark.version.coercion.creators", "")
    val labels = createSpark2Labels("IDE")
    val result = invokeSparkVersionCoercion(labels, "testUser", "testUser")
    assertSpark2(result)
  }

  @Test
  @DisplayName("UserCreatorLabel 不存在（labels 无该 key）→ 不抛异常，保持原 version")
  def testUserCreatorLabelNotExists(): Unit = {
    configMap.put("spark.version.coercion.creators", "IDE")
    val labels = createSpark2LabelsWithoutUserCreator()
    val result = invokeSparkVersionCoercion(labels, "testUser", "testUser")
    assertSpark2(result)
  }

  @Test
  @DisplayName("creator 为 null/空白 → 不命中，保持原 version")
  def testCreatorBlank(): Unit = {
    configMap.put("spark.version.coercion.creators", "IDE")

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
    configMap.put("spark.version.coercion.users", "testUser")
    configMap.put("spark.version.coercion.creators", "Schedulis")
    // creator is IDE (not in creators list), but user check should match first
    val labels = createSpark2Labels("IDE")
    val result = invokeSparkVersionCoercion(labels, "testUser", "testUser")
    assertSpark3(result)
  }

  @Test
  @DisplayName("部门级命中时不检查 creator（优先级）")
  def testDepartmentPriorityOverCreator(): Unit = {
    configMap.put("spark.version.coercion.users", "")
    configMap.put("spark.version.coercion.department.id", "dept01")
    configMap.put("spark.version.coercion.creators", "Schedulis")
    mockDeptId = "dept01"
    // creator is IDE (not in creators list), but department check should match first
    val labels = createSpark2Labels("IDE")
    val result = invokeSparkVersionCoercion(labels, "testUser", "testUser")
    assertSpark3(result)
  }

  @Test
  @DisplayName("总开关关闭 → creator 分支不执行")
  def testSwitchOff(): Unit = {
    configMap.put("spark.version.coercion.switch", "false")
    configMap.put("spark.version.coercion.creators", "IDE")
    val labels = createSpark2Labels("IDE")
    val result = invokeSparkVersionCoercion(labels, "testUser", "testUser")
    assertSpark2(result)
  }

  @Test
  @DisplayName("非 Spark 引擎 → 不切换")
  def testNonSparkEngine(): Unit = {
    configMap.put("spark.version.coercion.creators", "IDE")
    val labels = createHiveLabels()
    val result = invokeSparkVersionCoercion(labels, "testUser", "testUser")
    val engineTypeLabel = result
      .get(LabelKeyConstant.ENGINE_TYPE_KEY)
      .asInstanceOf[EngineTypeLabel]
    Assertions.assertEquals("2.3.3", engineTypeLabel.getVersion)
  }

  @Test
  @DisplayName("RPC 拉配置为 null（fallback）→ 不抛异常，保持原 version")
  def testRpcFallbackToNull(): Unit = {
    // Override fetch to return null (simulates RPC failure -> fallback to local default)
    val mockPersistenceManager = Mockito.mock(classOf[PersistenceManager])
    val nullRpcParser = new CommonEntranceParser(mockPersistenceManager) {
      override def fetchSpark3CoercionConfig(
          labels: util.HashMap[String, Label[_]],
          executeUser: String
      ): util.Map[String, String] = null

      override def fetchUserDepartmentId(user: String): String = ""
    }
    val method = classOf[CommonEntranceParser].getDeclaredMethod(
      "sparkVersionCoercion",
      classOf[util.HashMap[_, _]],
      classOf[String],
      classOf[String]
    )
    method.setAccessible(true)
    // RPC returns null -> getValue(null) walks CommonVars local default (switch=false, creators="")
    // -> keep Spark2
    val labels = createSpark2Labels("IDE")
    val result = method
      .invoke(nullRpcParser, labels, "testUser", "testUser")
      .asInstanceOf[util.HashMap[String, Label[_]]]
    assertSpark2(result)
  }

  @Test
  @DisplayName("异常降级（模拟 asInstanceOf 类型不匹配）→ 不抛异常，保持原 labels")
  def testExceptionDegradation(): Unit = {
    configMap.put("spark.version.coercion.creators", "IDE")
    val labels = new util.HashMap[String, Label[_]]()
    val spark2Label =
      EngineTypeLabelCreator.createEngineTypeLabel(EngineType.SPARK.toString, spark2Version)
    labels.put(LabelKeyConstant.ENGINE_TYPE_KEY, spark2Label)
    // Put a non-UserCreatorLabel under USER_CREATOR_TYPE_KEY to cause ClassCastException
    labels.put(LabelKeyConstant.USER_CREATOR_TYPE_KEY, spark2Label)
    val result = invokeSparkVersionCoercion(labels, "testUser", "testUser")
    assertSpark2(result)
  }

  // ==================== user+creator 组合维度用例 ====================

  @Test
  @DisplayName("user+creator 组合命中 → 切换 Spark3")
  def testUserCreatorComboHit(): Unit = {
    configMap.put("spark.version.coercion.creators", "")
    configMap.put("spark.version.coercion.user.creators", "testUser:IDE")
    val labels = createSpark2Labels("IDE")
    val result = invokeSparkVersionCoercion(labels, "testUser", "testUser")
    assertSpark3(result)
  }

  @Test
  @DisplayName("user+creator 组合 - creator 不匹配 → 保持 Spark2")
  def testUserCreatorComboCreatorNotMatch(): Unit = {
    configMap.put("spark.version.coercion.creators", "")
    configMap.put("spark.version.coercion.user.creators", "testUser:IDE")
    // creator=Schedulis，组合 testUser:Schedulis 不在名单
    val labels = createSpark2Labels("Schedulis")
    val result = invokeSparkVersionCoercion(labels, "testUser", "testUser")
    assertSpark2(result)
  }

  @Test
  @DisplayName("user+creator 组合 - user 不匹配 → 保持 Spark2")
  def testUserCreatorComboUserNotMatch(): Unit = {
    configMap.put("spark.version.coercion.creators", "")
    configMap.put("spark.version.coercion.user.creators", "otherUser:IDE")
    // executeUser=testUser，组合 testUser:IDE 不在名单（otherUser:IDE 在）
    val labels = createSpark2Labels("IDE")
    val result = invokeSparkVersionCoercion(labels, "testUser", "testUser")
    assertSpark2(result)
  }

  @Test
  @DisplayName("user+creator 组合 - 精确匹配（子串安全）")
  def testUserCreatorComboExactMatch(): Unit = {
    configMap.put("spark.version.coercion.creators", "")
    // 名单 "testUser:IDE"，creator="ID"（子串）→ testUser:ID 不在名单（split 精确匹配，不误命中）
    configMap.put("spark.version.coercion.user.creators", "testUser:IDE")
    val labels = createSpark2Labels("ID")
    val result = invokeSparkVersionCoercion(labels, "testUser", "testUser")
    assertSpark2(result)
  }

  @Test
  @DisplayName("用户级命中时不检查组合（优先级：个人 > 组合）")
  def testUserPriorityOverCombo(): Unit = {
    configMap.put("spark.version.coercion.users", "testUser")
    configMap.put("spark.version.coercion.creators", "")
    configMap.put("spark.version.coercion.user.creators", "otherUser:IDE")
    // executeUser=testUser 在用户级名单 → 用户级命中切换（组合 testUser:IDE 不在，但用户级先命中）
    val labels = createSpark2Labels("IDE")
    val result = invokeSparkVersionCoercion(labels, "testUser", "testUser")
    assertSpark3(result)
  }

  @Test
  @DisplayName("组合命中时不检查 creator（优先级：组合 > creator）")
  def testComboPriorityOverCreator(): Unit = {
    configMap.put("spark.version.coercion.creators", "Schedulis")
    configMap.put("spark.version.coercion.user.creators", "testUser:IDE")
    // 组合 testUser:IDE 命中 → 切换（creator 名单含 Schedulis 不含 IDE，但组合先命中）
    val labels = createSpark2Labels("IDE")
    val result = invokeSparkVersionCoercion(labels, "testUser", "testUser")
    assertSpark3(result)
  }

  @Test
  @DisplayName("组合名单为空 → 跳过组合检查")
  def testComboListEmpty(): Unit = {
    configMap.put("spark.version.coercion.creators", "")
    configMap.put("spark.version.coercion.user.creators", "")
    val labels = createSpark2Labels("IDE")
    val result = invokeSparkVersionCoercion(labels, "testUser", "testUser")
    assertSpark2(result)
  }

}
