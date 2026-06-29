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

package org.apache.linkis.metadata.query.service;

import org.apache.linkis.common.utils.AESUtils;
import org.apache.linkis.datasourcemanager.common.util.json.Json;
import org.apache.linkis.metadata.query.common.domain.MetaColumnInfo;
import org.apache.linkis.metadata.query.common.service.AbstractDbMetaService;
import org.apache.linkis.metadata.query.common.service.MetadataConnection;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class EsMetaService extends AbstractDbMetaService<ElasticConnection> {
  @Override
  public MetadataConnection<ElasticConnection> getConnection(
      String operator, Map<String, Object> params) throws Exception {
    String[] endPoints = new String[] {};
    Object urls = params.get(ElasticParamsMapper.PARAM_ES_URLS.getValue());
    if (!(urls instanceof List)) {
      List<String> urlList = Json.fromJson(String.valueOf(urls), List.class, String.class);
      assert urlList != null;
      endPoints = urlList.toArray(endPoints);
    } else {
      endPoints = ((List<String>) urls).toArray(endPoints);
    }
    String password =
        String.valueOf(params.getOrDefault(ElasticParamsMapper.PARAM_ES_PASSWORD.getValue(), ""));
    ElasticConnection conn =
        new ElasticConnection(
            endPoints,
            String.valueOf(
                params.getOrDefault(ElasticParamsMapper.PARAM_ES_USERNAME.getValue(), "")),
            AESUtils.isDecryptByConf(password));
    return new MetadataConnection<>(conn, false);
  }

  @Override
  public List<String> queryDatabases(ElasticConnection connection) {
    // Get indices
    try {
      return connection.getAllIndices();
    } catch (Exception e) {
      throw new RuntimeException("Fail to get ElasticSearch indices(获取索引列表失败)", e);
    }
  }

  @Override
  public List<String> queryTables(ElasticConnection connection, String database) {
    // Get types
    try {
      return connection.getTypes(database);
    } catch (Exception e) {
      throw new RuntimeException("Fail to get ElasticSearch types(获取索引类型失败)", e);
    }
  }

  @Override
  public List<MetaColumnInfo> queryColumns(
      ElasticConnection connection, String database, String table) {
    try {
      Map<Object, Object> props = connection.getProps(database, table);
      List<MetaColumnInfo> columns = new ArrayList<>();
      if (Objects.nonNull(props)) {
        // Flatten nested object fields recursively(递归展开嵌套对象字段)
        flattenFields(props, "", columns);
      }
      return columns;
    } catch (Exception e) {
      throw new RuntimeException("Fail to get ElasticSearch columns(获取索引字段失败)", e);
    }
  }

  /** Separator between levels of a flattened nested field name(展开嵌套字段时层级之间的分隔符) */
  private static final String FIELD_NAME_SEPARATOR = "|";

  /**
   * Recursively flatten ElasticSearch mapping properties into a flat column list.
   *
   * <p>For each entry of the props map:
   *
   * <ul>
   *   <li>If the value contains {@link ElasticConnection#DEFAULT_TYPE_NAME}, stop recursion and add
   *       a column whose name is the parent prefix joined with the key (or just the key at the top
   *       level) and whose type is the value of {@code type}.
   *   <li>Else if the value contains {@link ElasticConnection#FIELD_PROPS} as a map, descend into
   *       it with the key appended to the prefix.
   *   <li>Otherwise stop recursion and add nothing.
   * </ul>
   *
   * (递归展开 ES mappings properties 为扁平字段列表：value 含 type 则停止递归并加字段；含 properties 且为 Map 则用 properties
   * 进入下一层；其他情况停止递归不加字段)
   *
   * @param props properties map at current level
   * @param prefix parent field name prefix, empty at the top level
   * @param columns accumulator for flattened columns
   */
  @SuppressWarnings("unchecked")
  private void flattenFields(
      Map<Object, Object> props, String prefix, List<MetaColumnInfo> columns) {
    for (Map.Entry<Object, Object> entry : props.entrySet()) {
      Object value = entry.getValue();
      if (!(value instanceof Map)) {
        // Not a structured mapping, stop recursion(非结构化 mapping，停止递归)
        continue;
      }
      Map<String, Object> fieldMap = (Map<String, Object>) value;
      String key = String.valueOf(entry.getKey());
      String fieldName = StringUtils.isBlank(prefix) ? key : prefix + FIELD_NAME_SEPARATOR + key;
      if (fieldMap.containsKey(ElasticConnection.DEFAULT_TYPE_NAME)) {
        // Leaf field, stop recursion(叶子字段，停止递归)
        MetaColumnInfo info = new MetaColumnInfo();
        info.setIndex(columns.size());
        info.setName(fieldName);
        info.setType(
            String.valueOf(fieldMap.getOrDefault(ElasticConnection.DEFAULT_TYPE_NAME, "")));
        columns.add(info);
      } else if (fieldMap.containsKey(ElasticConnection.FIELD_PROPS)
          && fieldMap.get(ElasticConnection.FIELD_PROPS) instanceof Map) {
        // Nested object, descend with the properties(嵌套对象，用 properties 进入下一层)
        Map<Object, Object> nestedProps =
            (Map<Object, Object>) fieldMap.get(ElasticConnection.FIELD_PROPS);
        flattenFields(nestedProps, fieldName, columns);
      }
      // Otherwise stop recursion(其他情况停止递归，不加字段)
    }
  }
}
