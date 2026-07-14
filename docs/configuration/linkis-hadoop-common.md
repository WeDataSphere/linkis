## linkis-hadoop-common configure


| Module Name (Service Name) | Parameter Name | Default Value | Description |Used|
| -------- | -------- | ----- |----- |  -----   |
|linkis-hadoop-common|wds.linkis.hadoop.root.user|hadoop-8|hadoop.root.user|true|
|linkis-hadoop-common|wds.linkis.keytab.enable |false |keytab.enable|true|
|linkis-hadoop-common|wds.linkis.keytab.file|/appcom/keytab/|keytab.file|true|
|linkis-hadoop-common|wds.linkis.keytab.host| 127.0.0.1|keytab.host |true|
|linkis-hadoop-common|wds.linkis.keytab.host.enabled|false |keytab.host.enabled|true|
|linkis-hadoop-common|wds.linkis.keytab.host.auto|false |keytab.host.auto: when host.enabled=true, auto-resolve the principal host from the local short hostname (equivalent to shell `hostname`, e.g. `hadoop/${hostname}`); disabled uses the static wds.linkis.keytab.host value |true|
|linkis-hadoop-common|wds.linkis.keytab.proxyuser.enable| false|prometheus.endpoint  |true|
|linkis-hadoop-common|wds.linkis.keytab.proxyuser.superuser|hadoop| proxyuser.superuser |true|
|linkis-hadoop-common|hadoop.config.dir|  |config.dir|true|
|linkis-hadoop-common|wds.linkis.hadoop.external.conf.dir.prefix| /appcom/config/external-conf/hadoop|scan.package  |true|
|linkis-hadoop-common|wds.linkis.hadoop.hdfs.cache.enable|false|hdfs.cache.enable|true|
|linkis-hadoop-common|wds.linkis.hadoop.hdfs.cache.idle.time|3 * 60 * 1000|idle.time|true|
|linkis-hadoop-common|wds.linkis.hadoop.hdfs.cache.max.time|12h| max.time |true|
