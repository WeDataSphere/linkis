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

package org.apache.linkis.hadoop.common.utils;

import org.apache.hadoop.security.UserGroupInformation;

import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosTicket;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KerberosTgtUtils {

  private static final Logger logger = LoggerFactory.getLogger(KerberosTgtUtils.class);

  private static volatile Method getSubjectMethod = null;
  private static volatile boolean getSubjectMethodResolved = false;

  /** Maximum consecutive refresh failures before throwing an error. */
  private static final int MAX_REFRESH_FAILURES = 3;

  /** Consecutive refresh failure counter for UGI refresh (Hive path). */
  private static final AtomicInteger ugiRefreshFailCount = new AtomicInteger(0);

  /** Consecutive refresh failure counter for login user TGT refresh (JDBC/HBase path). */
  private static final AtomicInteger loginUserRefreshFailCount = new AtomicInteger(0);

  /**
   * Get the Subject from UserGroupInformation via reflection, since getSubject() is protected in
   * Hadoop 2.7.x.
   */
  private static Subject getSubjectFromUgi(UserGroupInformation ugi) {
    if (!getSubjectMethodResolved) {
      synchronized (KerberosTgtUtils.class) {
        if (!getSubjectMethodResolved) {
          try {
            Method method = UserGroupInformation.class.getDeclaredMethod("getSubject");
            method.setAccessible(true);
            getSubjectMethod = method;
          } catch (NoSuchMethodException e) {
            logger.warn("Failed to find getSubject method on UserGroupInformation", e);
            getSubjectMethod = null;
          }
          getSubjectMethodResolved = true;
        }
      }
    }
    if (getSubjectMethod == null) {
      return null;
    }
    try {
      return (Subject) getSubjectMethod.invoke(ugi);
    } catch (Exception e) {
      logger.warn("Failed to invoke getSubject on UserGroupInformation", e);
      return null;
    }
  }

  /**
   * Check if the Kerberos TGT in the given UGI is still valid (not expired). For proxy UGI, the
   * real user's TGT is checked.
   *
   * @param ugi the UserGroupInformation to check
   * @return true if UGI is null, security not enabled, or TGT is still valid; false if TGT has
   *     expired
   */
  public static boolean isTgtValid(UserGroupInformation ugi) {
    if (ugi == null || !UserGroupInformation.isSecurityEnabled()) {
      return true;
    }
    // For proxy UGI, check the real user's tickets
    UserGroupInformation checkUgi = ugi.getRealUser() != null ? ugi.getRealUser() : ugi;
    Subject subject = getSubjectFromUgi(checkUgi);
    if (subject == null) {
      return true;
    }
    Set<KerberosTicket> tickets = subject.getPrivateCredentials(KerberosTicket.class);
    if (tickets == null || tickets.isEmpty()) {
      return true;
    }
    long now = System.currentTimeMillis();
    for (KerberosTicket ticket : tickets) {
      if (ticket.getEndTime().getTime() <= now) {
        return false;
      }
    }
    return true;
  }

  /**
   * Refresh the UGI if its TGT has expired. This method is for engines that hold a long-lived UGI
   * object created via loginUserFromKeytabAndReturnUGI (e.g., Hive EngineConn). It creates a new
   * UGI object via HDFSUtils.getUserGroupInformation instead of modifying the existing one, to
   * avoid concurrency issues.
   *
   * <p>If the TGT is still valid, the original UGI is returned unchanged. If refresh fails, the
   * original UGI is returned (degradation). After {@value #MAX_REFRESH_FAILURES} consecutive
   * failures, a RuntimeException is thrown to alert that the TGT refresh mechanism is broken.
   *
   * @param ugi the original UserGroupInformation to check
   * @param userName the user name for re-login
   * @return a UGI with valid TGT (may be the original or a new one)
   * @throws RuntimeException if TGT refresh has failed consecutively for {@value
   *     #MAX_REFRESH_FAILURES} times
   */
  public static UserGroupInformation refreshUgiIfNeeded(UserGroupInformation ugi, String userName) {
    if (ugi == null || !UserGroupInformation.isSecurityEnabled()) {
      return ugi;
    }
    if (isTgtValid(ugi)) {
      ugiRefreshFailCount.set(0);
      return ugi;
    }
    logger.info("TGT expired, refreshing UGI for user: {}", userName);
    try {
      UserGroupInformation newUgi = HDFSUtils.getUserGroupInformation(userName);
      if (newUgi != null) {
        ugiRefreshFailCount.set(0);
        logger.info("UGI refreshed successfully for user: {}", userName);
        return newUgi;
      } else {
        throw new RuntimeException("HDFSUtils.getUserGroupInformation returned null UGI");
      }
    } catch (Exception e) {
      int failCount = ugiRefreshFailCount.incrementAndGet();
      logger.warn(
          "Failed to refresh UGI for user: {}, consecutive failure count: {}",
          userName,
          failCount,
          e);
      if (failCount >= MAX_REFRESH_FAILURES) {
        throw new RuntimeException(
            "TGT refresh has failed consecutively for "
                + MAX_REFRESH_FAILURES
                + " times (user: "
                + userName
                + "). Please check keytab file, Kerberos configuration and KDC availability.",
            e);
      }
      return ugi;
    }
  }

  /**
   * Refresh the JVM global login user's TGT if expired. This method is for engines that use
   * UserGroupInformation.getLoginUser() (e.g., JDBC, HBase EngineConn). It calls
   * checkTGTAndReloginFromKeytab on the login user.
   *
   * <p>If security is not enabled or refresh fails, no action is taken (degradation). After {@value
   * #MAX_REFRESH_FAILURES} consecutive failures, a RuntimeException is thrown to alert that the TGT
   * refresh mechanism is broken.
   *
   * @throws RuntimeException if TGT refresh has failed consecutively for {@value
   *     #MAX_REFRESH_FAILURES} times
   */
  public static void refreshLoginUserTgtIfNeeded() {
    if (!UserGroupInformation.isSecurityEnabled()) {
      return;
    }
    try {
      UserGroupInformation loginUser = UserGroupInformation.getLoginUser();
      if (!isTgtValid(loginUser)) {
        logger.info("TGT expired for login user, performing relogin from keytab");
        if (UserGroupInformation.isLoginKeytabBased()) {
          loginUser.checkTGTAndReloginFromKeytab();
        } else if (UserGroupInformation.isLoginTicketBased()) {
          loginUser.reloginFromTicketCache();
        }
        // Verify refresh succeeded
        if (isTgtValid(loginUser)) {
          loginUserRefreshFailCount.set(0);
          logger.info("Login user TGT refreshed successfully");
        } else {
          int failCount = loginUserRefreshFailCount.incrementAndGet();
          logger.warn(
              "Login user TGT refresh did not succeed, consecutive failure count: {}", failCount);
          if (failCount >= MAX_REFRESH_FAILURES) {
            throw new RuntimeException(
                "Login user TGT refresh has failed consecutively for "
                    + MAX_REFRESH_FAILURES
                    + " times. Please check keytab file, Kerberos configuration and KDC availability.");
          }
        }
      } else {
        loginUserRefreshFailCount.set(0);
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      int failCount = loginUserRefreshFailCount.incrementAndGet();
      logger.warn("Failed to refresh login user TGT, consecutive failure count: {}", failCount, e);
      if (failCount >= MAX_REFRESH_FAILURES) {
        throw new RuntimeException(
            "Login user TGT refresh has failed consecutively for "
                + MAX_REFRESH_FAILURES
                + " times. Please check keytab file, Kerberos configuration and KDC availability.",
            e);
      }
    }
  }
}
