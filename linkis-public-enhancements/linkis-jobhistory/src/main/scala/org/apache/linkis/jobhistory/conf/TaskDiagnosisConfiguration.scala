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

package org.apache.linkis.jobhistory.conf

import org.apache.linkis.common.conf.CommonVars

object TaskDiagnosisConfiguration {

  val TASK_CLASSIFIED_DIAGNOSIS_ENABLE: CommonVars[Boolean] =
    CommonVars("linkis.task.classified.diagnosis.enable", true)

  val LINKIS_ERROR_CODES: CommonVars[String] =
    CommonVars(
      "linkis.task.classified-diagnosis.linkis.error-codes",
      "20039,12003,40102,40103,40100,40105,20010,20011,20052"
    )

  val ENGINE_ERROR_CODE_RANGE_START: CommonVars[Int] =
    CommonVars("linkis.task.classified-diagnosis.engine.error-code-range-start", 26000)

  val ENGINE_ERROR_CODE_RANGE_END: CommonVars[Int] =
    CommonVars("linkis.task.classified-diagnosis.engine.error-code-range-end", 29999)

  val UNDERLYING_KEYWORDS: CommonVars[String] =
    CommonVars(
      "linkis.task.classified-diagnosis.underlying.keywords",
      "Container killed by YARN,Container killed by the Yarn,java.lang.OutOfMemoryError,OOM,引擎OOM,引擎退出,引擎意外退出,EC exits unexpectedly,quited unexpectedly,java.io.FileNotFoundException,org.apache.spark.SparkException,org.apache.hadoop.hive.ql.exec,Connection refused,Could not connect,Table not found,Database not found,Permission denied,无权限访问,权限不足,AccessControlException,Application killed by user,ParseException,语法错误,AnalysisException"
    )

  val LINKIS_KEYWORDS: CommonVars[String] =
    CommonVars(
      "linkis.task.classified-diagnosis.linkis.keywords",
      "EngineConn closed,engineconn is ShuttingDown,requestEngineFailed,ask engine failed,engine not exists,EngineConn not found,Failed to launch EngineConn,SendToEntrance error,主动kill引擎"
    )

}
