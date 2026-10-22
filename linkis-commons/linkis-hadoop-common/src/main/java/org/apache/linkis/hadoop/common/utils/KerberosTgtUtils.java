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

import org.apache.linkis.hadoop.common.conf.HadoopConf;

import org.apache.hadoop.security.UserGroupInformation;

import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosTicket;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KerberosTgtUtils {

  private static final Logger logger = LoggerFactory.getLogger(KerberosTgtUtils.class);

  private static final AtomicReference<Method> getSubjectMethod = new AtomicReference<>();
  private static final AtomicBoolean getSubjectMethodResolved = new AtomicBoolean(false);

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
    if (!getSubjectMethodResolved.get()) {
      synchronized (KerberosTgtUtils.class) {
        if (!getSubjectMethodResolved.get()) {
          try {
            Method method = UserGroupInformation.class.getDeclaredMethod("getSubject");
            method.setAccessible(true);
            getSubjectMethod.set(method);
          } catch (NoSuchMethodException e) {
            logger.warn("Failed to find getSubject method on UserGroupInformation", e);
            getSubjectMethod.set(null);
          }
          getSubjectMethodResolved.set(true);
        }
      }
    }
    Method method = getSubjectMethod.get();
    if (method == null) {
      return null;
    }
    try {
      return (Subject) method.invoke(ugi);
    } catch (Exception e) {
      logger.warn("Failed to invoke getSubject on UserGroupInformation", e);
      return null;
    }
  }

  /**
   * Check if the Kerberos TGT in the given UGI is still valid (not expired). For proxy UGI, the
   * real user's TGT is checked.
   *
   * <p>In Kerberos (security-enabled) environments, this method is strict: if the Subject cannot be
   * obtained via reflection or the KerberosTicket set is empty/missing, it returns {@code false} to
   * trigger a refresh. The previous conservative "return true on missing info" strategy caused real
   * TGT failures to be misjudged as valid, skipping refresh — verified by production logs where the
   * second TGT failure had no "TGT expired" log (refreshUgiIfNeeded returned early), causing all
   * subsequent SQL to fail with "No valid credentials provided (Failed to find any Kerberos tgt)".
   * Non-Kerberos environments still return {@code true} (no-op).
   *
   * @param ugi the UserGroupInformation to check
   * @return true if UGI is null, security not enabled, or TGT is still valid; false if TGT has
   *     expired or ticket info is unavailable in a Kerberos environment
   */
  public static boolean isTgtValid(UserGroupInformation ugi) {
    if (ugi == null || !UserGroupInformation.isSecurityEnabled()) {
      return true;
    }
    // For proxy UGI, check the real user's tickets
    UserGroupInformation checkUgi = ugi.getRealUser() != null ? ugi.getRealUser() : ugi;
    Subject subject = getSubjectFromUgi(checkUgi);
    if (subject == null) {
      // Kerberos 环境下拿不到 Subject，说明 UGI 状态异常，判定为无效以触发续期。
      // 原保守策略返回 true 会导致真正过期时不刷新（生产日志验证：第二次 TGT 失效
      // 时反射拿不到 Subject，误判有效跳过续期，后续 SQL 全部认证失败）。
      logger.warn(
          "Cannot get Subject from UGI for user {}, treating TGT as invalid to trigger refresh"
              + "（无法获取UGI的Subject，判定TGT无效以触发续期）",
          checkUgi.getUserName());
      return false;
    }
    Set<KerberosTicket> tickets = subject.getPrivateCredentials(KerberosTicket.class);
    if (tickets == null || tickets.isEmpty()) {
      // Kerberos 环境下票据集合为空，说明 TGT 已被移除或未注入，判定为无效以触发续期。
      logger.warn(
          "No KerberosTicket found in UGI Subject for user {}, treating TGT as invalid to trigger"
              + " refresh（UGI Subject中未找到Kerberos票据，判定TGT无效以触发续期）",
          checkUgi.getUserName());
      return false;
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
   * 返回UGI中TGT的剩余有效毫秒数，取所有KerberosTicket中最早的过期时间计算剩余。用于"提前续期"判断
   * （剩余小于阈值即续期，而非等过期后才续），避免MR/长任务执行中途TGT过期导致认证失败。
   *
   * <p>生产日志28025验证：10649958入口检查时TGT有效（isTgtValid返回true），但MR跑22秒中途TGT过期， 查job status时"No valid
   * credentials"。改为剩余时间判断后，入口处剩余不足阈值即提前续期，MR拿到新TGT。
   *
   * @return 非Kerberos环境返回Long.MAX_VALUE（永不续期）；拿不到Subject/票据或异常返回-1（视为需续期）； 否则返回最早过期票据的剩余毫秒数
   */
  public static long getTgtRemainingMillis(UserGroupInformation ugi) {
    if (ugi == null || !UserGroupInformation.isSecurityEnabled()) {
      return Long.MAX_VALUE;
    }
    try {
      // For proxy UGI, check the real user's tickets
      UserGroupInformation checkUgi = ugi.getRealUser() != null ? ugi.getRealUser() : ugi;
      Subject subject = getSubjectFromUgi(checkUgi);
      if (subject == null) {
        return -1;
      }
      Set<KerberosTicket> tickets = subject.getPrivateCredentials(KerberosTicket.class);
      if (tickets == null || tickets.isEmpty()) {
        return -1;
      }
      long now = System.currentTimeMillis();
      long minRemaining = Long.MAX_VALUE;
      for (KerberosTicket ticket : tickets) {
        long remaining = ticket.getEndTime().getTime() - now;
        if (remaining < minRemaining) {
          minRemaining = remaining;
        }
      }
      return minRemaining;
    } catch (Exception e) {
      // 反射/反射调用异常时降级为-1，触发续期（保守策略，宁可多续一次也不漏续）
      logger.warn(
          "Failed to get TGT remaining millis for user {}, treating as expired to trigger refresh"
              + "（获取TGT剩余时间异常，判定需续期）",
          ugi.getUserName(),
          e);
      return -1;
    }
  }

  /**
   * Refresh the UGI if its TGT has expired. This method is for engines that hold a long-lived UGI
   * object created via HDFSUtils.getUserGroupInformation (e.g., Hive EngineConn).
   *
   * <p>Strategy: ① first try in-place relogin via {@code checkTGTAndReloginFromKeytab()} on the
   * realUser (proxy UGI scenario) or the UGI itself, keeping the same UGI/Subject reference and
   * clearing the Hadoop FileSystem cache so subsequent {@code FileSystem.get()} rebuilds with the
   * new TGT; ② if in-place relogin is not possible (non-keytab UGI) or fails, fall back to creating
   * a new UGI via HDFSUtils.getUserGroupInformation and also clear the old FS cache.
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
    // 提前续期：用剩余时间判断而非"已过期才续"。剩余<阈值即续，避免MR/长任务中途TGT过期
    // （生产日志28025：10649958入口TGT有效，MR跑22秒中途过期，查job status认证失败）。
    long remainingMs = getTgtRemainingMillis(ugi);
    long thresholdMs = HadoopConf.ENGINE_TGT_REFRESH_AHEAD_SECONDS() * 1000L;
    if (remainingMs >= thresholdMs) {
      ugiRefreshFailCount.set(0);
      return ugi;
    }
    logger.info(
        "TGT need refresh, remaining {}ms < threshold {}ms, refreshing UGI for user: {}"
            + "（TGT剩余{}ms小于阈值{}ms，触发续期）",
        remainingMs,
        thresholdMs,
        userName,
        remainingMs,
        thresholdMs);
    // 防线1: 优先原地续票（checkTGTAndReloginFromKeytab 在同一 Subject 内换新票，
    //   不换 UGI 对象引用，杜绝引用漂移导致 FS 缓存绑定不一致）。
    //   proxy 场景下真正持有 keytab/TGT 的是 realUser，必须对 realUser 续票，
    //   否则静态 FS 缓存里的 DFSClient 绑定的仍是旧 Subject 中的旧票。
    try {
      UserGroupInformation refreshTarget = (ugi.getRealUser() != null) ? ugi.getRealUser() : ugi;
      if (refreshTarget.isFromKeytab()) {
        refreshTarget.checkTGTAndReloginFromKeytab();
        if (isTgtValid(ugi)) {
          ugiRefreshFailCount.set(0);
          logger.info("UGI relogin in-place successfully for user: {}", userName);
          // 原地续期不换 UGI 引用，已缓存的 FileSystem 天然复用新 TGT，无需 closeAllForUGI 清缓存。
          // 清缓存会关闭并发任务正使用的 FileSystem，触发 "Filesystem closed" 异常（Hive SemanticAnalyzer
          // 检查路径加密时即受此影响）。
          return ugi;
        } else {
          logger.warn(
              "checkTGTAndReloginFromKeytab completed but TGT still invalid for user: {}, "
                  + "will fallback to new UGI（原地续票后TGT仍无效，降级到换新UGI兜底）",
              userName);
        }
      } else {
        logger.warn(
            "refreshTarget is not from keytab for user: {}, will fallback to new UGI"
                + "（目标UGI非keytab登录，不可原地续票，降级到换新UGI兜底）",
            userName);
      }
    } catch (Exception reloginEx) {
      // checkTGTAndReloginFromKeytab 抛异常：不直接报错，降级到换新 UGI 兜底
      logger.warn(
          "checkTGTAndReloginFromKeytab failed for user: {}, will fallback to new UGI"
              + "（原地续票异常，降级到换新UGI兜底）",
          userName,
          reloginEx);
    }
    // 兜底路径：用原来的逻辑换新 UGI（HDFSUtils.getUserGroupInformation）
    try {
      UserGroupInformation newUgi = HDFSUtils.getUserGroupInformation(userName);
      if (newUgi != null) {
        ugiRefreshFailCount.set(0);
        logger.info("UGI refreshed via new UGI fallback successfully for user: {}", userName);
        // 不清旧 FS 缓存：并发场景下其他任务可能正持有旧 UGI 的 FileSystem，closeAllForUGI 会误伤
        // 导致 "Filesystem closed" 异常。
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
