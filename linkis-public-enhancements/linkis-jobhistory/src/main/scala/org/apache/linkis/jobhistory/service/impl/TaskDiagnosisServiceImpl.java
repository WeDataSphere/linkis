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

package org.apache.linkis.jobhistory.service.impl;

import org.apache.linkis.common.conf.Configuration;
import org.apache.linkis.jobhistory.conf.TaskDiagnosisConfiguration;
import org.apache.linkis.jobhistory.entity.DiagnosisResult;
import org.apache.linkis.jobhistory.entity.JobHistory;
import org.apache.linkis.jobhistory.service.JobHistoryQueryService;
import org.apache.linkis.jobhistory.service.TaskDiagnosisService;
import org.apache.linkis.jobhistory.transitional.TaskStatus;
import org.apache.linkis.jobhistory.util.JobhistoryUtils;

import org.apache.commons.lang3.StringUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class TaskDiagnosisServiceImpl implements TaskDiagnosisService {

  private static final Logger logger =
      LoggerFactory.getLogger(TaskDiagnosisServiceImpl.class);

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Autowired private JobHistoryQueryService jobHistoryQueryService;

  /**
   * Mapping from Linkis error code to [module, reason] for logging purposes.
   */
  private static final HashMap<Integer, String[]> LINKIS_ERROR_CODE_MAP = new HashMap<>();

  static {
    LINKIS_ERROR_CODE_MAP.put(20039, new String[] {"interceptor", "Interceptor error"});
    LINKIS_ERROR_CODE_MAP.put(12003, new String[] {"ecm", "ECM error"});
    LINKIS_ERROR_CODE_MAP.put(40102, new String[] {"engineconn", "EngineConn error"});
    LINKIS_ERROR_CODE_MAP.put(40103, new String[] {"engineconn", "EngineConn error"});
    LINKIS_ERROR_CODE_MAP.put(40100, new String[] {"engineconn", "EngineConn error"});
    LINKIS_ERROR_CODE_MAP.put(40105, new String[] {"engineconn", "EngineConn error"});
    LINKIS_ERROR_CODE_MAP.put(20010, new String[] {"entrance", "Entrance error"});
    LINKIS_ERROR_CODE_MAP.put(20011, new String[] {"entrance", "Entrance error"});
    LINKIS_ERROR_CODE_MAP.put(20052, new String[] {"jobhistory", "JobHistory error"});
  }

  @Override
  public DiagnosisResult classify(Long taskID, String username) {
    logger.info("Starting task diagnosis classification for taskID: {}, username: {}", taskID, username);

    JobHistory jobHistory = getJobHistoryWithPermission(taskID, username);
    if (jobHistory == null) {
      logger.warn("JobHistory not found for taskID: {}, username: {}", taskID, username);
      return null;
    }

    String taskStatus = jobHistory.getStatus();
    Integer errorCode = jobHistory.getErrorCode();
    String errorDesc = jobHistory.getErrorDesc();

    if ("Succeed".equals(taskStatus)) {
      logger.info("Task {} completed successfully", taskID);
      DiagnosisResult result = new DiagnosisResult();
      result.setTaskID(taskID);
      result.setTaskStatus(taskStatus);
      result.setDiagnosisType("SUCCESS");
      return result;
    }

    Map<String, Object> metricsMap = parseMetrics(jobHistory.getMetrics());

    if (isFailedStatus(taskStatus)) {
      logger.info("Task {} is in failed status: {}, applying failed diagnosis", taskID, taskStatus);
      return applyFailedDiagnosis(taskID, taskStatus, errorCode, errorDesc, jobHistory, metricsMap);
    }

    logger.info("Task {} is in progress: {}, diagnosing blocking stage", taskID, taskStatus);
    return buildRunningTaskResult(taskID, taskStatus, jobHistory, metricsMap);
  }

  /**
   * Apply stage-first classification for failed tasks.
   * 1. Determine stage from metrics (Scheduling vs Execution)
   * 2. UNDERLYING keywords override everything
   * 3. Scheduling stage defaults to LINKIS; Execution stage applies errorCode → Linkis keyword → UNKNOWN
   */
  private DiagnosisResult applyFailedDiagnosis(
      Long taskID, String taskStatus, Integer errorCode, String errorDesc,
      JobHistory jobHistory, Map<String, Object> metricsMap) {

    String stage = determineFailedStage(metricsMap);
    String keywordResult = applyKeywordRule(errorDesc);

    // UNDERLYING keywords override stage
    if ("UNDERLYING".equals(keywordResult)) {
      String engineInstance = metricsMap != null ? (String) metricsMap.get("engineInstance") : null;
      String engineType = jobHistory.getEngineType();
      Object yarnResource = metricsMap != null ? metricsMap.get("yarnResource") : null;
      logger.info("Task {} classified as UNDERLYING by keyword rule at {} stage", taskID, stage);
      return DiagnosisResult.underlying(
          taskID, taskStatus, stage, errorCode, errorDesc,
          engineType, engineInstance, yarnResource);
    }

    // Scheduling stage: Linkis keyword → errorCode → default LINKIS
    if ("Scheduling".equals(stage)) {
      if ("LINKIS".equals(keywordResult)) {
        logger.info("Task {} classified as LINKIS by keyword rule at Scheduling stage", taskID);
        return DiagnosisResult.linkis(taskID, taskStatus, "Scheduling", errorCode, errorDesc);
      }
      String errorCodeResult = applyErrorCodeRule(errorCode);
      if ("LINKIS".equals(errorCodeResult)) {
        logLinkisErrorCode(taskID, errorCode, "Scheduling");
        return DiagnosisResult.linkis(taskID, taskStatus, "Scheduling", errorCode, errorDesc);
      }
      logger.info("Task {} classified as LINKIS at Scheduling stage (default)", taskID);
      return DiagnosisResult.linkis(taskID, taskStatus, "Scheduling", errorCode, errorDesc);
    }

    // Execution stage: errorCode → Linkis keyword → default UNKNOWN
    String errorCodeResult = applyErrorCodeRule(errorCode);
    if (errorCodeResult != null) {
      if ("LINKIS".equals(errorCodeResult)) {
        logLinkisErrorCode(taskID, errorCode, "Execution");
        return DiagnosisResult.linkis(taskID, taskStatus, "Execution", errorCode, errorDesc);
      } else {
        String engineInstance = metricsMap != null ? (String) metricsMap.get("engineInstance") : null;
        String engineType = jobHistory.getEngineType();
        Object yarnResource = metricsMap != null ? metricsMap.get("yarnResource") : null;
        logger.info("Task {} classified as UNDERLYING by error code range {} at Execution stage", taskID, errorCode);
        return DiagnosisResult.underlying(
            taskID, taskStatus, "Execution", errorCode, errorDesc,
            engineType, engineInstance, yarnResource);
      }
    }

    if ("LINKIS".equals(keywordResult)) {
      logger.info("Task {} classified as LINKIS by keyword rule at Execution stage", taskID);
      return DiagnosisResult.linkis(taskID, taskStatus, "Execution", errorCode, errorDesc);
    }

    logger.info("Task {} classified as UNKNOWN at Execution stage (no matching rule)", taskID);
    return DiagnosisResult.unknown(taskID, taskStatus, "Execution", errorCode, errorDesc);
  }

  private void logLinkisErrorCode(Long taskID, Integer errorCode, String stage) {
    String[] moduleInfo = LINKIS_ERROR_CODE_MAP.get(errorCode);
    String module = moduleInfo != null ? moduleInfo[0] : "unknown";
    String reason = moduleInfo != null ? moduleInfo[1] : "Linkis error code: " + errorCode;
    logger.info("Task {} classified as LINKIS by error code {} at {} stage, module: {}, reason: {}",
        taskID, errorCode, stage, module, reason);
  }

  /**
   * Determine which stage the failed task reached based on metrics.
   * 1. yarnResource with applicationId → Execution (strongest signal)
   * 2. engineInstance + jobToECTime → Execution
   * 3. engineInstance only → Scheduling
   * 4. No engineInstance → Scheduling
   */
  private String determineFailedStage(Map<String, Object> metricsMap) {
    if (metricsMap == null) {
      return "Scheduling";
    }
    String applicationId = extractApplicationId(metricsMap);
    if (applicationId != null) {
      return "Execution";
    }
    Object engineInstance = metricsMap.get("engineInstance");
    if (engineInstance == null || StringUtils.isBlank(engineInstance.toString())) {
      return "Scheduling";
    }
    Object jobToECTime = metricsMap.get("jobToECTime");
    if (jobToECTime == null) {
      return "Scheduling";
    }
    return "Execution";
  }

  /**
   * Build diagnosis result for running/in-progress tasks based on metrics signals.
   * Uses determineFailedStage() for consistent stage determination.
   */
  private DiagnosisResult buildRunningTaskResult(
      Long taskID, String taskStatus, JobHistory jobHistory, Map<String, Object> metricsMap) {

    String stage = determineFailedStage(metricsMap);
    Object engineInstance = metricsMap != null ? metricsMap.get("engineInstance") : null;
    String engineInstanceStr = (engineInstance != null && StringUtils.isNotBlank(engineInstance.toString()))
        ? engineInstance.toString() : null;

    if ("Scheduling".equals(stage)) {
      logger.info("Running task {} blocked at Scheduling stage", taskID);
      DiagnosisResult result = DiagnosisResult.linkis(taskID, taskStatus, "Scheduling", null, null);
      if (engineInstanceStr != null) {
        result.setEngineConnInstance(engineInstanceStr);
        result.setEngineType(jobHistory.getEngineType());
      }
      return result;
    }

    // Execution stage
    Object yarnResource = metricsMap != null ? metricsMap.get("yarnResource") : null;
    logger.info("Running task {} executing in engine, blocked at underlying", taskID);
    return DiagnosisResult.underlying(
        taskID, taskStatus, "Execution", null, null,
        jobHistory.getEngineType(), engineInstanceStr, yarnResource);
  }

  private JobHistory getJobHistoryWithPermission(Long taskID, String username) {
    boolean isAdmin = Configuration.isJobHistoryAdmin(username) || Configuration.isAdmin(username);
    boolean isDepartmentAdmin = Configuration.isDepartmentAdmin(username);

    if (isAdmin) {
      return jobHistoryQueryService.getJobHistoryByIdAndName(taskID, null);
    } else if (isDepartmentAdmin) {
      String departmentId = JobhistoryUtils.getDepartmentByuser(username);
      if (StringUtils.isNotBlank(departmentId)) {
        List<JobHistory> list =
            jobHistoryQueryService.search(
                taskID, null, null, null, null, null, null, null, null, departmentId, null, null);
        if (list != null && !list.isEmpty()) {
          return list.get(0);
        }
      }
      return null;
    } else {
      return jobHistoryQueryService.getJobHistoryByIdAndName(taskID, username);
    }
  }

  private boolean isFailedStatus(String taskStatus) {
    if (StringUtils.isBlank(taskStatus)) {
      return false;
    }
    try {
      TaskStatus status = TaskStatus.valueOf(taskStatus);
      return status == TaskStatus.Failed
          || status == TaskStatus.Cancelled
          || status == TaskStatus.Timeout;
    } catch (IllegalArgumentException e) {
      logger.warn("Unknown task status: {}", taskStatus);
      return false;
    }
  }

  private Map<String, Object> parseMetrics(String metricsJson) {
    if (StringUtils.isBlank(metricsJson)) {
      return null;
    }
    try {
      @SuppressWarnings("unchecked")
      Map<String, Object> map = OBJECT_MAPPER.readValue(metricsJson, Map.class);
      return map;
    } catch (Exception t) {
      logger.warn("Failed to parse metrics JSON: {}", t.getMessage());
      return null;
    }
  }

  private String applyErrorCodeRule(Integer errorCode) {
    if (errorCode == null) {
      return null;
    }

    String linkisErrorCodesStr = TaskDiagnosisConfiguration.LINKIS_ERROR_CODES().getHotValue();
    Set<Integer> linkisErrorCodes = new HashSet<>();
    if (StringUtils.isNotBlank(linkisErrorCodesStr)) {
      for (String codeStr : linkisErrorCodesStr.split(",")) {
        try {
          linkisErrorCodes.add(Integer.parseInt(codeStr.trim()));
        } catch (NumberFormatException e) {
          logger.warn("Invalid Linkis error code in config: {}", codeStr.trim());
        }
      }
    }

    if (linkisErrorCodes.contains(errorCode)) {
      return "LINKIS";
    }

    int rangeStart =
        (Integer) TaskDiagnosisConfiguration.ENGINE_ERROR_CODE_RANGE_START().getHotValue();
    int rangeEnd =
        (Integer) TaskDiagnosisConfiguration.ENGINE_ERROR_CODE_RANGE_END().getHotValue();
    if (errorCode >= rangeStart && errorCode <= rangeEnd) {
      return "UNDERLYING";
    }

    return null;
  }

  private String applyKeywordRule(String errorDesc) {
    if (StringUtils.isBlank(errorDesc)) {
      return "UNKNOWN";
    }

    List<Pattern> underlyingPatterns = compileUnderlyingKeywords();
    for (Pattern pattern : underlyingPatterns) {
      if (pattern.matcher(errorDesc).find()) {
        return "UNDERLYING";
      }
    }

    List<Pattern> linkisPatterns = compileLinkisKeywords();
    for (Pattern pattern : linkisPatterns) {
      if (pattern.matcher(errorDesc).find()) {
        return "LINKIS";
      }
    }

    return "UNKNOWN";
  }

  private String extractApplicationId(Map<String, Object> metricsMap) {
    if (metricsMap == null) {
      return null;
    }
    Object yarnResourceObj = metricsMap.get("yarnResource");
    if (yarnResourceObj instanceof Map) {
      @SuppressWarnings("unchecked")
      Map<String, Object> yarnResourceMap = (Map<String, Object>) yarnResourceObj;
      if (!yarnResourceMap.isEmpty()) {
        return yarnResourceMap.keySet().iterator().next();
      }
    }
    return null;
  }

  private List<Pattern> compileUnderlyingKeywords() {
    String keywordsStr = TaskDiagnosisConfiguration.UNDERLYING_KEYWORDS().getHotValue();
    return compileKeywordPatterns(keywordsStr);
  }

  private List<Pattern> compileLinkisKeywords() {
    String keywordsStr = TaskDiagnosisConfiguration.LINKIS_KEYWORDS().getHotValue();
    return compileKeywordPatterns(keywordsStr);
  }

  private List<Pattern> compileKeywordPatterns(String keywordsStr) {
    if (StringUtils.isBlank(keywordsStr)) {
      return Collections.emptyList();
    }
    return Arrays.stream(keywordsStr.split(","))
        .filter(StringUtils::isNotBlank)
        .map(keyword -> Pattern.compile(Pattern.quote(keyword.trim()), Pattern.CASE_INSENSITIVE))
        .collect(Collectors.toList());
  }
}
