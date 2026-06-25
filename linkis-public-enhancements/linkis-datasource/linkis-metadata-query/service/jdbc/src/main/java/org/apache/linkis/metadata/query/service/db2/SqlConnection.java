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

package org.apache.linkis.metadata.query.service.db2;

import org.apache.linkis.common.conf.CommonVars;
import org.apache.linkis.common.utils.AESUtils;
import org.apache.linkis.metadata.query.common.domain.MetaColumnInfo;

import org.apache.logging.log4j.util.Strings;

import java.io.Closeable;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SqlConnection implements Closeable {

  private static final Logger LOG = LoggerFactory.getLogger(SqlConnection.class);

  private static final CommonVars<String> SQL_DRIVER_CLASS =
      CommonVars.apply("wds.linkis.server.mdm.service.db2.driver", "com.ibm.db2.jcc.DB2Driver");

  private static final CommonVars<String> SQL_CONNECT_URL =
      CommonVars.apply("wds.linkis.server.mdm.service.db2.url", "jdbc:db2://%s:%s/%s");

  private static final CommonVars<String> SQL_SCHEMA_QUERY =
      CommonVars.apply(
          "wds.linkis.server.mdm.service.db2.schema.query.sql",
          "SELECT SCHEMANAME FROM SYSCAT.SCHEMATA WHERE SCHEMANAME NOT LIKE 'SYS%' AND SCHEMANAME NOT IN ('NULLID', 'SQLJ') WITH UR");

  private Connection conn;

  private ConnectMessage connectMessage;

  public SqlConnection(
      String host,
      Integer port,
      String username,
      String password,
      String database,
      Map<String, Object> extraParams)
      throws ClassNotFoundException, SQLException {
    if (Strings.isBlank(database)) {
      database = "SAMPLE";
    }
    connectMessage = new ConnectMessage(host, port, username, password, extraParams);
    conn = getDBConnection(connectMessage, database);
    // Try to create statement
    Statement statement = conn.createStatement();
    statement.close();
  }

  public List<String> getAllDatabases() throws SQLException {
    // Query schema list from system catalog view with system schema filtering
    List<String> schemaNames = new ArrayList<>();
    Statement stmt = null;
    ResultSet rs = null;
    try {
      stmt = conn.createStatement();
      // Use configurable SQL to query schemas from SYSCAT.SCHEMATA
      // Default query filters out system schemas (SYS%, NULLID, SQLJ)
      rs = stmt.executeQuery(SQL_SCHEMA_QUERY.getValue());
      while (rs.next()) {
        schemaNames.add(rs.getString(1));
      }
    } finally {
      closeResource(null, stmt, rs);
    }
    return schemaNames;
  }

  public List<String> getAllTables(String tabschema) throws SQLException {
    List<String> tableNames = new ArrayList<>();
    Statement stmt = null;
    ResultSet rs = null;
    try {
      stmt = conn.createStatement();
      rs =
          stmt.executeQuery(
              "select tabname as table_name from syscat.tables where tabschema = '"
                  + tabschema
                  + "' and type = 'T'  order by tabschema, tabname");
      while (rs.next()) {
        tableNames.add(rs.getString(1));
      }
      return tableNames;
    } finally {
      closeResource(null, stmt, rs);
    }
  }

  public List<MetaColumnInfo> getColumns(String schemaname, String table)
      throws SQLException, ClassNotFoundException {
    List<MetaColumnInfo> columns = new ArrayList<>();
    // Quote the table identifier with double quotes so that the original case is preserved.
    // For tables created with a quoted lowercase name, e.g. DB2INST1."db2hive04", the unquoted
    // identifier would be folded to uppercase by DB2 and fail to match.
    // (对表标识符加双引号以保留原始大小写；带引号建的小写表名如 DB2INST1."db2hive04"，
    //  不加引号会被 DB2 折叠为大写导致找不到表)
    String columnSql = "SELECT * FROM " + schemaname + ".\"" + table + "\" WHERE 1 = 2";
    PreparedStatement ps = null;
    ResultSet rs = null;
    ResultSetMetaData meta = null;
    try {
      List<String> primaryKeys = getPrimaryKeys(schemaname, table);
      ps = conn.prepareStatement(columnSql);
      rs = ps.executeQuery();
      meta = rs.getMetaData();
      int columnCount = meta.getColumnCount();
      for (int i = 1; i < columnCount + 1; i++) {
        MetaColumnInfo info = new MetaColumnInfo();
        info.setIndex(i);
        info.setName(meta.getColumnName(i));
        info.setType(meta.getColumnTypeName(i));
        if (primaryKeys.contains(meta.getColumnName(i))) {
          info.setPrimaryKey(true);
        }
        columns.add(info);
      }
    } finally {
      closeResource(null, ps, rs);
    }
    return columns;
  }

  /**
   * Get primary key column names by querying SYSCAT.KEYCOLUSE directly.
   *
   * <p>The JDBC {@link DatabaseMetaData#getPrimaryKeys} is backed by the {@code
   * SYSIBM.SQLPRIMARYKEYS} procedure, which is unreliable for quoted lowercase table names because
   * the table name may be folded to uppercase before matching. Querying {@code SYSCAT.KEYCOLUSE}
   * with the exact case stored in the catalog avoids this issue. (JDBC getPrimaryKeys 底层走
   * SYSIBM.SQLPRIMARYKEYS，对带引号的小写表名不可靠； 直接查 SYSCAT.KEYCOLUSE，按库中存储的真实大小写匹配主键列)
   *
   * @param schemaname schema name
   * @param table table name
   * @return primary key column names
   * @throws SQLException
   */
  private List<String> getPrimaryKeys(String schemaname, String table) throws SQLException {
    List<String> primaryKeys = new ArrayList<>();
    PreparedStatement ps = null;
    ResultSet rs = null;
    try {
      ps =
          conn.prepareStatement(
              "SELECT COLNAME FROM SYSCAT.KEYCOLUSE WHERE TABSCHEMA = ? AND TABNAME = ? ORDER BY COLSEQ");
      ps.setString(1, schemaname);
      ps.setString(2, table);
      rs = ps.executeQuery();
      while (rs.next()) {
        primaryKeys.add(rs.getString("COLNAME"));
      }
      return primaryKeys;
    } finally {
      closeResource(null, ps, rs);
    }
  }

  /**
   * close database resource
   *
   * @param connection connection
   * @param statement statement
   * @param resultSet result set
   */
  private void closeResource(Connection connection, Statement statement, ResultSet resultSet) {
    try {
      if (null != resultSet && !resultSet.isClosed()) {
        resultSet.close();
      }
      if (null != statement && !statement.isClosed()) {
        statement.close();
      }
      if (null != connection && !connection.isClosed()) {
        connection.close();
      }
    } catch (SQLException e) {
      LOG.warn("Fail to release resource [" + e.getMessage() + "]", e);
    }
  }

  @Override
  public void close() throws IOException {
    closeResource(conn, null, null);
  }

  /**
   * @param connectMessage
   * @param database
   * @return
   * @throws ClassNotFoundException
   */
  private Connection getDBConnection(ConnectMessage connectMessage, String database)
      throws ClassNotFoundException, SQLException {
    String extraParamString =
        connectMessage.extraParams.entrySet().stream()
            .map(e -> String.join("=", e.getKey(), String.valueOf(e.getValue())))
            .collect(Collectors.joining("&"));
    Class.forName(SQL_DRIVER_CLASS.getValue());
    String url =
        String.format(
            SQL_CONNECT_URL.getValue(), connectMessage.host, connectMessage.port, database);
    if (!connectMessage.extraParams.isEmpty()) {
      url += "?" + extraParamString;
    }
    return DriverManager.getConnection(
        url, connectMessage.username, AESUtils.isDecryptByConf(connectMessage.password));
  }

  /** Connect message */
  private static class ConnectMessage {
    private String host;

    private Integer port;

    private String username;

    private String password;

    private Map<String, Object> extraParams;

    public ConnectMessage(
        String host,
        Integer port,
        String username,
        String password,
        Map<String, Object> extraParams) {
      this.host = host;
      this.port = port;
      this.username = username;
      this.password = password;
      this.extraParams = extraParams;
    }
  }
}
