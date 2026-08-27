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

package org.apache.linkis.jobhistory.entity;

public class DiagnosisResult {

  private Long taskID;
  private String taskStatus;
  private String diagnosisType;
  private String failedStage;
  private Integer errorCode;
  private String errorDesc;
  private String engineType;
  private String engineConnInstance;
  private Object yarnResource;

  public DiagnosisResult() {}

  public static DiagnosisResult linkis(
      Long taskID, String taskStatus, String failedStage, Integer errorCode, String errorDesc) {
    DiagnosisResult result = new DiagnosisResult();
    result.setTaskID(taskID);
    result.setTaskStatus(taskStatus);
    result.setDiagnosisType("LINKIS");
    result.setFailedStage(failedStage);
    result.setErrorCode(errorCode);
    result.setErrorDesc(errorDesc);
    return result;
  }

  public static DiagnosisResult underlying(
      Long taskID,
      String taskStatus,
      String failedStage,
      Integer errorCode,
      String errorDesc,
      String engineType,
      String engineConnInstance,
      Object yarnResource) {
    DiagnosisResult result = new DiagnosisResult();
    result.setTaskID(taskID);
    result.setTaskStatus(taskStatus);
    result.setDiagnosisType("UNDERLYING");
    result.setFailedStage(failedStage);
    result.setErrorCode(errorCode);
    result.setErrorDesc(errorDesc);
    result.setEngineType(engineType);
    result.setEngineConnInstance(engineConnInstance);
    result.setYarnResource(yarnResource);
    return result;
  }

  public static DiagnosisResult unknown(
      Long taskID, String taskStatus, String failedStage, Integer errorCode, String errorDesc) {
    DiagnosisResult result = new DiagnosisResult();
    result.setTaskID(taskID);
    result.setTaskStatus(taskStatus);
    result.setDiagnosisType("UNKNOWN");
    result.setFailedStage(failedStage);
    result.setErrorCode(errorCode);
    result.setErrorDesc(errorDesc);
    return result;
  }

  public Long getTaskID() {
    return taskID;
  }

  public void setTaskID(Long taskID) {
    this.taskID = taskID;
  }

  public String getTaskStatus() {
    return taskStatus;
  }

  public void setTaskStatus(String taskStatus) {
    this.taskStatus = taskStatus;
  }

  public String getDiagnosisType() {
    return diagnosisType;
  }

  public void setDiagnosisType(String diagnosisType) {
    this.diagnosisType = diagnosisType;
  }

  public String getFailedStage() {
    return failedStage;
  }

  public void setFailedStage(String failedStage) {
    this.failedStage = failedStage;
  }

  public Integer getErrorCode() {
    return errorCode;
  }

  public void setErrorCode(Integer errorCode) {
    this.errorCode = errorCode;
  }

  public String getErrorDesc() {
    return errorDesc;
  }

  public void setErrorDesc(String errorDesc) {
    this.errorDesc = errorDesc;
  }

  public String getEngineType() {
    return engineType;
  }

  public void setEngineType(String engineType) {
    this.engineType = engineType;
  }

  public String getEngineConnInstance() {
    return engineConnInstance;
  }

  public void setEngineConnInstance(String engineConnInstance) {
    this.engineConnInstance = engineConnInstance;
  }

  public Object getYarnResource() {
    return yarnResource;
  }

  public void setYarnResource(Object yarnResource) {
    this.yarnResource = yarnResource;
  }
}
