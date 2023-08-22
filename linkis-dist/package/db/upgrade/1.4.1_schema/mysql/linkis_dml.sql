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

update  linkis_ps_configuration_config_key set engine_conn_type = "" where engine_conn_type is NULL;


INSERT INTO linkis_ps_configuration_config_key
(`key`, description, name, default_value, validate_type, validate_range, engine_conn_type, is_hidden, is_advanced, `level`, treeName, boundary_type, en_treeName, en_description, en_name)
VALUES
('spark.conf', 'spark自定义参数配置,多个参数使用分号;分隔', 'spark自定义配置参数', null, 'None', null, 'spark', 0, 1, 1, 'spark资源设置', 0, 'Spark resource settings', 'Spark custom parameter configuration, using semicolons for multiple parameters; separate', 'Spark custom configuration parameters');