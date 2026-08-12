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

package org.apache.linkis.metadata.query.common;

import org.apache.linkis.common.conf.CommonVars;

public class MdmConfiguration {

  public static CommonVars<String> METADATA_SERVICE_APPLICATION =
      CommonVars.apply("wds.linkis.server.mdm.service.app.name", "linkis-ps-metadataquery");

  public static CommonVars<String> DATA_SOURCE_SERVICE_APPLICATION =
      CommonVars.apply("wds.linkis.server.dsm.app.name", "linkis-ps-data-source-manager");

  /**
   * Feature switch for Hive datasource sharing. When enabled, non-creator users can query Hive
   * datasource.
   */
  public static CommonVars<Boolean> HIVE_DATASOURCE_SHARE_ENABLE =
      CommonVars.apply("linkis.datasource.hive.share.enable", false);

  /** Local keytab directory path for Hive datasource sharing (used when share is enabled). */
  public static CommonVars<String> HIVE_DATASOURCE_SHARE_KEYTAB_PATH =
      CommonVars.apply("linkis.datasource.hive.share.keytab.path", "/appcom/keytab/");
}
