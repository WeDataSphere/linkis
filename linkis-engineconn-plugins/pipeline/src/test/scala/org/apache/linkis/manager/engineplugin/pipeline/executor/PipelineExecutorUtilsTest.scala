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

package org.apache.linkis.manager.engineplugin.pipeline.executor

import org.apache.linkis.common.io.FsPath

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class PipelineExecutorUtilsTest {

  @Test
  def testLocalOwnerRoot(): Unit = {
    Assertions.assertTrue(
      PipelineExecutorUtils
        .isUnderOwnerRoot(new FsPath("file:///tmp/linkis/alice/export/result.csv"), "alice")
    )
    // 根路径本身也应判定为归属
    Assertions.assertTrue(
      PipelineExecutorUtils.isUnderOwnerRoot(new FsPath("file:///tmp/linkis/alice"), "alice")
    )
    // 前缀相同的其他用户（/tmp/linkis/alice2）不能被误判为归属 alice
    Assertions.assertFalse(
      PipelineExecutorUtils
        .isUnderOwnerRoot(new FsPath("file:///tmp/linkis/alice2/export/result.csv"), "alice")
    )
  }

  @Test
  def testHdfsOwnerRoot(): Unit = {
    Assertions.assertTrue(
      PipelineExecutorUtils
        .isUnderOwnerRoot(new FsPath("hdfs:///tmp/alice/linkis/export/result.csv"), "alice")
    )
    Assertions.assertFalse(
      PipelineExecutorUtils
        .isUnderOwnerRoot(new FsPath("hdfs:///tmp/bob/linkis/export/result.csv"), "alice")
    )
  }

  @Test
  def testOtherFsTypeNotMatched(): Unit = {
    Assertions.assertFalse(
      PipelineExecutorUtils.isUnderOwnerRoot(new FsPath("s3://bucket/alice/result.csv"), "alice")
    )
  }

}
