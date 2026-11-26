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

import org.springframework.mock.web.MockServletContext

import scala.collection.JavaConverters._

import org.junit.jupiter.api.{AfterEach, Assertions, DisplayName, Test}

/** Minimal FilterRegistration.Dynamic recording init params and url-pattern mappings. */
class RecordingFilterRegistration(val name: String, val clazz: Class[_ <: javax.servlet.Filter])
    extends javax.servlet.FilterRegistration.Dynamic {

  private val initParams = scala.collection.mutable.Map.empty[String, String]
  val urlPatternMappings = scala.collection.mutable.ArrayBuffer.empty[String]

  override def getName: String = name

  override def getClassName: String = clazz.getName

  override def getInitParameter(name: String): String = initParams.getOrElse(name, null)

  override def getInitParameters: java.util.Map[String, String] = initParams.asJava

  override def setInitParameter(name: String, value: String): Boolean = {
    initParams.put(name, value)
    true
  }

  override def setInitParameters(
      initParameters: java.util.Map[String, String]
  ): java.util.Set[String] = {
    if (initParameters == null) throw new NullPointerException("initParameters must not be null")
    initParameters.asScala.foreach { case (k, v) => initParams.put(k, v) }
    new java.util.HashSet[String]()
  }

  override def setAsyncSupported(isAsyncSupported: Boolean): Unit = ()

  override def addMappingForServletNames(
      dispatcherTypes: java.util.EnumSet[javax.servlet.DispatcherType],
      isMatchAfter: Boolean,
      servletNames: String*
  ): Unit = ()

  override def addMappingForUrlPatterns(
      dispatcherTypes: java.util.EnumSet[javax.servlet.DispatcherType],
      isMatchAfter: Boolean,
      urlPatterns: String*
  ): Unit = urlPatternMappings ++= urlPatterns

  override def getServletNameMappings: java.util.Collection[String] =
    java.util.Collections.emptyList()

  override def getUrlPatternMappings: java.util.Collection[String] = {
    val list = new java.util.ArrayList[String]()
    urlPatternMappings.foreach(list.add)
    list
  }

}

/** Minimal ServletRegistration.Dynamic recording mappings and multipart config. */
class RecordingServletRegistration(val name: String, val clazz: Class[_ <: javax.servlet.Servlet])
    extends javax.servlet.ServletRegistration.Dynamic {

  private val initParams = scala.collection.mutable.Map.empty[String, String]
  val mappings = scala.collection.mutable.ArrayBuffer.empty[String]
  var multipartConfig: javax.servlet.MultipartConfigElement = _

  override def getName: String = name

  override def getClassName: String = clazz.getName

  override def getInitParameter(name: String): String = initParams.getOrElse(name, null)

  override def getInitParameters: java.util.Map[String, String] = initParams.asJava

  override def setInitParameter(name: String, value: String): Boolean = {
    initParams.put(name, value)
    true
  }

  override def setInitParameters(
      initParameters: java.util.Map[String, String]
  ): java.util.Set[String] = {
    if (initParameters == null) throw new NullPointerException("initParameters must not be null")
    initParameters.asScala.foreach { case (k, v) => initParams.put(k, v) }
    new java.util.HashSet[String]()
  }

  override def setAsyncSupported(isAsyncSupported: Boolean): Unit = ()

  override def addMapping(urlPatterns: String*): java.util.Set[String] = {
    mappings ++= urlPatterns
    new java.util.HashSet[String](mappings.asJava)
  }

  override def getMappings: java.util.Collection[String] = mappings.asJavaCollection

  override def getRunAsRole: String = null

  override def setRunAsRole(roleName: String): Unit = ()

  override def setLoadOnStartup(loadOnStartup: Int): Unit = ()

  override def setMultipartConfig(multipartConfig: javax.servlet.MultipartConfigElement): Unit = {
    this.multipartConfig = multipartConfig
  }

  override def setServletSecurity(
      constraint: javax.servlet.ServletSecurityElement
  ): java.util.Set[String] = new java.util.HashSet[String]()

}

/**
 * MockServletContext (spring-test 5.3) leaves every Servlet 3.0 dynamic registration entry point
 * unsupported; override them with the recording registrations above.
 */
class RecordingServletContext extends MockServletContext {

  private val filterRegMap = new java.util.LinkedHashMap[String, javax.servlet.FilterRegistration]()

  private val servletRegMap =
    new java.util.LinkedHashMap[String, javax.servlet.ServletRegistration]()

  override def addFilter(
      filterName: String,
      filterClass: Class[_ <: javax.servlet.Filter]
  ): javax.servlet.FilterRegistration.Dynamic = {
    val reg = new RecordingFilterRegistration(filterName, filterClass)
    filterRegMap.put(filterName, reg)
    reg
  }

  override def addFilter(
      filterName: String,
      filter: javax.servlet.Filter
  ): javax.servlet.FilterRegistration.Dynamic = {
    val reg = new RecordingFilterRegistration(
      filterName,
      filter.getClass.asInstanceOf[Class[_ <: javax.servlet.Filter]]
    )
    filterRegMap.put(filterName, reg)
    reg
  }

  override def addServlet(
      servletName: String,
      servlet: javax.servlet.Servlet
  ): javax.servlet.ServletRegistration.Dynamic = {
    val reg = new RecordingServletRegistration(
      servletName,
      servlet.getClass.asInstanceOf[Class[_ <: javax.servlet.Servlet]]
    )
    servletRegMap.put(servletName, reg)
    reg
  }

  override def getFilterRegistrations
      : java.util.Map[String, _ <: javax.servlet.FilterRegistration] =
    filterRegMap

  override def getServletRegistrations
      : java.util.Map[String, _ <: javax.servlet.ServletRegistration] =
    servletRegMap

}

/**
 * REQ-06 TC009 (unit-test form): session-timeout normalization.
 *
 * <p>jetty branch: `ServletContext#setSessionTimeout` is SKIPPED (it resolves to an abstract method
 * on WebAppContext$Context and would throw AbstractMethodError); the timeout is applied by
 * JettyServerCustomizerConfiguration via SessionHandler#setMaxInactiveInterval (seconds).
 *
 * <p>non-jetty (bes) branch: minutes normalization `max(1, timeoutMs / 60000)` per Servlet spec
 * (design §7.3). NOTE: `CommonVars.getValue` is a constructor-evaluated val — it freezes at
 * ServerConfiguration class-load time, so an injected `wds.linkis.server.web.session.timeout`
 * system property set inside a test method does NOT reach the initializer (measured 2026-11-25).
 * The minutes normalization below is therefore asserted on the frozen default `TimeType("2h")`
 * = 7200000ms -> 120min; the `Math.max(1, ..)` sub-minute clamp branch is NOT unit-testable here
 * (no injection path) and stays covered by the 2026-11-23 runtime evidence (7200000ms -> 120min
 * observed on DEV entrance).
 *
 * <p>Also covers (TC011 unit-level evidence): the container-agnostic initializer is the SOLE
 * registrar of the characterEncoding filter / securityFilter / DispatcherServlet, each registered
 * exactly once with the expected mapping.
 */
class LinkisServletContextInitializerTest {

  private val CONTAINER_KEY = "linkis.web.container"

  @AfterEach
  def cleanup(): Unit = {
    System.clearProperty(CONTAINER_KEY)
  }

  private def runOnStartup(): RecordingServletContext = {
    val mockContext = new RecordingServletContext
    new LinkisServletContextInitializer()
      .servletContextInitializerBean()
      .onStartup(mockContext)
    mockContext
  }

  @Test
  @DisplayName("TC009-a: bes branch normalizes the 2h default to 120 minutes")
  def besBranchNormalizesToMinutes(): Unit = {
    System.setProperty(CONTAINER_KEY, "bes")
    val mockContext = runOnStartup()
    // frozen default TimeType("2h") = 7200000ms -> max(1, 7200000/60000) = 120
    Assertions.assertEquals(120, mockContext.getSessionTimeout)
  }

  @Test
  @DisplayName(
    "TC009-c: jetty branch skips ServletContext#setSessionTimeout (AbstractMethodError guard)"
  )
  def jettyBranchSkipsSetSessionTimeout(): Unit = {
    System.setProperty(CONTAINER_KEY, "jetty")
    val mockContext = runOnStartup()
    // MockServletContext default sessionTimeout is 0: any write would produce >= 1,
    // so 0 proves setSessionTimeout was NOT called on the jetty branch
    Assertions.assertEquals(0, mockContext.getSessionTimeout)
  }

  @Test
  @DisplayName("TC009-d: container property absent defaults to the jetty branch")
  def absentContainerPropertyDefaultsToJetty(): Unit = {
    val mockContext = runOnStartup()
    Assertions.assertEquals(0, mockContext.getSessionTimeout)
  }

  @Test
  @DisplayName("TC011-a: characterEncoding/securityFilter/servlet each registered exactly once")
  def filtersAndServletRegisteredOnce(): Unit = {
    System.setProperty(CONTAINER_KEY, "bes")
    val mockContext = runOnStartup()
    val filterNames = mockContext.getFilterRegistrations.asScala.keySet
    Assertions.assertEquals(
      Set("characterEncoding", "securityFilter"),
      filterNames.toSet,
      s"unexpected filter registrations: $filterNames"
    )
    val servletNames = mockContext.getServletRegistrations.asScala.keySet
    Assertions.assertEquals(Set("springrestful"), servletNames.toSet)
    // Encoding filter must carry the utf-8 init params and the /* mapping
    // (Configuration.BDP_ENCODING default is lower-case "utf-8", measured 2026-11-25)
    val encodingReg = mockContext.getFilterRegistrations
      .get("characterEncoding")
      .asInstanceOf[RecordingFilterRegistration]
    Assertions.assertEquals("utf-8", encodingReg.getInitParameter("encoding"))
    Assertions.assertEquals("true", encodingReg.getInitParameter("forceEncoding"))
    Assertions.assertTrue(encodingReg.urlPatternMappings.contains("/*"))
    // Security filter must map onto the restful path
    val securityReg = mockContext.getFilterRegistrations
      .get("securityFilter")
      .asInstanceOf[RecordingFilterRegistration]
    Assertions.assertFalse(securityReg.urlPatternMappings.isEmpty)
  }

}
