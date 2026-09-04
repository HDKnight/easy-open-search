# easy-open-search 设计规格

**日期：** 2026-09-03  
**状态：** 已认可（2026-09-03）  
**项目目录（实现时创建）：** `F:\code\java\workspace\opensource\easy-open-search`（与 `easy-es` 同级）  
**Maven groupId：** `io.github.hdknight`（建议）  
**Java 包名：** `org.dromara.easyos`

---

## 1. 背景与目标

从 Easy-Es 演进需求出发：服务最新 OpenSearch，并解决聚合难用、希望接近 MySQL + MyBatis-Plus 体验的问题。

**产品定位：** 面向 **OpenSearch only** 的轻量 ORM（类 MyBatis-Plus），而非完整搜索中台。

**不兼容 Elasticsearch（硬性决策）：**

- 本项目**只支持 OpenSearch**，不做 ES 双栈、不做 ES 客户端/SQL 兼容层。
- 原因：读路径依赖 OpenSearch SQL 插件 + `sql-jdbc`；Elasticsearch 的 SQL 能力（如 X-Pack SQL）**不对普通开源场景开放/不可作为本项目依赖前提**，因此无法也不应把 ES 纳入同一套 JDBC/ORM 读模型。
- Easy-Es 继续服务 Elasticsearch；本项目与 Easy-Es **并列**，互不替代对方引擎。

**核心策略：**


| 通道                    | 用途                        |
| --------------------- | ------------------------- |
| `opensearch-java`     | 增删改、索引托管（建索引 / mapping）   |
| OpenSearch `sql-jdbc` | 查询、分页、聚合、全文检索、`_score` 排序 |


上层提供**统一 Wrapper / Mapper API**；不向用户暴露手写 `executeSQL` 作为主路径。

**明确不做（首期）：** Elasticsearch 兼容、highlight、function_score、knn、geo、nested、search_after、管道聚合、完整平滑索引迁移（`smoothly` 二期）。

---

## 2. 能力范围（首期）

### 2.1 对齐 MySQL + MyBatis-Plus

- 条件：`eq/ne/gt/ge/lt/le`、`like`、`in`、`between`、`isNull/isNotNull`、`and/or`、嵌套条件
- 投影 / 排序 / 分页：`select`、`orderBy`、`LIMIT/OFFSET` 分页
- CRUD：`insert` / `insertBatch` / `deleteById` / `delete` / `updateById` / `update`
- 查询：`selectById` / `selectBatchIds` / `selectOne` / `selectList` / `selectCount` / `selectMaps`
- 聚合：`groupBy` + `count/sum/avg/min/max` + 基础 `having`（经 SQL 渲染，非原生 Aggregation DSL，非 `executeSQL` 主 API）

### 2.2 全文检索 + 简单评分（一并首期落地）

- `match` / `matchPhrase` / `multiMatch`
- `orderByScoreDesc` / `orderByScoreAsc`（`ORDER BY _score`）
- 可选：`score(matchExpr, boost)` → OpenSearch SQL `SCORE(MATCH(...), boost)`
- 实体可用 `@Score` 映射 `_score`；或 `selectMaps` 中取 `_score`

### 2.3 索引托管（可配置）

对齐 Easy-Es 的 `process-index-mode`：


| 模式             | 首期                           |
| -------------- | ---------------------------- |
| `manual`       | 必达：启动不自动处理，提供 Mapper 索引 API  |
| `not_smoothly` | 必达：启动按实体创建/重建索引（允许短中断）       |
| `smoothly`     | **紧随（二期）**：别名 + reindex 平滑迁移 |


索引相关操作一律走 **Client 写通道**，不经 JDBC。

---

## 3. 总体架构

```text
                ┌──────────────────────────────────┐
                │  统一 Wrapper / BaseMapper API    │
                │  org.dromara.easyos.*             │
                └───────────────┬──────────────────┘
                                │
          ┌─────────────────────┼─────────────────────┐
          ▼                                           ▼
   Write / Index                               Query / Agg / Fulltext
   opensearch-java                             SQL AST → SqlRenderer
   createIndex / mapping                       → sql-jdbc → ResultSet
          │                                           │
          └──────────────────┬────────────────────────┘
                             ▼
                      OpenSearch Cluster
                   (需 SQL 插件可用，见 §7)
```

**模块划分：**

```text
easy-open-search/
├── easy-os-parent
├── easy-os-annotation
├── easy-os-core          # wrapper / sql / jdbc / write / index / mapper
├── easy-os-spring-boot-starter
└── easy-os-spring-boot-sample
```

**与 Easy-Es 关系：**

- 新项目，**不保证** API 二进制兼容 Easy-Es
- **借鉴/移植**写能力、注解思路、`manual`/`not_smoothly`、配置结构
- **不依赖** `easy-es` Maven 产物（避免 Elasticsearch Client 传递依赖）

---

## 4. Mapper 与 Wrapper API

### 4.1 BaseMapper


| 分类  | 方法                                                                                          |
| --- | ------------------------------------------------------------------------------------------- |
| 写   | `insert` / `insertBatch` / `deleteById` / `delete` / `updateById` / `update`                |
| 读   | `selectById` / `selectBatchIds` / `selectOne` / `selectList` / `selectCount` / `selectMaps` |
| 分页  | `page`（`LIMIT/OFFSET`）                                                                      |
| 索引  | `createIndex` / `deleteIndex` / `existsIndex`（手动挡）                                          |


### 4.2 Wrapper（读路径 → SQL）

类 MP 方法见 §2.1；全文扩展：

```java
wrapper.match(Doc::getTitle, "功夫")
       .matchPhrase(Doc::getContent, "传统功夫")
       .multiMatch("老汉", Doc::getTitle, Doc::getContent)
       .eq(Doc::getStatus, 1)
       .orderByScoreDesc();
```

期望 SQL 形态：

```sql
SELECT id, title, _score
FROM article
WHERE MATCH(title, ?)
  AND MATCH_PHRASE(content, ?)
  AND MULTI_MATCH(...)
  AND status = ?
ORDER BY _score DESC
LIMIT ? OFFSET ?
```

### 4.3 聚合（ORM 风格，非 executeSQL）

```java
wrapper.select(Doc::getTitle)
       .selectCount("cnt")
       .selectSum(Doc::getStarNum, "sumStar")
       .eq(Doc::getStatus, 1)
       .groupBy(Doc::getTitle)
       .having("cnt > ?", 10);
List<Map<String, Object>> rows = mapper.selectMaps(wrapper);
```

底层生成 `GROUP BY` SQL，经 JDBC 执行并映射为 `List<Map>`（或后续扩展 VO）。

---

## 5. SQL 渲染与 JDBC 执行

```text
LambdaQueryWrapper
  → QueryAst
  → SqlRenderer（OpenSearch SQL 方言）
  → BoundSql(sql, params[])
  → JdbcExecutor（PreparedStatement）
  → ResultSetMapper
```

**硬性要求：**

- 用户输入一律参数绑定；禁止拼接进 SQL 文本
- `like` 由框架处理通配符
- 配置 `print-sql` 可打印 SQL（敏感参数可脱敏）
- `map-underscore-to-camel-case` 语义对齐 Easy-Es

**结果映射：**


| API                        | 结果                          |
| -------------------------- | --------------------------- |
| `selectList` / `selectOne` | 实体                          |
| `selectMaps`               | `List<Map<String,Object>>`  |
| `selectCount`              | `Long`                      |
| `_score`                   | `@Score` 字段或 Map 键 `_score` |


---

## 6. 配置（示意）

```yaml
easy-open-search:
  enable: true
  banner: true
  print-sql: true
  schema: http
  address: 127.0.0.1:9200
  username: admin
  password: admin
  global-config:
    process-index-mode: manual   # manual | not_smoothly | smoothly(二期)
    async-process-index-blocking: true
    distributed: true
    db-config:
      index-prefix: ""
      map-underscore-to-camel-case: true
      id-type: none
```

Starter 用**同一套** address/认证/SSL 信息组装：

1. `OpenSearchClient`（写 / 索引）
2. `jdbc:opensearch://...` DataSource/Connection（读）

---

## 7. JDBC 驱动与 SQL 插件（含安装提示）

### 7.1 分工


| 组件                    | 位置                  | 是否随 easy-open-search 提供           |
| --------------------- | ------------------- | --------------------------------- |
| **sql-jdbc 驱动**       | 应用 classpath        | **是** — Starter/core 以 Maven 依赖引入 |
| **opensearch-sql 插件** | OpenSearch **集群节点** | **否** — 无法由 Java 库远程安装到集群         |


客户端库不能、也不应尝试向远程集群静默安装服务端插件（权限、版本、托管环境均不允许）。

### 7.2 集群侧现状

- **标准 OpenSearch 发行版**：默认捆绑 `opensearch-sql`，一般开箱可用
- **minimal 发行版**：默认不带，需手动安装
- **托管服务**（如 Amazon OpenSearch Service）：通常已启用 SQL/PPL，以厂商文档为准

### 7.3 安装 / 确认方式（文档与启动提示必须包含）

**1）确认插件是否已安装：**

```bash
bin/opensearch-plugin list
# 期望看到 opensearch-sql
```

或：

```http
GET _cat/plugins?v
```

**2）minimal / 未安装时的安装示例：**

```bash
# 在 OpenSearch 安装目录执行（版本需与集群一致）
bin/opensearch-plugin install opensearch-sql

# 或按官方文档使用插件名 / zip / Maven 坐标安装
# 安装后必须重启节点
```

参考：[Installing plugins](https://docs.opensearch.org/latest/install-and-configure/plugins/)  
SQL 能力说明：[SQL](https://docs.opensearch.org/latest/sql-and-ppl/sql/index/)  
JDBC 驱动：[opensearch-project/sql-jdbc](https://github.com/opensearch-project/sql-jdbc)

**3）连通性探测（框架行为）：**

- 应用启动或首次查询前，探测 SQL 端点（如 `POST /_plugins/_sql` 或 JDBC 握手）
- 若不可用：抛出明确异常，**附带上述安装/确认步骤摘要**（不要只写 “SQL not available”）

**4）README / 快速开始：**

- 写明：标准发行版通常无需额外装插件；minimal 必须先装 `opensearch-sql` 再使用查询/聚合/全文 API
- 写明：jdbc 驱动已由本项目依赖管理，业务方一般无需再单独引入

---

## 8. 错误处理


| 场景                  | 行为                             |
| ------------------- | ------------------------------ |
| SQL 插件不可用           | `EasyOsException` + 安装提示（§7.3） |
| 渲染出不支持的 SQL         | 快速失败，明确不支持的语法点                 |
| `not_smoothly` 索引失败 | 日志 + 可配置是否阻断启动                 |
| `manual`            | 不自动处理索引                        |
| Client 写失败          | 包装 `EasyOsException`，保留 cause  |


不吞异常；读写共用异常类型体系。

---

## 9. 依赖（首期）

- `org.opensearch.client:opensearch-java`（及所需 transport）
- OpenSearch SQL JDBC 驱动（Maven 坐标以官方最新为准，打进 core/starter）
- Spring Boot Starter：**首期对齐 Spring Boot 2.7.x**（与当前 Easy-Es 一致）；JDK 8+；Boot 3 支持放二期
- Jackson、lombok

---

## 10. 非目标与二期

**首期非目标：** Elasticsearch 兼容、Easy-Es API 全量兼容、highlight、knn、geo、nested、search_after、`smoothly`、读写都走 JDBC。

**二期：**

- `process-index-mode: smoothly`
- 更强全文参数（operator、boost 细粒度暴露）
- 聚合结果强类型 VO
- 如有必要：Client 查询逃生舱（仅高级场景）

---

## 11. 已确认决策摘要

1. 新项目 `easy-open-search`，包名 `org.dromara.easyos`，目录与 `easy-es` 同级
2. **仅 OpenSearch**；不兼容 ES（ES SQL 不对开源场景开放，读路径无法共用）
3. 方案：写 Client + 读 JDBC，统一 Wrapper
4. 能力：MP 级 CRUD/条件/分页/聚合 + 全文 match 族 + 简单 `_score` 排序
5. 索引：首期 `manual` + `not_smoothly`；`smoothly` 紧随
6. 不用 `executeSQL` 作为聚合主路径；ORM 生成 SQL
7. JDBC 驱动随包；SQL 插件不随包安装，但文档与启动错误必须提示安装方式

---

## 12. 实现前注意

本规格文件当前落在 `easy-es` 仓库的 `docs/superpowers/specs/`，便于评审。  
**实现阶段**应在同级目录创建独立 Git 仓库 `easy-open-search`，可将本规格复制到新仓库并以此为实施依据。