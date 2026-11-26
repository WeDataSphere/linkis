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

package org.apache.linkis.server.conf

import org.springframework.boot.test.context.assertj.AssertableApplicationContext
import org.springframework.boot.test.context.runner.{ApplicationContextRunner, ContextConsumer}

import org.junit.jupiter.api.{Assertions, DisplayName, Test}

/**
 * REQ-06 TC001 (unit-test form): the Jetty customizer bean must be guarded by the double condition
 * `@ConditionalOnClass(JettyServletWebServerFactory)` + `@ConditionalOnProperty(
 * linkis.web.container=jetty, matchIfMissing=true)`, so that "customizer loaded but not applied"
 * (R-001) can never happen under bes/non-jetty deployments.
 *
 * <p>Note: Scala 2.11 has no SAM conversion — the ContextConsumer must be an anonymous class.
 */
class JettyServerCustomizerConfigurationTest {

  private def runner(): ApplicationContextRunner =
    new ApplicationContextRunner()
      .withUserConfiguration(classOf[JettyServerCustomizerConfiguration])

  private def consumer(expectBean: Boolean): ContextConsumer[AssertableApplicationContext] =
    new ContextConsumer[AssertableApplicationContext] {

      override def accept(context: AssertableApplicationContext): Unit = {
        if (expectBean) {
          context.assertThat().hasBean("jettyFactoryCustomizer")
        } else {
          context.assertThat().doesNotHaveBean("jettyFactoryCustomizer")
        }
      }

    }

  @Test
  @DisplayName("TC001-a: linkis.web.container=jetty loads the jetty customizer bean")
  def jettyPropertyLoadsBean(): Unit = {
    runner()
      .withPropertyValues("linkis.web.container=jetty")
      .run(consumer(expectBean = true))
  }

  @Test
  @DisplayName("TC001-b: linkis.web.container=bes keeps the customizer bean away (no dangling)")
  def besPropertySkipsBean(): Unit = {
    runner()
      .withPropertyValues("linkis.web.container=bes")
      .run(consumer(expectBean = false))
  }

  @Test
  @DisplayName("TC001-c: property absent falls back to the default jetty branch (matchIfMissing)")
  def missingPropertyDefaultsToJetty(): Unit = {
    runner().run(consumer(expectBean = true))
  }

  @Test
  @DisplayName("TC006-a: illegal value JETTY — Spring layer matches havingValue case-insensitively")
  def illegalValueStillMatchesInSpringLayer(): Unit = {
    // Measured fact (2026-11-25): @ConditionalOnProperty(havingValue="jetty") matches "JETTY"
    // case-insensitively, so the bean IS still loaded under the illegal upper-case value —
    // different from the script layer's POSIX case exact-match (JETTY -> illegal -> fallback
    // jetty). On the real chain the script normalizes before injecting
    // -Dlinkis.web.container=$CONTAINER_MODE, and "fallback jetty" + "bean still loaded"
    // converge to the same jetty outcome, so the two layers agree end-to-end.
    runner()
      .withPropertyValues("linkis.web.container=JETTY")
      .run(consumer(expectBean = true))
  }

}
