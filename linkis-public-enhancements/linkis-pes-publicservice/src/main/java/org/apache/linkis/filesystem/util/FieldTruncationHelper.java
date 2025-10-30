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

package org.apache.linkis.filesystem.util;

import org.apache.linkis.filesystem.conf.WorkSpaceConfiguration;
import org.apache.linkis.filesystem.entity.FieldTruncationResult;
import org.apache.linkis.filesystem.entity.OversizedFieldInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Helper class for field truncation detection and processing */
public class FieldTruncationHelper {
  private static final Logger logger = LoggerFactory.getLogger(FieldTruncationHelper.class);

  /**
   * Detect and handle oversized fields in result set
   *
   * @param metadata Column names list
   * @param FileContent Data rows list (each row is an ArrayList or Object[])
   * @param maxLength Maximum field length threshold
   * @param truncate Whether to truncate (false means detection only)
   * @return FieldTruncationResult containing detection results and processed data
   */
  public static FieldTruncationResult detectAndHandle(
      Object metadata, List<String[]> FileContent, int maxLength, boolean truncate) {

    if (metadata == null || !(metadata instanceof Map[])) {
      return new FieldTruncationResult();
    }

    // 2. 类型转换（已通过校验，可安全强转）
    Map<String, Object>[] originalMaps = (Map<String, Object>[]) metadata;

    // 提取列名
    List<String> columnNames = new ArrayList<>();
    if (metadata != null) {
      for (Map meta : originalMaps) {
        Object columnName = meta.get("columnName");
        columnNames.add(columnName != null ? columnName.toString() : "");
      }
    }

    // 转换 String[] 数组为 ArrayList<String>
    List<ArrayList<String>> dataList = new ArrayList<>();
    for (String[] row : FileContent) {
      ArrayList<String> rowList = new ArrayList<>(Arrays.asList(row));
      dataList.add(rowList);
    }

    int maxCount = WorkSpaceConfiguration.OVERSIZED_FIELD_MAX_COUNT.getValue();

    // Detect oversized fields
    List<OversizedFieldInfo> oversizedFields =
        detectOversizedFields(columnNames, dataList, maxLength, maxCount);

    boolean hasOversizedFields = !oversizedFields.isEmpty();

    // Truncate if requested
    List<ArrayList<String>> processedData = dataList;
    if (truncate && hasOversizedFields) {
      processedData = truncateFields(columnNames, dataList, maxLength);
    }

    return new FieldTruncationResult(hasOversizedFields, oversizedFields, maxCount, processedData);
  }

  /**
   * Detect oversized fields
   *
   * @param metadata Column names
   * @param dataList Data rows
   * @param maxLength Max length threshold
   * @param maxCount Max number of oversized fields to collect
   * @return List of oversized field info
   */
  private static List<OversizedFieldInfo> detectOversizedFields(
      List<String> metadata, List<ArrayList<String>> dataList, int maxLength, int maxCount) {

    List<OversizedFieldInfo> oversizedFields = new ArrayList<>();

    if (metadata == null || dataList == null || dataList.isEmpty()) {
      return oversizedFields;
    }

    // Iterate through data rows
    for (int rowIndex = 0; rowIndex < dataList.size(); rowIndex++) {
      if (oversizedFields.size() >= maxCount) {
        break; // Stop if we've collected enough
      }

      ArrayList<String> row = dataList.get(rowIndex);
      if (row == null) {
        continue;
      }

      // Check each field in the row
      for (int colIndex = 0; colIndex < row.size() && colIndex < metadata.size(); colIndex++) {
        if (oversizedFields.size() >= maxCount) {
          break;
        }

        String fieldValue = row.get(colIndex);
        int fieldLength = getFieldLength(fieldValue);

        if (fieldLength > maxLength) {
          String fieldName = metadata.get(colIndex);
          OversizedFieldInfo info =
              new OversizedFieldInfo(fieldName, rowIndex, fieldLength, maxLength);
          oversizedFields.add(info);

          logger.info(
              "Detected oversized field: field={}, row={}, actualLength={}, maxLength={}",
              fieldName,
              rowIndex,
              fieldLength,
              maxLength);
        }
      }
    }

    return oversizedFields;
  }

  /**
   * Truncate oversized fields
   *
   * @param metadata Column names
   * @param dataList Data rows
   * @param maxLength Max length
   * @return Truncated data list
   */
  private static List<ArrayList<String>> truncateFields(
      List<String> metadata, List<ArrayList<String>> dataList, int maxLength) {

    if (dataList == null || dataList.isEmpty()) {
      return dataList;
    }

    List<ArrayList<String>> truncatedData = new ArrayList<>();

    for (ArrayList<String> row : dataList) {
      if (row == null) {
        truncatedData.add(null);
        continue;
      }

      ArrayList<String> truncatedRow = new ArrayList<>();
      for (String fieldValue : row) {
        String truncatedValue = truncateFieldValue(fieldValue, maxLength);
        truncatedRow.add(truncatedValue);
      }
      truncatedData.add(truncatedRow);
    }

    return truncatedData;
  }

  /**
   * Get field value character length
   *
   * @param value Field value
   * @return Character length
   */
  private static int getFieldLength(Object value) {
    if (value == null) {
      return 0;
    }
    return value.toString().length();
  }

  /**
   * Truncate single field value
   *
   * @param value Field value
   * @param maxLength Max length
   * @return Truncated value
   */
  private static String truncateFieldValue(Object value, int maxLength) {
    if (value == null) {
      return null;
    }
    String str = value.toString();
    if (str.length() <= maxLength) {
      return str;
    }
    return str.substring(0, maxLength);
  }
}
