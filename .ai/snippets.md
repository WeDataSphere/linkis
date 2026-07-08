# Linkis AI 代码片段库（ai-snippets）

> 本文件收录项目中高频复用的代码片段，所有片段均**取自项目真实代码**，AI 协助开发时可直接复用。
> 主指引文档见：[`../CLAUDE.md`](../CLAUDE.md)
>
> 每个片段包含：① 适用场景 ② 可复制代码块 ③ 项目内真实参考文件路径。

---

## 目录

1. [配置开关声明（CommonVars）](#1-配置开关声明commonvars)
2. [异常降级（Utils.tryCatch 三种变体）](#2-异常降级utilstrycatch-三种变体)
3. [日志（LogUtils 推前端 + LoggerUtils MDC）](#3-日志logutils-推前端--loggerutils-mdc)
4. [任务参数（TaskUtils 三套 Map + EngineCreationContext）](#4-任务参数taskutils-三套-map--enginecreationcontext)
5. [Label 读写（LabelUtil + LabelBuilderFactory）](#5-label-读写labelutil--labelbuilderfactory)
6. [RPC（Sender 客户端 + Receiver/ReceiverChooser 服务端）](#6-rpcsender-客户端--receiverreceiverchooser-服务端)
7. [EntranceInterceptor 完整模板](#7-entranceinterceptor-完整模板)
8. [EngineConnPlugin 完整模板](#8-engineconnplugin-完整模板)
9. [Java Restful Controller（Message + 鉴权）](#9-java-restful-controllermessage--鉴权)
10. [Java Service + MyBatis（@Transactional + Mapper）](#10-java-service--mybatistransactional--mapper)
11. [用户角色与权限判断（Configuration.isAdmin / ModuleUserUtils）](#11-用户角色与权限判断统一入口禁止自己读配置)
12. [任务关键信息获取（JobId / Label 强类型读取）](#12-任务关键信息获取jobid--label-强类型读取)

---

## 1. 配置开关声明（CommonVars）

**适用场景**：声明可配置项（功能开关、阈值、URL 等），禁止散落硬编码。

```scala
import org.apache.linkis.common.conf.CommonVars

object FooConfiguration {
  // Boolean 开关（推荐写法，类型一目了然）
  val FOO_BAR_SWITCH = CommonVars("linkis.foo.bar.switch", false)

  // 字符串配置
  val AI_SQL_KEY: CommonVars[String] =
    CommonVars[String]("linkis.ai.sql.enable", "true")

  // 带 description（复杂场景，便于运维理解）
  val FOO_BAR_SWITCH =
    CommonVars("linkis.foo.bar.switch", false,
      "Enable foo bar feature, fallback to legacy path on exception")
}
```

```java
// Java 配置类写法（参考 EnvConfiguration）
public class EngineConnConf {
    public static final CommonVars<Boolean> FOO_ENABLED =
        CommonVars.apply("linkis.foo.enabled", Boolean.FALSE);
}
```

**热加载边界**：

| 方法 | 行为 | 适用场景 |
| --- | --- | --- |
| `key.getValue` | 首次读后缓存到 JVM，**重启才更新** | 90% 场景 |
| `key.getHotValue` / `key.acquireNew` | 每次读"热加载缓存"（后台线程定时刷新） | 运维需要不重启动态调参时 |
| `key.getValue(options)` | 从任务级 map 读 | EngineConn 内部读取任务级配置 |

> 若新增功能开关期望"出问题立刻关掉，不重启"，**必须用 `getHotValue`**。

**参考**：`linkis-computation-governance/linkis-entrance/src/main/scala/org/apache/linkis/entrance/conf/EntranceConfiguration.scala`、`linkis-computation-governance/linkis-engineconn/linkis-engineconn-plugin-core/src/main/scala/org/apache/linkis/manager/engineplugin/common/conf/EnvConfiguration.scala`

---

## 2. 异常降级（Utils.tryCatch 三种变体）

**适用场景**：所有新功能主路径必须包裹异常保护，确保未预料异常能让代码自动回退到旧行为。

### 2.1 显式降级（推荐，最常用）

```scala
import org.apache.linkis.common.utils.Utils

var currentEngineType = legacyEngineType
Utils.tryCatch {
  val dataSource = EntranceUtils.getDatasourceByDatasourceTypeAndUser(...)
  if (dataSource != null) {
    changeEngineLabel(newEngineType, labels)
    currentEngineType = newEngineType
    logAppender.append(LogUtils.generateInfo(s"use $newEngineType engine"))
  } else {
    changeEngineLabel(legacyEngineType, labels)
    currentEngineType = legacyEngineType
  }
} { t =>
  // 任何异常都退回旧行为，不让新功能炸掉任务
  changeEngineLabel(legacyEngineType, labels)
  currentEngineType = legacyEngineType
  logger.warn("Failed to select new engine, fallback to legacy: ", t)
  logAppender.append(LogUtils.generateInfo(
    s"Failed to select new engine, service exception. now use $currentEngineType"))
}
```

### 2.2 仅记录日志，返回 null

```scala
val result = Utils.tryAndWarn {
  riskyOperation()
}
// result 为 null 表示失败，调用方需做 null 检查
```

### 2.3 包装后重新抛出（不可降级时用）

```scala
Utils.tryThrow(doSomething()) { t =>
  throw new EngineConnErrorException(50001, "engine start failed: " + t.getMessage, t)
}
```

### 2.4 工具方法对照表

| 方法 | 失败时行为 |
| --- | --- |
| `Utils.tryCatch(tryOp)(catchOp)` | 由 `catchOp` 决定 |
| `Utils.tryAndWarn(tryOp)` | 记录 warn 日志，返回 null |
| `Utils.tryAndWarnMsg(tryOp)(msg)` | 同上 + 自定义消息 |
| `Utils.tryAndError(tryOp)` / `Utils.tryAndErrorMsg(tryOp)(msg)` | 记录 error 日志，返回 null |
| `Utils.tryQuietly(tryOp)` | 静默吞掉，返回 null |
| `Utils.tryThrow(tryOp)(f)` | 包装后重新抛出 |

**Java 等价写法**：普通 `try/catch`，但 catch 块必须用项目 logger（不要 `e.printStackTrace()`），且走老路径或返回安全默认值。

**参考**：`linkis-computation-governance/linkis-entrance/src/main/scala/org/apache/linkis/entrance/interceptor/impl/AISQLTransformInterceptor.scala`、`linkis-commons/linkis-common/src/main/scala/org/apache/linkis/common/utils/Utils.scala`

---

## 3. 日志（LogUtils 推前端 + LoggerUtils MDC）

### 3.1 推前端的日志（必须用 LogUtils）

```scala
import org.apache.linkis.common.log.LogUtils

logAppender.append(LogUtils.generateInfo(s"use starrocks engine ..."))
logAppender.append(LogUtils.generateWarn(s"User $user is not in whitelist"))
logAppender.append(LogUtils.generateError(s"engine start failed: ${e.getMessage}"))
```

> **不要直接拼字符串推前端**——`LogUtils` 会包装成标准格式，前端按类型显示。

### 3.2 普通业务日志（继承 Logging trait）

```scala
import org.apache.linkis.common.utils.Logging

class FooService extends Logging {
  def doSomething(): Unit = {
    logger.info("start doSomething")           // 简单信息
    logger.warn("deprecated usage", e)          // 带异常
    logger.error(s"failed: ${e.getMessage}", e) // 错误码用双语
  }
}

// 或者 object 单例
object FooUtils extends Logging { ... }
```

历史代码也常见：`private val logger: Logger = LoggerFactory.getLogger(getClass)`

### 3.3 任务上下文 MDC（链路追踪）

```scala
import org.apache.linkis.common.utils.LoggerUtils

// 任务入口处设置 jobId 到 MDC
LoggerUtils.setJobIdMDC(jobRequest.getId.toString)
try {
  // 处理任务
} finally {
  LoggerUtils.removeJobIdMDC()
}
```

> 所有日志在 MDC 范围内都会带上 jobId，便于线上按任务排查。

### 3.4 业务错误日志格式（项目惯例）

```scala
// ErrorException / WarnException 的标准日志格式
logger.error(
  s"error code（错误码）: ${error.getErrCode}, Error message（错误信息）: ${error.getDesc}.", error)
```

**参考**：`linkis-computation-governance/linkis-entrance/src/main/scala/org/apache/linkis/entrance/EntranceServer.scala`（MDC）、`AISQLTransformInterceptor.scala`（LogUtils）

---

## 4. 任务参数（TaskUtils 三套 Map + EngineCreationContext）

### 4.1 JobRequest 的 params 三套子 Map

```scala
import org.apache.linkis.protocol.utils.TaskUtils
import scala.collection.JavaConverters._

val params = jobRequest.getParams  // java.util.Map[String, AnyRef]

// 读 startup（启动参数：内存、引擎版本、ec.resource.name、AI SQL 开关等）
val startupMap: util.Map[String, AnyRef] = TaskUtils.getStartupMap(params)

// 读 runtime（运行时参数：数据源、超时、并发数）
val runtimeMap: util.Map[String, AnyRef] = TaskUtils.getRuntimeMap(params)

// 读 special（特殊参数，少用）
val specialMap = TaskUtils.getSpecialMap(params)

// 读 variable（代码 ${var} 占位符）
val variableMap = TaskUtils.getVariableMap(params)

// 写
val kv = Map("linkis.foo.bar" -> "value").asJava
TaskUtils.addStartupMap(params, kv)
TaskUtils.addRuntimeMap(params, kv)
```

> **禁止直接 `params.get("startup")`**，必须走 TaskUtils。

### 4.2 EngineCreationContext 内部读取任务级配置

```scala
// 在 EngineConn 进程内部（EngineConnExecutor / Hook 等）
val props: util.Map[String, String] = engineCreationContext.getOptions

// 方式 A：直接读，带默认值
val taskRetry: String = props.getOrDefault("linkis.task.retry.switch", "false")

// 方式 B：通过 CommonVars 读（推荐，类型安全）
val enabled = INIT_SQL_ENABLE.getValue(engineCreationContext.getOptions)
```

**参考**：`linkis-commons/linkis-protocol/src/main/scala/org/apache/linkis/protocol/utils/TaskUtils.scala`、`linkis-computation-governance/linkis-engineconn/linkis-computation-engineconn/src/main/scala/org/apache/linkis/engineconn/computation/executor/hook/InitSQLHook.scala`

---

## 5. Label 读写（LabelUtil + LabelBuilderFactory）

### 5.1 读 Label（用工具类，不要自己 new + 强转）

```scala
import org.apache.linkis.manager.label.utils.LabelUtil
import scala.collection.JavaConverters._

val labels = jobRequest.getLabels  // java.util.List[Label[_]]

val codeType: String = LabelUtil.getCodeType(labels)             // 如 "sql"、"aisql"
val (user, creator) = LabelUtil.getUserCreator(labels)           // (submitUser, creator)
val engineType: String = LabelUtil.getEngineType(labels)         // 如 "spark"、"jdbc"

// 找特定 Label
val userCreatorOpt = labels.asScala.find(_.isInstanceOf[UserCreatorLabel])
val engineTypeLabel = labels.asScala
  .find(_.isInstanceOf[EngineTypeLabel])
  .map(_.asInstanceOf[EngineTypeLabel])
  .orNull
```

### 5.2 创建 / 修改 Label（用 LabelBuilderFactory）

```scala
import org.apache.linkis.manager.label.builder.factory.LabelBuilderFactoryContext

// 创建新 Label
val newLabel: EngineTypeLabel =
  LabelBuilderFactoryContext.getLabelBuilderFactory.createLabel(classOf[EngineTypeLabel])
newLabel.setEngineType("spark")
newLabel.setVersion("3.4.4")

// 修改现有 Label：先 remove 旧的，再加新的（参考 changeEngineLabel）
def changeEngineLabel(engineTypeStr: String, labels: util.List[Label[_]]): Unit = {
  val it = labels.iterator()
  while (it.hasNext) {
    if (it.next().isInstanceOf[EngineTypeLabel]) it.remove()
  }
  val newLabel = LabelBuilderFactoryContext.getLabelBuilderFactory
    .createLabel(classOf[EngineTypeLabel])
  newLabel.setEngineType(engineTypeStr.split("-")(0))
  newLabel.setVersion(engineTypeStr.split("-")(1))
  labels.add(newLabel)
}
```

### 5.3 常见 Label 类

| Label 类 | 含义 | 关键字段 |
| --- | --- | --- |
| `UserCreatorLabel` | 提交用户 + creator | `user`, `creator` |
| `EngineTypeLabel` | 引擎类型 + 版本 | `engineType`, `version` |
| `CodeLanguageLabel` | 运行语言 | `runType` |
| `TenantLabel` | 租户 | — |
| 其余 | `EngineNodeLabel`、`RetryLabel`、`ResourceLabel`、`EnvLabel`… | `linkis-label-common/.../entity` |

**参考**：`linkis-computation-governance/linkis-entrance/src/main/scala/org/apache/linkis/entrance/interceptor/impl/AISQLTransformInterceptor.scala`（changeEngineLabel 方法）

---

## 6. RPC（Sender 客户端 + Receiver/ReceiverChooser 服务端）

### 6.1 客户端：调用其他服务

```scala
import org.apache.linkis.rpc.Sender
import org.apache.linkis.common.conf.Configuration

// 同步调用（最常见）
val sender = Sender.getSender(Configuration.JOBHISTORY_SPRING_APPLICATION_NAME.getValue)
val response = sender.ask(request) match {
  case resp: XxxResponse => resp
  case other =>
    logger.error(s"unexpected response: $other")
    null
}

// 异步调用（fire and forget）
sender.call(request)

// 拿到本服务的 instance（用于 callback）
val thisInstance = Sender.getThisServiceInstance
```

### 6.2 服务端：实现 Receiver + ReceiverChooser（必须成对）

**协议类（必须放 `linkis-protocol` 模块）**：

```scala
// linkis-commons/linkis-protocol/src/main/scala/.../protocol/FooProtocol.scala
case class FooRequest(user: String, code: String) extends RequestProtocol
case class FooResponse(success: Boolean, data: Any)
```

**Receiver（处理请求）**：

```scala
// <service>/src/main/scala/.../receiver/FooReceiver.scala
import org.apache.linkis.rpc.{Receiver, Sender}
import scala.concurrent.duration.Duration

class FooReceiver extends Receiver {
  private var fooService: FooService = _

  def this(fooService: FooService) = {
    this()
    this.fooService = fooService
  }

  override def receive(message: Any, sender: Sender): Unit = {}

  override def receiveAndReply(message: Any, sender: Sender): Any = message match {
    case FooRequest(user, code) => fooService.handle(user, code)
    case _ => null
  }

  override def receiveAndReply(message: Any, duration: Duration, sender: Sender): Any = {}
}
```

**ReceiverChooser（路由请求到 Receiver）**：

```scala
// <service>/src/main/scala/.../receiver/FooReceiverChooser.scala
import org.apache.linkis.rpc.{ReceiverChooser, RPCMessageEvent}
import org.springframework.beans.factory.annotation.Autowired
import javax.annotation.PostConstruct

class FooReceiverChooser extends ReceiverChooser {

  @Autowired
  private var fooService: FooService = _

  private var receiver: Option[Receiver] = _

  @PostConstruct
  def init(): Unit = receiver = Some(new FooReceiver(fooService))

  override def chooseReceiver(event: RPCMessageEvent): Option[Receiver] = event.message match {
    case _: FooProtocol => receiver       // 注意：只 match 协议 trait，不 match 具体 case
    case _ => None
  }
}
```

> **协议类必须定义在 `linkis-commons/linkis-protocol/`**，跨模块共享，否则编译不过。
> 大于 1MB 的请求体不要走 RPC，改走 BML 上传 + 传 resourceId。

**参考**：`linkis-public-enhancements/linkis-configuration/src/main/scala/org/apache/linkis/configuration/receiver/ConfigurationReceiver.scala` + `ConfigurationReceiverChooser.scala`、`linkis-computation-governance/linkis-engineconn-manager/linkis-engineconn-manager-server/src/main/scala/org/apache/linkis/ecm/server/service/impl/DefaultECMRegisterService.scala`（Sender 调用）

---

## 7. EntranceInterceptor 完整模板

**适用场景**：在 Entrance 接收 JobRequest 后、编排前注入业务逻辑（参数修改、Label 路由、安全检查等）。

```scala
// linkis-computation-governance/linkis-entrance/src/main/scala/org/apache/linkis/entrance/interceptor/impl/FooInterceptor.scala
package org.apache.linkis.entrance.interceptor.impl

import org.apache.linkis.common.log.LogUtils
import org.apache.linkis.common.utils.{Logging, Utils}
import org.apache.linkis.entrance.conf.EntranceConfiguration
import org.apache.linkis.entrance.interceptor.EntranceInterceptor
import org.apache.linkis.governance.common.entity.job.JobRequest

class FooInterceptor extends EntranceInterceptor with Logging {

  override def apply(
      jobRequest: JobRequest,
      logAppender: java.lang.StringBuilder
  ): JobRequest = {

    // 1. 先读开关
    if (!EntranceConfiguration.FOO_BAR_SWITCH.getValue) {
      return jobRequest   // 开关关闭，直接走老路径
    }

    // 2. 主逻辑用 tryCatch 兜底
    Utils.tryCatch {
      // 新功能逻辑...
      logAppender.append(LogUtils.generateInfo(s"foo feature applied for ${jobRequest.getId}"))
      jobRequest
    } { t =>
      // 3. 降级：不修改 jobRequest，让它走老路径
      logger.warn(s"Failed to apply foo feature for ${jobRequest.getId}, fallback: ", t)
      logAppender.append(
        LogUtils.generateWarn(s"foo feature disabled due to exception"))
      jobRequest
    }
  }
}
```

**注册到拦截器链**（顺序敏感！）：

```java
// linkis-entrance/src/main/java/org/apache/linkis/entrance/conf/EntranceSpringConfiguration.java
@Bean
@ConditionalOnMissingBean(name = {ServiceNameConsts.ENTRANCE_INTERCEPTOR})
public EntranceInterceptor[] entranceInterceptors() {
  return new EntranceInterceptor[] {
    new OnceJobInterceptor(),
    // ... 既有拦截器
    new AISQLTransformInterceptor(),
    // new FooInterceptor(),   // ← 加在这里（位置谨慎选择）
    // ... 既有拦截器
    new QueueSelectionInterceptor()
  };
}
```

**参考**：`linkis-computation-governance/linkis-entrance/src/main/scala/org/apache/linkis/entrance/interceptor/impl/AISQLTransformInterceptor.scala`、`LogPathCreateInterceptor.scala`、`linkis-computation-governance/linkis-entrance/src/main/java/org/apache/linkis/entrance/conf/EntranceSpringConfiguration.java`

---

## 8. EngineConnPlugin 完整模板

**适用场景**：接入新计算引擎（如 trino/doris），实现插件 4 件套。

### 8.1 插件入口（单例懒加载）

```scala
// linkis-engineconn-plugins/<engine>/src/main/scala/.../<Engine>EngineConnPlugin.scala
class JDBCEngineConnPlugin extends EngineConnPlugin {

  private val resourceLocker = new Object()
  private val engineLaunchBuilderLocker = new Object()
  private val engineFactoryLocker = new Object()

  private var engineResourceFactory: EngineResourceFactory = _
  private var engineLaunchBuilder: EngineConnLaunchBuilder = _
  private var engineFactory: EngineConnFactory = _

  private val defaultLabels: util.List[Label[_]] = new util.ArrayList[Label[_]]()

  override def init(params: util.Map[String, AnyRef]): Unit = {
    // 可在此设置默认 Label（一般留空）
  }

  override def getEngineResourceFactory: EngineResourceFactory = {
    if (null == engineResourceFactory) resourceLocker synchronized {
      engineResourceFactory = new GenericEngineResourceFactory
    }
    engineResourceFactory
  }

  override def getEngineConnLaunchBuilder: EngineConnLaunchBuilder =
    new JDBCProcessEngineConnLaunchBuilder

  override def getEngineConnFactory: EngineConnFactory = {
    if (null == engineFactory) engineFactoryLocker synchronized {
      engineFactory = new JDBCEngineConnFactory
    }
    engineFactory
  }

  override def getDefaultLabels: util.List[Label[_]] = this.defaultLabels
}
```

### 8.2 注册插件（关键！漏了引擎起不来）

```properties
# src/main/resources/linkis-engineconn.properties
wds.linkis.engineconn.plugin.default.class=org.apache.linkis.manager.engineplugin.jdbc.JDBCEngineConnPlugin
```

### 8.3 4 件套职责

| 类 | 职责 |
| --- | --- |
| `<Engine>EngineConnPlugin` | 插件入口，懒加载下面 3 个组件 |
| `<Engine>EngineConnFactory` | 创建 EngineConn 对象、初始化引擎连接 |
| `<Engine>EngineConnExecutor extends ConcurrentComputationExecutor` | 执行代码、回写日志/结果集 |
| `<Engine>ProcessEngineConnLaunchBuilder` | 构建启动命令行参数 |

**参考**：`linkis-engineconn-plugins/jdbc/src/main/scala/org/apache/linkis/manager/engineplugin/jdbc/JDBCEngineConnPlugin.scala` + 同目录 `factory/JDBCEngineConnFactory.scala` + `executor/JDBCEngineConnExecutor.scala` + `builder/JDBCProcessEngineConnLaunchBuilder.scala`

---

## 9. Java Restful Controller（Message + 鉴权）

**适用场景**：给前端/客户端暴露 RESTful 接口。

```java
// <service>/src/main/java/.../restful/FooRestfulApi.java
package org.apache.linkis.foo.restful;

import org.apache.linkis.server.Message;
import org.apache.linkis.server.security.SecurityFilter;
import org.apache.linkis.server.utils.ModuleUserUtils;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@Api(tags = "foo restful")
@RestController
@RequestMapping(path = "/foo")
public class FooRestfulApi {

  @Autowired private FooService fooService;

  // GET 接口
  @ApiOperation(value = "getFoo", notes = "get foo by id", response = Message.class)
  @RequestMapping(path = "/get", method = RequestMethod.GET)
  public Message getFoo(
      HttpServletRequest req,
      @RequestParam("id") Long id) {
    String username = ModuleUserUtils.getOperationUser(req, "getFoo");
    FooVo foo = fooService.getById(id, username);
    return Message.ok().data("foo", foo);
  }

  // POST 接口（写操作）
  @ApiOperation(value = "saveFoo", notes = "save foo", response = Message.class)
  @RequestMapping(path = "/save", method = RequestMethod.POST)
  public Message saveFoo(HttpServletRequest req, @RequestBody FooSaveRequest request) {
    String username = ModuleUserUtils.getOperationUser(req, "saveFoo");
    fooService.save(request, username);
    return Message.ok();
  }
}
```

**约定**：
- 路径前缀：公共走 `/api/rest_j/v1/<module>/<action>`，私有管理走 `/api/rest_s/v1/...`；
- 取当前用户：`ModuleUserUtils.getOperationUser(req, "operationDesc")`，**不要用 `request.getUserPrincipal()`**；
- 返回值统一 `Message.ok()` / `Message.error()`，不要直接返回业务对象；
- 管理员接口加 `checkAdmin(username)`；
- 免鉴权路径需在 `linkis.properties` 的 `wds.linkis.server.user.restful.uri.pass.auth` 声明。

**参考**：`linkis-public-enhancements/linkis-pes-publicservice/src/main/java/org/apache/linkis/errorcode/server/restful/LinkisErrorCodeRestful.java`、`linkis-computation-governance/linkis-manager/linkis-application-manager/src/main/java/org/apache/linkis/manager/am/restful/EMRestfulApi.java`

---

## 10. Java Service + MyBatis（@Transactional + Mapper）

**适用场景**：业务逻辑层 + 持久化。

```java
// Service 接口
public interface FooService {
    Foo getById(Long id, String username);
    Boolean save(FooSaveRequest request, String username);
}

// Service 实现
@Service
public class FooServiceImpl implements FooService {
  private static final Logger LOG = LoggerFactory.getLogger(FooService.class);

  @Resource private FooMapper fooMapper;

  @Override
  public Foo getById(Long id, String username) {
    return fooMapper.selectById(id);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Boolean save(FooSaveRequest request, String username) {
    Foo foo = new Foo();
    BeanUtils.copyProperties(request, foo);
    foo.setCreateUser(username);
    int inserted = fooMapper.insert(foo);
    LOG.info("inserted foo, id={}", foo.getId());
    return inserted > 0;
  }
}
```

**Mapper 接口 + XML**：

```java
// src/main/java/.../mapper/FooMapper.java
public interface FooMapper {
    Foo selectById(@Param("id") Long id);
    int insert(Foo foo);
}
```

```xml
<!-- src/main/resources/mapper/common/FooMapper.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="org.apache.linkis.foo.mapper.FooMapper">

  <sql id="columns">id, name, create_user, create_time</sql>

  <select id="selectById" resultType="org.apache.linkis.foo.entity.Foo">
    SELECT <include refid="columns"/>
    FROM linkis_foo
    WHERE id = #{id}
  </select>

  <insert id="insert" parameterType="org.apache.linkis.foo.entity.Foo" useGeneratedKeys="true" keyProperty="id">
    INSERT INTO linkis_foo (name, create_user, create_time)
    VALUES (#{name}, #{createUser}, NOW())
  </insert>

</mapper>
```

**约定**：
- 写操作必须 `@Transactional(rollbackFor = Exception.class)`；
- Mapper XML 放 `src/main/resources/mapper/common/`，namespace 与 Mapper 接口对齐；
- DDL 放 `linkis-dist/package/db/linkis_<module>.sql`（升级脚本放 `linkis-dist/package/db/upgrade/<version>/`）；
- 列表查询必须分页 + 加索引（参考近期 commit `[fix] 修复linkismanager 慢sql`）。

**参考**：`linkis-public-enhancements/linkis-pes-publicservice/src/main/java/org/apache/linkis/basedatamanager/server/service/impl/ConfigurationTemplateServiceImpl.java`、`linkis-public-enhancements/linkis-pes-publicservice/src/main/resources/mapper/common/`

---

## 附：常用工具类位置速查

| 工具类 | 路径 |
| --- | --- |
| `Utils`（tryCatch / classForName / 线程池） | `linkis-commons/linkis-common/src/main/scala/org/apache/linkis/common/utils/Utils.scala` |
| `Logging` trait | 同上目录 `Logging.scala` |
| `CommonVars` / `Configuration` / `BDPConfiguration` | `linkis-commons/linkis-common/src/main/scala/org/apache/linkis/common/conf/` |
| `TaskUtils`（参数三套 Map） | `linkis-commons/linkis-protocol/src/main/scala/org/apache/linkis/protocol/utils/TaskUtils.scala` |
| `LogUtils` / `LoggerUtils` | 同上 `common/utils/` 目录 |
| `LabelUtil` | `linkis-computation-governance/linkis-manager/linkis-label-common/src/main/java/.../utils/LabelUtil.java` |
| `LabelBuilderFactoryContext` | 同 `linkis-label-common/.../builder/factory/` |
| `Sender` / `Receiver` / `ReceiverChooser` | `linkis-commons/linkis-rpc/src/main/scala/org/apache/linkis/rpc/` |
| `Message` / `SecurityFilter` / `ModuleUserUtils` | `linkis-commons/linkis-module/src/main/java/.../server/` |
| `EntranceInterceptor` | `linkis-computation-governance/linkis-entrance/src/main/scala/.../interceptor/EntranceInterceptor.scala` |
| `EngineConnPlugin` | `linkis-computation-governance/linkis-engineconn/linkis-engineconn-plugin-core/src/main/scala/.../manager/engineplugin/common/` |

---

> 如发现新的高频复用模式，请补充到本文末尾并在 [`CLAUDE.md`](../CLAUDE.md) 第七章（常用查找位置）加引用。

---

## 11. 用户角色与权限判断（统一入口，禁止自己读配置）

### 11.1 判断当前用户是否是管理员

**位置**：`linkis-commons/linkis-common/src/main/scala/org/apache/linkis/common/conf/Configuration.scala`

```scala
import org.apache.linkis.common.conf.Configuration

// 超级管理员（GOVERNANCE_STATION_ADMIN，热加载）
Configuration.isAdmin(username)            // => Boolean
Configuration.isNotAdmin(username)         // => Boolean

// jobhistory 维度管理员（含超级管理员 + JOB_HISTORY_ADMIN，热加载）
Configuration.isJobHistoryAdmin(username)
Configuration.getJobHistoryAdmin()         // => Array[String]

// 部门维度管理员（JOB_HISTORY_DEPARTMENT_ADMIN，热加载）
Configuration.isDepartmentAdmin(username)

// Token 维度管理员（Token-Code header 判断）
Configuration.isAdminToken(token)          // => Boolean
```

**关键点**：
- `isAdmin` / `isJobHistoryAdmin` / `isDepartmentAdmin` **全部用 `getHotValue`**，运维改 `wds.linkis.governance.station.admin` 后无需重启即可生效；
- 管理员列表均以逗号分隔，且**大小写不敏感**（内部 `equalsIgnoreCase`）；
- **禁止自己 `split(",")` 配置 key**——一定走这几个统一入口，否则热加载和大小写处理不一致。

### 11.2 在 Restful 接口里取"当前操作用户"

**位置**：`linkis-commons/linkis-module/src/main/java/org/apache/linkis/server/utils/ModuleUserUtils.java`

```java
import org.apache.linkis.server.utils.ModuleUserUtils;

// 取当前操作用户（已考虑代理模式：若启用代理则返回 proxyUser，否则返回 loginUser）
String opUser = ModuleUserUtils.getOperationUser(httpServletRequest);

// 带审计日志版本（自动打 info 日志：user X proxy to Y operation Z）
String opUser = ModuleUserUtils.getOperationUser(httpServletRequest, "delete datasource " + dsId);

// 同时需要 loginUser 和 proxyUser 的场景
ProxyUserEntity proxyUserEntity = ModuleUserUtils.getProxyUserEntity(req);
proxyUserEntity.getUsername();              // 登录用户
proxyUserEntity.getProxyUser();             // 代理用户（可能为 null）
proxyUserEntity.isProxyMode();              // 是否启用代理
```

**反模式**（禁止使用）：
- `request.getUserPrincipal().getName()` —— Linkis 用 `SecurityFilter` 自管身份，不走 Servlet 容器；
- 直接读 header `Token-User` —— 应通过 `ModuleUserUtils.getTokenUser(req)`，自带日志。

### 11.3 标准用法：取用户 + 判管理员

```java
@RestController
@RequestMapping("/api/rest_j/v1/foo")
public class FooRestfulApi {

  @Autowired
  private FooService fooService;

  @GetMapping("/bar")
  public Message bar(@RequestParam String targetUser, HttpServletRequest req) {
    String opUser = ModuleUserUtils.getOperationUser(req, "bar");
    // 跨用户操作时校验：要么是本人，要么是管理员
    if (!targetUser.equals(opUser) && Configuration.isNotAdmin(opUser)) {
      return Message.error("Permission denied (无权限操作其他用户资源)");
    }
    return Message.ok().data("result", fooService.bar(targetUser));
  }
}
```

---

## 12. 任务关键信息获取（JobId / Label 强类型读取）

### 12.1 从任意 Map 取 jobId（跨服务追踪任务用）

**位置**：`linkis-computation-governance/linkis-computation-governance-common/src/main/scala/org/apache/linkis/governance/common/utils/JobUtils.scala`

```scala
import org.apache.linkis.governance.common.utils.JobUtils
import org.apache.commons.linkis.common.constant.job.JobRequestConstants

// 从 Map[String, AnyRef] 读
val jobId: String = JobUtils.getJobIdFromMap(params)

// 从 Map[String, String] 读（更常见，比如 task params 序列化为 String-String）
val jobId: String = JobUtils.getJobIdFromStringMap(stringParams)

// 从 task 源标签里读（用于追溯任务来源链）
val tags: String = JobUtils.getJobSourceTagsFromStringMap(stringParams)
```

**用途**：
- EC 内部记录 metric / 日志时，把 `jobId` 一并打出来，方便回溯；
- 跨服务 RPC（如 AM ↔ ECM ↔ Entrance）传递任务上下文时统一用 `JobUtils.getJobIdFromStringMap` 而不是 `params.get("jobId")`——避免 key 写错（key 实际是常量 `JobRequestConstants.JOB_ID`）。

### 12.2 任务 Label 强类型读取

**位置**：`linkis-computation-governance/linkis-manager/linkis-label-common/src/main/scala/org/apache/linkis/manager/label/utils/LabelUtil.scala`

```scala
import org.apache.linkis.manager.label.utils.LabelUtil
import org.apache.linkis.manager.label.entity.engine.{EngineTypeLabel, UserCreatorLabel, CodeLanguageLabel}
import org.apache.linkis.manager.label.entity.TenantLabel

// 一次性拿到的常用元组（user, creator）
val (user, creator): (String, String) = LabelUtil.getUserCreator(jobRequest.getLabels)

// 单值字段
val engineType: String = LabelUtil.getEngineType(jobRequest.getLabels)
val codeType:   String = LabelUtil.getCodeType(jobRequest.getLabels)   // sql/python/scala/aisql...
val tenant:     String = LabelUtil.getTenantValue(jobRequest.getLabels) // 空字符串而非 null

// 需要拿整个 Label 对象时（推荐，便于取 version 等字段）
val etLabel:   EngineTypeLabel   = LabelUtil.getEngineTypeLabel(labels)
val ucLabel:   UserCreatorLabel  = LabelUtil.getUserCreatorLabel(labels)
val codeLabel: CodeLanguageLabel = LabelUtil.getCodeTypeLabel(labels)
val tenantLabel: TenantLabel     = LabelUtil.getTenantLabel(labels)

// 判断 YarnCluster 模式
val isCluster: Boolean = LabelUtil.isYarnClusterMode(labels)

// 判断目标引擎（含版本）
val isSpark3: Boolean = LabelUtil.isTargetEngine(labels, "spark", "3.4")
```

**反模式**（禁止使用）：
- 自己 `labels.stream().filter(_.isInstanceOf[...]).findFirst()` —— 直接用 `LabelUtil.getLabelFromList[A]`；
- `labels.forEach { case e: EngineTypeLabel => ... }` —— 当存在多个 EngineTypeLabel 时取错；
- 直接 `params.get("engineType")` —— label 序列化成字符串时 key/格式有规则，必须走 Label API（见 §5）。

### 12.3 标准用法：拦截器里读 Label 决策

```scala
override def apply(jobRequest: JobRequest, logAppender: StringBuilder): JobRequest = {
  val (user, creator) = LabelUtil.getUserCreator(jobRequest.getLabels)
  val engineType = LabelUtil.getEngineType(jobRequest.getLabels)
  val codeType = LabelUtil.getCodeType(jobRequest.getLabels)
  logger.info(s"job incoming: user=$user, creator=$creator, engine=$engineType, code=$codeType")
  // ... 业务逻辑
  jobRequest
}
```

---

## 13. 前端模板（api.fetch / 模块注册 / 路由 / i18n）

> 完整模块模板见 [`frontend/module-template.md`](./frontend/module-template.md)。

### 13.1 API 调用（service.js）

```js
import api from '@/common/service/api';

const baseUrl = '/basedata-manager/xxx';   // 前缀对照 frontend/architecture.md §3

export const getList = (params) => api.fetch(baseUrl, params, 'get');
export const add = (data) => api.fetch(baseUrl, data, 'post');
export const edit = (data) => api.fetch(baseUrl, data, 'put');
export const del = (id) => api.fetch(`${baseUrl}/${id}`, 'delete');
```

**注意**：baseURL 默认 `/api/rest_j/v1/`，所以 `/udf/add` 实际打到 `/api/rest_j/v1/udf/add`，由 Gateway 转给后端。

### 13.2 模块注册（index.js）

```js
export default {
  name: 'XxxManagement',
  dispatchs: {
    // 需要通知其他模块时填，例如：Workbench: ['add']
  },
  component: () => import('./index.vue'),
};
```

### 13.3 路由注册（apps/<app>/router.js）

```js
export default [
  // ... existing
  {
    name: 'xxxManagement',
    path: 'xxxManagement',
    component: () => import('./module/xxxManagement/index.vue'),
    meta: {
      title: 'xxxManagement',
      publicPage: true,   // true 表示免鉴权
    },
  },
]
```

### 13.4 i18n（zh.json + en.json 双语同步）

```json
// src/apps/<app>/i18n/common/zh.json
{
  "message": {
    "linkis": {
      "xxx": {
        "name": "名称",
        "add": "新增",
        "edit": "编辑",
        "deleteSuccess": "删除成功"
      }
    }
  }
}
```

```json
// src/apps/<app>/i18n/common/en.json
{
  "message": {
    "linkis": {
      "xxx": {
        "name": "Name",
        "add": "Add",
        "edit": "Edit",
        "deleteSuccess": "Deleted successfully"
      }
    }
  }
}
```

调用：`$t('message.linkis.xxx.add')`。

### 13.5 iview Table + 操作列（vlist.vue 列定义）

```js
columns: [
  { title: this.$t('message.linkis.xxx.name'), key: 'name' },
  { title: this.$t('message.linkis.createTime'), key: 'createTime' },
  {
    title: this.$t('message.linkis.action'),
    key: 'action',
    render: (h, params) => (
      <div>
        <Button size="small" on-click={() => this.$emit('edit', params.row)}>
          {this.$t('message.linkis.edit')}
        </Button>
        <Button size="small" type="error" on-click={() => this.$emit('delete', params.row)}>
          {this.$t('message.linkis.delete')}
        </Button>
      </div>
    ),
  },
]
```

---

