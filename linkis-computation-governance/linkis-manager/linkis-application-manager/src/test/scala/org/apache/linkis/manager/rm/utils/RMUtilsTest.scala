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

package org.apache.linkis.manager.rm.utils

import org.apache.linkis.manager.rm.restful.vo.UserResourceVo

import java.util

import org.junit.jupiter.api.Test

class RMUtilsTest {

  @Test def getResourceInfoMsg(): Unit = {
    val resourceType: String = "Memory"
    val unitType: String = "bytes"
    val requestResource: Any = "644245094400" // 600G
    val availableResource: Any = "-2147483648" // -2G
    val maxResource: Any = "20454781747200" // 19050G
    val result = RMUtils.getResourceInfoMsg(
      resourceType,
      unitType,
      requestResource,
      availableResource,
      maxResource
    )
    assert(
      " use Memory, requestResource : 600GB > availableResource : -2GB,  maxResource : 19050GB."
        .equals(result)
    )
  }

  // ==================== REQ-01 修复资源管理资源展示为负问题 单测 ====================

  /** 构造带数值字段的 resource Map（模拟 memory/cores 等数值型字段）。 */
  private def resourceMap(
      memory: Object,
      cores: Object,
      instances: Object
  ): util.Map[String, Any] = {
    val m = new util.HashMap[String, Any]()
    m.put("memory", memory)
    m.put("cores", cores)
    m.put("instances", instances)
    m
  }

  /** normalizeNegativeResource: 负数归零，非负数保持不变。 */
  @Test def normalizeNegativeResource_zeroesOnlyNegatives(): Unit = {
    val m =
      resourceMap(new java.lang.Integer(-16), new java.lang.Double(4.0), new java.lang.Integer(2))
    RMUtils.normalizeNegativeResource(m)
    assert(m.get("memory") == new java.lang.Integer(0), "负 Integer 应归零")
    assert(m.get("cores") == new java.lang.Double(4.0), "非负 Double 应保持不变")
    assert(m.get("instances") == new java.lang.Integer(2), "非负 Integer 应保持不变")
  }

  /** normalizeNegativeResource: null 输入安全返回 null。 */
  @Test def normalizeNegativeResource_nullIsSafe(): Unit = {
    assert(RMUtils.normalizeNegativeResource(null) == null, "null 入参应原样返回")
  }

  /** normalizeNegativeResource: 支持多种数值类型归零。 */
  @Test def normalizeNegativeResource_multiNumericTypes(): Unit = {
    val m =
      resourceMap(new java.lang.Long(-100L), new java.lang.Float(-1.5f), new java.lang.Integer(3))
    RMUtils.normalizeNegativeResource(m)
    assert(m.get("memory") == new java.lang.Long(0L), "负 Long 应归零")
    assert(m.get("cores") == new java.lang.Float(0f), "负 Float 应归零")
    assert(m.get("instances") == new java.lang.Integer(3), "正数应保持不变")
  }

  /** normalizeUserResourceVoNegative: used/left 负值归零（开关默认开启）。 */
  @Test def normalizeUserResourceVoNegative_zeroesUsedAndLeft(): Unit = {
    val vo = new UserResourceVo
    vo.setUsedResource(
      resourceMap(new java.lang.Integer(-8), new java.lang.Double(2.0), new java.lang.Integer(1))
    )
    vo.setLeftResource(
      resourceMap(new java.lang.Integer(-4), new java.lang.Double(1.0), new java.lang.Integer(1))
    )
    RMUtils.normalizeUserResourceVoNegative(vo)
    assert(
      vo.getUsedResource.asInstanceOf[util.Map[String, Any]].get("memory") == new java.lang.Integer(
        0
      )
    )
    assert(
      vo.getLeftResource.asInstanceOf[util.Map[String, Any]].get("memory") == new java.lang.Integer(
        0
      )
    )
  }

  /** normalizeUserResourceVoNegative: null 输入安全。 */
  @Test def normalizeUserResourceVoNegative_nullIsSafe(): Unit = {
    assert(RMUtils.normalizeUserResourceVoNegative(null) == null, "null 入参应原样返回")
  }

  /** recomputeUserResourceLeftNegative: 剩余 = 最大 - 已用（开关默认开启）。 */
  @Test def recomputeUserResourceLeftNegative_leftEqualsMaxMinusUsed(): Unit = {
    val vo = new UserResourceVo
    // max: memory=100, cores=10, instances=2
    vo.setMaxResource(
      resourceMap(new java.lang.Integer(100), new java.lang.Integer(10), new java.lang.Integer(2))
    )
    // used: memory=60, cores=4, instances=1（无负数）
    vo.setUsedResource(
      resourceMap(new java.lang.Integer(60), new java.lang.Integer(4), new java.lang.Integer(1))
    )
    // left 初始为 DB 快照（人为置成错误值，验证会被重算覆盖）
    vo.setLeftResource(
      resourceMap(
        new java.lang.Integer(-999),
        new java.lang.Integer(999),
        new java.lang.Integer(999)
      )
    )
    RMUtils.recomputeUserResourceLeftNegative(vo)
    val left = vo.getLeftResource.asInstanceOf[util.Map[String, Any]]
    assert(left.get("memory") == new java.lang.Integer(40), "left.memory 应 = 100-60=40")
    assert(left.get("cores") == new java.lang.Integer(6), "left.cores 应 = 10-4=6")
    assert(left.get("instances") == new java.lang.Integer(1), "left.instances 应 = 2-1=1")
  }

  /** recomputeUserResourceLeftNegative: 已用为负先归零，再计算 left = max - 0。 */
  @Test def recomputeUserResourceLeftNegative_zeroesUsedBeforeSub(): Unit = {
    val vo = new UserResourceVo
    vo.setMaxResource(
      resourceMap(new java.lang.Integer(100), new java.lang.Integer(10), new java.lang.Integer(2))
    )
    // used.memory = -20 → 归零后 left.memory = 100-0 = 100
    vo.setUsedResource(
      resourceMap(new java.lang.Integer(-20), new java.lang.Integer(5), new java.lang.Integer(1))
    )
    RMUtils.recomputeUserResourceLeftNegative(vo)
    val left = vo.getLeftResource.asInstanceOf[util.Map[String, Any]]
    assert(left.get("memory") == new java.lang.Integer(100), "已用为负归零后 left.memory 应 = max")
  }

  /** recomputeUserResourceLeftNegative: 已用 > 最大 时结果为负归零。 */
  @Test def recomputeUserResourceLeftNegative_negativeResultZeroed(): Unit = {
    val vo = new UserResourceVo
    vo.setMaxResource(
      resourceMap(new java.lang.Integer(10), new java.lang.Integer(10), new java.lang.Integer(2))
    )
    vo.setUsedResource(
      resourceMap(new java.lang.Integer(80), new java.lang.Integer(3), new java.lang.Integer(1))
    )
    RMUtils.recomputeUserResourceLeftNegative(vo)
    val left = vo.getLeftResource.asInstanceOf[util.Map[String, Any]]
    assert(left.get("memory") == new java.lang.Integer(0), "max-used 为负应归零")
    assert(left.get("cores") == new java.lang.Integer(7), "正常字段仍按 max-used 计算")
  }

  /** recomputeUserResourceLeftNegative: max 或 used 缺失时返回原 vo 不重算（不抛异常）。 */
  @Test def recomputeUserResourceLeftNegative_missingFieldIsSafe(): Unit = {
    val vo = new UserResourceVo
    vo.setUsedResource(
      resourceMap(new java.lang.Integer(60), new java.lang.Integer(4), new java.lang.Integer(1))
    )
    // 未设置 max
    RMUtils.recomputeUserResourceLeftNegative(vo)
    assert(vo.getLeftResource == null, "max 缺失时不应重算 left")
    assert(RMUtils.recomputeUserResourceLeftNegative(null) == null, "null 入参应原样返回")
  }

  /** recomputeUserResourceLeftNegative: Double 类型计算 left = max - used。 */
  @Test def recomputeUserResourceLeftNegative_doubleTypes(): Unit = {
    val vo = new UserResourceVo
    vo.setMaxResource(
      resourceMap(new java.lang.Double(100.0), new java.lang.Double(10.0), new java.lang.Integer(2))
    )
    vo.setUsedResource(
      resourceMap(new java.lang.Double(35.5), new java.lang.Double(10.0), new java.lang.Integer(1))
    )
    RMUtils.recomputeUserResourceLeftNegative(vo)
    val left = vo.getLeftResource.asInstanceOf[util.Map[String, Any]]
    assert(left.get("memory") == new java.lang.Double(64.5), "Double left.memory 应 = 100-35.5=64.5")
  }

  /**
   * recomputeUserResourceLeftNegative: DriverAndYarn 复合结构(嵌套 Map)不再被覆盖为空 {}。 回归用例：修复前顶层仅按 Number
   * 匹配，嵌套的 loadInstanceResource/yarnResource 被整体忽略， 导致 left 被覆盖成空 map；修复后需递归计算内层 left=max-used。
   */
  @Test def recomputeUserResourceLeftNegative_driverAndYarnNested(): Unit = {
    def nested(load: util.Map[String, Any], yarn: util.Map[String, Any]): util.Map[String, Any] = {
      val m = new util.HashMap[String, Any]()
      m.put("loadInstanceResource", load)
      m.put("yarnResource", yarn)
      m
    }
    val vo = new UserResourceVo
    val maxLoad =
      resourceMap(new java.lang.Integer(100), new java.lang.Integer(10), new java.lang.Integer(2))
    val maxYarn = new util.HashMap[String, Any]()
    maxYarn.put("queueMemory", new java.lang.Integer(300))
    maxYarn.put("queueCores", new java.lang.Integer(30))
    maxYarn.put("queueName", "dws")
    vo.setMaxResource(nested(maxLoad, maxYarn))
    val usedLoad =
      resourceMap(new java.lang.Integer(40), new java.lang.Integer(4), new java.lang.Integer(1))
    val usedYarn = new util.HashMap[String, Any]()
    usedYarn.put("queueMemory", new java.lang.Integer(50))
    usedYarn.put("queueCores", new java.lang.Integer(5))
    usedYarn.put("queueName", "dws")
    vo.setUsedResource(nested(usedLoad, usedYarn))
    // left 初始为 DB 快照(非空)，验证会被重算填充而非覆盖为空
    vo.setLeftResource(new util.HashMap[String, Any]())
    RMUtils.recomputeUserResourceLeftNegative(vo)
    val left = vo.getLeftResource.asInstanceOf[util.Map[String, Any]]
    assert(left.size() == 2, "left 顶层应有 loadInstanceResource 与 yarnResource 两个嵌套结构，而不是空 {}")
    val leftLoad = left.get("loadInstanceResource").asInstanceOf[util.Map[String, Any]]
    val leftYarn = left.get("yarnResource").asInstanceOf[util.Map[String, Any]]
    assert(leftLoad.get("memory") == new java.lang.Integer(60), "load.left.memory 应 = 100-40=60")
    assert(leftLoad.get("cores") == new java.lang.Integer(6), "load.left.cores 应 = 10-4=6")
    assert(
      leftYarn.get("queueMemory") == new java.lang.Integer(250),
      "yarn.left.queueMemory 应 = 300-50=250"
    )
    assert(
      leftYarn.get("queueCores") == new java.lang.Integer(25),
      "yarn.left.queueCores 应 = 30-5=25"
    )
    assert(leftYarn.get("queueName") == "dws", "非数值字段 queueName 应保留 max 值")
  }

  /** normalizeNegativeResource: 嵌套复合结构负值递归归零。 */
  @Test def normalizeNegativeResource_nestedMapRecursion(): Unit = {
    val inner =
      resourceMap(new java.lang.Integer(-20), new java.lang.Integer(4), new java.lang.Integer(1))
    val outer = new util.HashMap[String, Any]()
    outer.put("loadInstanceResource", inner)
    outer.put("yarnResource", new util.HashMap[String, Any]())
    RMUtils.normalizeNegativeResource(outer)
    val innerAfter = outer.get("loadInstanceResource").asInstanceOf[util.Map[String, Any]]
    assert(innerAfter.get("memory") == new java.lang.Integer(0), "嵌套 load 的负 memory 应递归归零")
    assert(innerAfter.get("cores") == new java.lang.Integer(4), "嵌套 load 的非负 cores 应保持不变")
  }

}
