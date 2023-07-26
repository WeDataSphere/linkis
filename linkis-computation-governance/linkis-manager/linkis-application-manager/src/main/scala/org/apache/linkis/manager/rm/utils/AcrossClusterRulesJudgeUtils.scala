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

import org.apache.linkis.common.utils.Logging
import org.apache.linkis.manager.common.entity.resource.YarnResource

object AcrossClusterRulesJudgeUtils extends Logging {

  def acrossClusterRuleJudge(
      leftResource: YarnResource,
      maxResource: YarnResource,
      leftCPUThreshold: Int,
      leftMemoryThreshold: Long,
      UsedCPUPercentageThreshold: Double,
      UsedMemoryPercentageThreshold: Double
  ): Boolean = {
    if (leftResource != null && maxResource != null) {
      val leftQueueMemory = leftResource.queueMemory / Math.pow(1024, 3).toLong

      logger.info(
        s"leftResource.queueCores: ${leftResource.queueCores}, leftCPUThreshold: $leftCPUThreshold"
      )
      logger.info(s"leftQueueMemory: $leftQueueMemory, leftMemoryThreshold: $leftMemoryThreshold")
      if (leftResource.queueCores < leftCPUThreshold && leftQueueMemory < leftMemoryThreshold) { // 集群的剩余资源小于阈值
        val maxQueueMemory = maxResource.queueMemory / Math.pow(1024, 3).toLong
        val usedCPUPercentage =
          1 - leftResource.queueCores.toDouble / maxResource.queueCores.toDouble
        val usedMemoryPercentage =
          1 - leftQueueMemory.toDouble / maxQueueMemory.toDouble

        logger.info(
          s"usedCPUPercentage: $usedCPUPercentage, UsedCPUPercentageThreshold: $UsedCPUPercentageThreshold"
        )
        logger.info(
          s"usedMemoryPercentage: $usedMemoryPercentage, UsedMemoryPercentageThreshold: $UsedMemoryPercentageThreshold"
        )
        if (
            usedCPUPercentage < UsedCPUPercentageThreshold && usedMemoryPercentage < UsedMemoryPercentageThreshold
        ) { // 集群的资源使用率小于阈值
          return true
        }
      }
    }

    false
  }

}
