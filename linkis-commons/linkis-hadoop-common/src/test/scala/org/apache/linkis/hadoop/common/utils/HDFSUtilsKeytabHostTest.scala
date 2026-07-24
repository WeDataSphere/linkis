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

package org.apache.linkis.hadoop.common.utils

import org.junit.jupiter.api.{Assertions, Test}

/**
 * Unit tests for keytab principal host resolution (resolveKeytabHost / getKerberosUser).
 *
 * The host part is driven by a single switch `wds.linkis.keytab.host.enabled`: when true the local
 * machine hostname is appended to the principal; when false (default) no host is appended.
 *
 * Note: CommonVars.getValue is a `val` (cached at HadoopConf object init time), so runtime
 * System.setProperty override is unreliable. These tests therefore exercise the branch reachable
 * under the default config (host.enabled=false -> no host). The host.enabled=true branch is
 * verified via code review + integration testing on a real Kerberos cluster (see
 * keytab-hostname-auth_测试报告.md).
 */
class HDFSUtilsKeytabHostTest {

  private def resolveKeytabHost(): String = {
    val method = HDFSUtils.getClass.getDeclaredMethod("resolveKeytabHost")
    method.setAccessible(true)
    method.invoke(HDFSUtils).asInstanceOf[String]
  }

  /** host.enabled=false (default) -> no host appended (resolveKeytabHost returns null). */
  @Test
  def testResolveDefaultNoHost: Unit = {
    val host = resolveKeytabHost()
    Assertions.assertNull(host)
  }

  /** getKerberosUser: default host.enabled=false -> principal = userName (no host). */
  @Test
  def testGetKerberosUserNoHostByDefault: Unit = {
    val principal = HDFSUtils.getKerberosUser("hadoop", null)
    Assertions.assertEquals("hadoop", principal)
  }

}
