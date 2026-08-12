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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KerberosTgtUtils {

  private static final Logger logger = LoggerFactory.getLogger(KerberosTgtUtils.class);

  private static volatile Method getSubjectMethod = null;
  private static volatile boolean getSubjectMethodResolved = false;

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
}
