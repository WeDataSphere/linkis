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

-- spark.conf
INSERT INTO linkis_ps_configuration_config_key
(`key`, description, name,
default_value, validate_type, validate_range, engine_conn_type,
is_hidden, is_advanced, `level`,
treeName, boundary_type, en_treeName,
en_description, en_name)
VALUES(
'spark.conf', 'spark自定义参数配置,多个参数使用分号;分隔', 'spark自定义配置参数',
null, 'Regex', '([0-9a-zA-Z.]+=[0-9A-Za-z]+)', 'spark',
0, 1, 1,
'spark资源设置', 0, 'Spark resource settings',
'Spark custom parameter configuration, using semicolons for multiple parameters; separate', 'Spark custom configuration parameters');

INSERT INTO `linkis_ps_configuration_key_engine_relation` (`config_key_id`, `engine_type_label_id`)
(
        SELECT config.id AS `config_key_id`, label.id AS `engine_type_label_id`
        FROM (
                select * from linkis_ps_configuration_config_key
                where `key`="spark.conf"
                and `engine_conn_type`="spark") config
   INNER JOIN linkis_cg_manager_label label ON label.label_value ="*-*,spark-2.4.3"
);


INSERT INTO `linkis_ps_configuration_config_value` (`config_key_id`, `config_value`, `config_label_id`)
(
    SELECT `relation`.`config_key_id` AS `config_key_id`, NULL AS `config_value`, `relation`.`engine_type_label_id` AS `config_label_id`
    FROM linkis_ps_configuration_key_engine_relation relation
    INNER JOIN ( select * from linkis_ps_configuration_config_key  where `key`="spark.conf" and `engine_conn_type`="spark") config on relation.config_key_id=config.id
    INNER JOIN ( select * from linkis_cg_manager_label   where label_value ="*-*,spark-2.4.3") label on label.id=relation.engine_type_label_id
);


-- spark.locality.wait

INSERT INTO linkis_ps_configuration_config_key
(`key`, description, name, default_value, validate_type, validate_range, engine_conn_type, is_hidden, is_advanced, `level`, treeName, boundary_type, en_treeName, en_description, en_name)
VALUES
('spark.locality.wait', '范围：0-3000，单位：毫秒', '任务调度本地等待时间', '3000', 'Regex', '^(0|1000|2000|3000)$', 'spark', 0, 1, 1, 'spark资源设置', 0, 'Spark resource settings', 'Range: 0-3000, Unit: millisecond', 'Task scheduling local waiting time');


-- all 默认
INSERT INTO `linkis_ps_configuration_key_engine_relation` (`config_key_id`, `engine_type_label_id`)
(
        SELECT config.id AS `config_key_id`, label.id AS `engine_type_label_id`
        FROM (
                select * from linkis_ps_configuration_config_key
                where `key`="spark.locality.wait"
                and `engine_conn_type`="spark") config
   INNER JOIN linkis_cg_manager_label label ON label.label_value ="*-*,spark-2.4.3"
);



INSERT INTO `linkis_ps_configuration_config_value` (`config_key_id`, `config_value`, `config_label_id`)
(
    SELECT `relation`.`config_key_id` AS `config_key_id`, NULL AS `config_value`, `relation`.`engine_type_label_id` AS `config_label_id`
    FROM linkis_ps_configuration_key_engine_relation relation
    INNER JOIN ( select * from linkis_ps_configuration_config_key  where `key`="spark.locality.wait" and `engine_conn_type`="spark") config on relation.config_key_id=config.id
    INNER JOIN ( select * from linkis_cg_manager_label   where label_value ="*-*,spark-2.4.3") label on label.id=relation.engine_type_label_id
);


-- spark.memory.fraction

INSERT INTO linkis_ps_configuration_config_key
(`key`, description, name, default_value, validate_type, validate_range, engine_conn_type, is_hidden, is_advanced, `level`, treeName, boundary_type, en_treeName, en_description, en_name)
VALUES
('spark.memory.fraction', '范围：0.4,0.5,0.6，单位：百分比', 'JVM堆内存中M的百分比', '0.6', 'Regex', '^(0\\.4|0\\.5|0\\.6)$', 'spark', 0, 1, 1, 'spark资源设置', 0, 'Spark resource settings', 'Range: 0.4, 0.5, 0.6, in percentage', 'The percentage of M in JVM heap memory');

-- all 默认
INSERT INTO `linkis_ps_configuration_key_engine_relation` (`config_key_id`, `engine_type_label_id`)
(
        SELECT config.id AS `config_key_id`, label.id AS `engine_type_label_id`
        FROM (
                select * from linkis_ps_configuration_config_key
                where `key`="spark.memory.fraction"
                and `engine_conn_type`="spark") config
   INNER JOIN linkis_cg_manager_label label ON label.label_value ="*-*,spark-2.4.3"
);

INSERT INTO `linkis_ps_configuration_config_value` (`config_key_id`, `config_value`, `config_label_id`)
(
    SELECT `relation`.`config_key_id` AS `config_key_id`, NULL AS `config_value`, `relation`.`engine_type_label_id` AS `config_label_id`
    FROM linkis_ps_configuration_key_engine_relation relation
    INNER JOIN ( select * from linkis_ps_configuration_config_key  where `key`="spark.memory.fraction" and `engine_conn_type`="spark") config on relation.config_key_id=config.id
    INNER JOIN ( select * from linkis_cg_manager_label   where label_value ="*-*,spark-2.4.3") label on label.id=relation.engine_type_label_id
);


