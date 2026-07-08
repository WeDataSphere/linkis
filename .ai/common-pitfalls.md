# 常见踩坑与反例

> 收录 8 个本项目实战中高频出现的"反模式 + 正确写法"对照。
> 通用规范见 [`../CLAUDE.md`](../CLAUDE.md) 第五章；可复用代码模板见 [snippets.md](./snippets.md)。

---

## ❌ 错误 1：字符编码使用字符串

### 反例
```java
// 字符串易拼错、编译器无法检查
String content = new String(bytes, "UTF-8");
response.setCharacterEncoding("UTF-8");
```

### 正例
```java
import java.nio.charset.StandardCharsets;

String content = new String(bytes, StandardCharsets.UTF_8);
response.setCharacterEncoding(StandardCharsets.UTF_8.name());
```

---

## ❌ 错误 2：新功能未加开关 / 未降级

### 反例
```java
// 直接生效，出问题只能回滚代码
public void executeNewFeature() {
    newAlgorithm();
}
```

### 正例
```scala
// 1. 配置开关
object NewFeatureConfiguration {
  val ENABLE = CommonVars("linkis.new.feature.enable", false)
}
```

```java
// 2. 开关 + tryCatch 降级
public void executeFeature() {
    if (!NewFeatureConfiguration.ENABLE.getValue()) {
        executeLegacyFeature();   // 关闭时走老路径
        return;
    }
    try {
        executeNewFeature();
    } catch (Exception e) {
        logger.warn("New feature failed, fallback to legacy", e);
        executeLegacyFeature();    // 异常时降级（详见 CLAUDE.md §4.2）
    }
}
```

---

## ❌ 错误 3：表结构变更未记录 / 直接 ALTER

### 反例
```sql
-- 直接在数据库执行，没有变更记录
ALTER TABLE linkis_ps_job_history_group_history
ADD COLUMN new_field VARCHAR(50) COMMENT 'new field';
```

### 正例
```sql
-- 在 linkis-dist/package/db/upgrade/dev-2.0.0/<module>.sql 添加
-- ================================================================
-- 版本: 2.0.0
-- 需求: 添加任务扩展字段支持
-- 日期: 2026-07-08
-- ================================================================
ALTER TABLE linkis_ps_job_history_group_history
ADD COLUMN new_field VARCHAR(50) COMMENT 'new field for extended info';

CREATE INDEX idx_new_field ON linkis_ps_job_history_group_history(new_field);
```

详见 [rules.md §1](./rules.md#1-数据库修改原则)。

---

## ❌ 错误 4：异常处理不规范

### 反例
```java
// 1. 吞掉异常
try { processData(); } catch (Exception e) { /* 什么都不做 */ }

// 2. printStackTrace
try { processData(); } catch (Exception e) {
    e.printStackTrace();
    throw e;
}

// 3. 捕获过宽
try { processData(); } catch (Throwable t) { ... }
```

### 正例
```java
// Scala 用 Utils.tryCatch（见 snippets §2）
// Java 用 try/catch，但必须：
//   ① 用项目统一 logger（不 printStackTrace）
//   ② 抛出业务异常（LinkisException 子类）
//   ③ 不要把受检异常直接抛给上层
try {
    processData();
} catch (IOException e) {
    logger.error("Failed to process data", e);
    throw new DataProcessException("Failed to process data", e);
}
```

---

## ❌ 错误 5：日志不规范

### 反例
```java
System.out.println("Processing: " + data);        // 1. System.out
logger.error("User {} logged in", username);       // 2. 级别错（登录不是 error）
logger.info("user: " + username);                  // 3. 字符串拼接
logger.info("Password: {}", password);             // 4. 敏感信息（CLAUDE.md §8 第 4 条禁止）
```

### 正例
```java
private static final Logger logger = LoggerFactory.getLogger(ClassName.class);

logger.info("User {} logged in successfully", username);
logger.warn("Login attempt from unknown IP: {}", ip);
logger.error("Failed to authenticate user {}", username, e);  // 注意带异常参数

// 推前端日志用 LogUtils.generateInfo / generateWarn / generateError
// （详见 snippets §3）
```

**敏感信息处理**：
- 不打印 token、密码、用户凭证；
- 需要记录 SQL 时用 `CodeUtils.maskCode(code, engineType)` 脱敏（见 CLAUDE.md §8）。

---

## ❌ 错误 6：REST 接口返回值不规范

### 反例
```java
@RequestMapping("/getData")
public UserData getData() { return userData; }     // 直接返回业务对象

@RequestMapping("/save")
public String save(@RequestBody Data data) { return "success"; }   // 直接返回 String
```

### 正例
```java
import org.apache.linkis.server.Message;
import org.apache.linkis.server.utils.ModuleUserUtils;

@GetMapping("/getData")
public Message getData(HttpServletRequest req) {
    try {
        String username = ModuleUserUtils.getOperationUser(req, "getData");
        UserData data = userService.getData(username);
        return Message.ok("Query successful").data("userData", data);
    } catch (Exception e) {
        logger.error("Failed to get user data", e);
        return Message.error("Failed to get user data: " + e.getMessage());
    }
}
```

**禁止**：用 `request.getUserPrincipal().getName()` 取用户——必须 `ModuleUserUtils.getOperationUser(req)`（见 snippets §11）。

---

## ❌ 错误 7：MyBatis SQL 注入

### 反例
```xml
<select id="selectByName" resultType="User">
    SELECT * FROM user WHERE name = '${name}'    <!-- ${} 直接拼接 -->
</select>
```

### 正例
```xml
<select id="selectByName" resultType="User">
    SELECT * FROM user WHERE name = #{name}      <!-- #{} 参数化 -->
</select>
```

**动态字段名**（如 `ORDER BY ${orderBy}`）必须在 Service 层做白名单校验：
```java
List<String> allowedFields = Arrays.asList("id", "name", "create_time");
if (!allowedFields.contains(orderBy)) {
    throw new IllegalArgumentException("Invalid order field: " + orderBy);
}
```

---

## ❌ 错误 8：事务使用不当

### 反例
```java
// 1. 多写操作未加事务
public void createOrder(Order order) {
    orderMapper.insert(order);
    stockMapper.decrease(order.getProductId());
    // 减库存失败 → 数据不一致
}

// 2. 加了事务但异常被吞
@Transactional
public void processOrder(Order order) {
    try {
        orderMapper.insert(order);
        stockMapper.decrease(order.getProductId());
    } catch (Exception e) {
        logger.error("Error", e);    // 异常被吞，事务不回滚
    }
}
```

### 正例
```java
@Transactional(rollbackFor = Exception.class)
public void createOrder(Order order) {
    orderMapper.insert(order);
    stockMapper.decrease(order.getProductId());
}

// 需要捕获特定异常时 → 重新抛出
@Transactional(rollbackFor = Exception.class)
public void processOrder(Order order) {
    try {
        orderMapper.insert(order);
        stockMapper.decrease(order.getProductId());
    } catch (StockNotEnoughException e) {
        logger.warn("Stock not enough for product: {}", order.getProductId());
        throw e;    // 重新抛出，触发回滚
    } catch (Exception e) {
        logger.error("Unexpected error", e);
        throw new OrderProcessException("Failed to process order", e);
    }
}
```

**注意**：外部调用（RPC、HTTP、发邮件）**不要**放在事务内，避免长事务。

---

## 自检清单

代码提交前对照：

- [ ] 字符编码用 `StandardCharsets.UTF_8`
- [ ] 新功能有开关（默认 false）+ 异常降级
- [ ] DDL/DML 变更已记录到 `linkis-dist/package/db/...`
- [ ] 异常用 `Utils.tryCatch` / 项目 logger，不 `printStackTrace`
- [ ] 日志不打敏感信息（CLAUDE.md §8）
- [ ] REST 返回 `Message.ok()` / `Message.error()`
- [ ] MyBatis 用 `#{}` 不用 `${}`
- [ ] 多写操作加 `@Transactional(rollbackFor = Exception.class)`
