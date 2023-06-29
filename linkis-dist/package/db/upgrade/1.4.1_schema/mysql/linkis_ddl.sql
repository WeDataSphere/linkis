/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */



ALTER TABLE `linkis_ps_udf_user_load` ADD CONSTRAINT  `uniq_uid_uname` UNIQUE (`udf_id`, `user_name`);

DROP TABLE IF EXISTS `linkis_ps_bml_cleaned_resources_version`;
CREATE TABLE `linkis_ps_bml_cleaned_resources_version` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `resource_id` varchar(50) NOT NULL COMMENT '资源id，资源的uuid',
  `file_md5` varchar(32) NOT NULL COMMENT '文件的md5摘要',
  `version` varchar(20) NOT NULL COMMENT '资源版本（v 加上 五位数字）',
  `size` int(10) NOT NULL COMMENT '文件大小',
  `start_byte` bigint(20) unsigned NOT NULL DEFAULT '0',
  `end_byte` bigint(20) unsigned NOT NULL DEFAULT '0',
  `resource` varchar(2000) NOT NULL COMMENT '资源内容（文件信息 包括 路径和文件名）',
  `description` varchar(2000) DEFAULT NULL COMMENT '描述',
  `start_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
  `end_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '结束时间',
  `client_ip` varchar(200) NOT NULL COMMENT '客户端ip',
  `updator` varchar(50) DEFAULT NULL COMMENT '修改者',
  `enable_flag` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态，1：正常，0：冻结',
  `old_resource` varchar(2000) NOT NULL COMMENT '旧的路径',
  PRIMARY KEY (`id`),
  UNIQUE KEY `resource_id_version` (`resource_id`,`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;