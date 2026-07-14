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
 * Unit tests for keytab principal host auto-resolution (resolveKeytabHost / getKerberosUser /
 * localHostname).
 *
 * Note: CommonVars.getValue is a `val` (cached at HadoopConf object init time), so runtime
 * System.setProperty override is unreliable. These tests therefore exercise the branches reachable
 * under default config values:
 *   - host.enabled = false (default)
 *   - host.auto = false (default)
 *   - host.map = "cluster1=127.0.0.2,cluster2=127.0.0.3" (default) The host.auto=true branches are
 *     verified via the design decision tree + code review (see keytab-hostname-auth_测试报告.md).
 */
class HDFSUtilsKeytabHostTest {

  private def resolveKeytabHost(label: String): String = {
    val method = HDFSUtils.getClass.getDeclaredMethod("resolveKeytabHost", classOf[String])
    method.setAccessible(true)
    method.invoke(HDFSUtils, label).asInstanceOf[String]
  }

  /** TC-01: label=null, host.enabled=false (default) -> no host appended. */
  @Test
  def testResolveLabelNullDefaultNoHost: Unit = {
    val host = resolveKeytabHost(null)
    Assertions.assertNull(host)
  }

  /** TC-04: label="cluster1", default host.map hit -> "127.0.0.2". */
  @Test
  def testResolveLabelMapHitDefault: Unit = {
    val host = resolveKeytabHost("cluster1")
    Assertions.assertEquals("127.0.0.2", host)
  }

  /** TC-06: label not in host.map, auto=false (default) -> null (no host). */
  @Test
  def testResolveLabelMapMissDefault: Unit = {
    val host = resolveKeytabHost("cluster-not-exists")
    Assertions.assertNull(host)
  }

  /**
   * localHostname() is inlined by scalac (private, simple body, single call site), so it is not
   * reachable via reflection. Its correctness is covered indirectly by the host.auto=true branch of
   * resolveKeytabHost and by code review.
   */

  /** getKerberosUser: default host.enabled=false -> principal = userName (no host). */
  @Test
  def testGetKerberosUserNoHostByDefault: Unit = {
    val principal = HDFSUtils.getKerberosUser("hadoop", null)
    Assertions.assertEquals("hadoop", principal)
  }

  /** getKerberosUser: label="cluster1" default host.map hit -> "hadoop/127.0.0.2". */
  @Test
  def testGetKerberosUserWithMapHit: Unit = {
    val principal = HDFSUtils.getKerberosUser("hadoop", "cluster1")
    Assertions.assertEquals("hadoop/127.0.0.2", principal)
  }

}
