# easy-open-search Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在同级目录新建 OpenSearch-only ORM 项目 `easy-open-search`：写走 `opensearch-java`，读/聚合/全文走 `sql-jdbc`，API 对齐 MyBatis-Plus 体验。

**Architecture:** 统一 `LambdaQueryWrapper` 产出 `QueryAst` → `SqlRenderer` → JDBC；`BaseMapper` 写方法走 `OpenSearchClient`；索引托管首期支持 `manual` + `not_smoothly`。不兼容 Elasticsearch。

**Tech Stack:** Java 8+、Maven 多模块、`opensearch-java` 2.x/3.x（实现时锁定与目标 OS 匹配的版本）、`org.opensearch.driver:opensearch-sql-jdbc:1.4.0.1`、Spring Boot 2.7.x、JUnit 5、Testcontainers（可选集成测）。

**Spec:** `docs/superpowers/specs/2026-09-03-easy-open-search-design.md`（评审稿在 easy-es；实现仓库创建后复制过去）

## Global Constraints

- OpenSearch only；禁止引入 `co.elastic.*` / Elasticsearch 依赖
- 包名：`org.dromara.easyos`
- 项目路径：`F:\code\java\workspace\opensource\easy-open-search`
- 读路径禁止字符串拼接用户输入；必须 `PreparedStatement` 参数绑定
- 首期索引模式：`manual` + `not_smoothly`；`smoothly` 不做
- SQL 插件不可用时异常文案必须含安装提示（见 spec §7.3）
- Spring Boot 首期 2.7.x；JDK 8+

---

## File Structure (target repo)

```text
easy-open-search/
├── pom.xml                          # aggregator
├── easy-os-parent/pom.xml           # dependencyManagement
├── easy-os-annotation/src/main/java/org/dromara/easyos/annotation/
│   ├── IndexName.java
│   ├── IndexId.java
│   ├── IndexField.java
│   ├── Score.java
│   └── ...
├── easy-os-core/src/main/java/org/dromara/easyos/
│   ├── exception/EasyOsException.java
│   ├── property/EasyOsProperties.java
│   ├── property/GlobalConfig.java
│   ├── enums/ProcessIndexMode.java
│   ├── toolkit/SFunction.java / FieldUtils.java
│   ├── sql/ast/QueryAst.java
│   ├── sql/ast/ConditionNode.java
│   ├── sql/BoundSql.java
│   ├── sql/SqlRenderer.java
│   ├── jdbc/JdbcUrlBuilder.java
│   ├── jdbc/SqlProbe.java
│   ├── jdbc/JdbcExecutor.java
│   ├── jdbc/ResultSetMapper.java
│   ├── client/OpenSearchClientFactory.java
│   ├── write/DocumentWriter.java
│   ├── index/IndexSchemaBuilder.java
│   ├── index/IndexProcessor.java
│   ├── index/ManualIndexProcessor.java
│   ├── index/NotSmoothlyIndexProcessor.java
│   ├── conditions/Wrapper.java
│   ├── conditions/AbstractWrapper.java
│   ├── conditions/LambdaQueryWrapper.java
│   ├── mapper/BaseMapper.java
│   ├── mapper/BaseMapperImpl.java
│   └── biz/PageInfo.java
├── easy-os-core/src/test/java/org/dromara/easyos/...
├── easy-os-spring-boot-starter/...
├── easy-os-spring-boot-sample/...
├── README.md
└── docs/superpowers/specs/2026-09-03-easy-open-search-design.md
```

---

### Task 1: 创建仓库与 Maven 骨架

**Files:**
- Create: `F:/code/java/workspace/opensource/easy-open-search/` 下全部 `pom.xml`、`.gitignore`、`README.md`（占位）
- Create: 复制 design spec 到新仓库 `docs/superpowers/specs/`

**Interfaces:**
- Produces: 可 `mvn -q -DskipTests package` 的空多模块工程；`groupId=io.github.hdknight`，`version=0.1.0-SNAPSHOT`

- [ ] **Step 1: 创建同级目录并 init git**

```powershell
mkdir F:\code\java\workspace\opensource\easy-open-search
cd F:\code\java\workspace\opensource\easy-open-search
git init
```

- [ ] **Step 2: 写入根 `pom.xml`（packaging=pom，modules 含 parent/annotation/core/starter/sample）与 `easy-os-parent/pom.xml`**

在 `dependencyManagement` 锁定至少：

```xml
<opensearch-java.version>2.18.0</opensearch-java.version>
<!-- 若目标集群为 OS 3.x，实现时改为 3.x 并全仓统一 -->
<opensearch-sql-jdbc.version>1.4.0.1</opensearch-sql-jdbc.version>
<spring-boot.version>2.7.18</spring-boot.version>
<jackson.version>2.15.4</jackson.version>
```

依赖坐标：

```xml
<dependency>
  <groupId>org.opensearch.client</groupId>
  <artifactId>opensearch-java</artifactId>
</dependency>
<dependency>
  <groupId>org.opensearch.driver</groupId>
  <artifactId>opensearch-sql-jdbc</artifactId>
</dependency>
```

- [ ] **Step 3: 各子模块空 `pom.xml` + `src/main/java` / `src/test/java` 包目录 `org.dromara.easyos`**

- [ ] **Step 4: 验证构建**

```powershell
mvn -q -DskipTests package
```

Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add .
git commit -m "chore: bootstrap easy-open-search multi-module skeleton"
```

---

### Task 2: 注解、配置、异常

**Files:**
- Create: `easy-os-annotation/.../IndexName.java`, `IndexId.java`, `IndexField.java`, `Score.java`
- Create: `easy-os-core/.../exception/EasyOsException.java`
- Create: `easy-os-core/.../enums/ProcessIndexMode.java`（`MANUAL`, `NOT_SMOOTHLY`；预留注释 `SMOOTHLY` 二期）
- Create: `easy-os-core/.../property/EasyOsProperties.java`, `GlobalConfig.java`
- Test: `easy-os-core/src/test/java/org/dromara/easyos/property/EasyOsPropertiesTest.java`

**Interfaces:**
- Produces:
  - `enum ProcessIndexMode { MANUAL, NOT_SMOOTHLY }`
  - `class EasyOsProperties { boolean enable; String address; String schema; String username; String password; boolean printSql; GlobalConfig globalConfig; }`
  - `class EasyOsException extends RuntimeException` + `static EasyOsException sqlPluginMissing()`

- [ ] **Step 1: 写失败测试 — `sqlPluginMissing` 消息含安装关键字**

```java
@Test
void sqlPluginMissingMessageContainsInstallHint() {
    EasyOsException ex = EasyOsException.sqlPluginMissing();
    assertTrue(ex.getMessage().contains("opensearch-sql"));
    assertTrue(ex.getMessage().contains("opensearch-plugin install"));
}
```

- [ ] **Step 2: 运行测试确认失败**

```powershell
mvn -pl easy-os-core -am test -Dtest=EasyOsPropertiesTest
```

Expected: 编译失败或测试失败（类不存在）

- [ ] **Step 3: 实现注解（运行时 Retention）、Properties、Exception**

`EasyOsException.sqlPluginMissing()` 消息必须包含：

```text
OpenSearch SQL plugin is required.
Check: bin/opensearch-plugin list  (expect opensearch-sql)
Install (minimal distro): bin/opensearch-plugin install opensearch-sql
Then restart the node.
Docs: https://docs.opensearch.org/latest/install-and-configure/plugins/
```

- [ ] **Step 4: 测试通过并 Commit**

```bash
git commit -m "feat: add annotations, properties, and SQL plugin exception"
```

---

### Task 3: QueryAst + SqlRenderer（eq/and + limit）

**Files:**
- Create: `easy-os-core/.../sql/ast/ConditionNode.java`, `QueryAst.java`, `SelectItem.java`, `OrderItem.java`
- Create: `easy-os-core/.../sql/BoundSql.java`（`String sql`, `List<Object> params`）
- Create: `easy-os-core/.../sql/SqlRenderer.java`
- Test: `easy-os-core/src/test/java/org/dromara/easyos/sql/SqlRendererTest.java`

**Interfaces:**
- Produces: `BoundSql SqlRenderer.render(QueryAst ast)`
- `QueryAst` 字段：`index`, `selectItems`, `whereRoot`, `groupBy`, `having`, `orders`, `limit`, `offset`, `includeScore`

- [ ] **Step 1: 写失败测试**

```java
@Test
void renderEqAndLimitUsesPlaceholders() {
    QueryAst ast = new QueryAst();
    ast.setIndex("article");
    ast.setWhereRoot(ConditionNode.eq("status", 1));
    ast.setLimit(10);
    ast.setOffset(20);
    BoundSql bound = new SqlRenderer().render(ast);
    assertEquals(
        "SELECT * FROM article WHERE status = ? LIMIT ? OFFSET ?",
        normalize(bound.getSql()));
    assertEquals(Arrays.asList(1, 10, 20), bound.getParams());
}
```

- [ ] **Step 2: 跑测确认失败 → 实现最小 `SqlRenderer` → 跑测通过**

- [ ] **Step 3: Commit**

```bash
git commit -m "feat: add QueryAst and SqlRenderer for eq/limit"
```

---

### Task 4: SqlRenderer 扩展 — like/in/between/or/orderBy

**Files:**
- Modify: `SqlRenderer.java`, `ConditionNode.java`
- Test: `SqlRendererTest.java` 增补用例

**Interfaces:**
- Produces: `ConditionNode.like/in/between/or/and/isNull/...`；`OrderItem.asc/desc`

- [ ] **Step 1: 测试 like 由框架加 `%`**

```java
@Test
void likeWrapsPercentInParamNotSql() {
    QueryAst ast = new QueryAst();
    ast.setIndex("article");
    ast.setWhereRoot(ConditionNode.like("title", "功夫"));
    BoundSql bound = new SqlRenderer().render(ast);
    assertTrue(bound.getSql().contains("title LIKE ?"));
    assertEquals("%功夫%", bound.getParams().get(0));
}
```

- [ ] **Step 2: 测试 IN 展开多个 `?`；OR 嵌套加括号**

- [ ] **Step 3: 实现并提交**

```bash
git commit -m "feat: extend SqlRenderer for like/in/between/or/orderBy"
```

---

### Task 5: 全文 + `_score` 渲染

**Files:**
- Modify: `ConditionNode.java`, `SqlRenderer.java`, `QueryAst.java`
- Test: `SqlRendererFulltextTest.java`

**Interfaces:**
- Produces: `ConditionNode.match(field,text)`, `matchPhrase`, `multiMatch(text, fields...)`；`QueryAst.orderByScore(boolean desc)`；可选 `scoreBoost` 节点 → `SCORE(MATCH(field, ?), ?)`

- [ ] **Step 1: 写测试**

```java
@Test
void matchAndOrderByScore() {
    QueryAst ast = new QueryAst();
    ast.setIndex("article");
    ast.setIncludeScore(true);
    ast.setWhereRoot(ConditionNode.match("title", "功夫"));
    ast.orderByScoreDesc();
    BoundSql bound = new SqlRenderer().render(ast);
    assertTrue(bound.getSql().contains("SELECT"));
    assertTrue(bound.getSql().contains("_score"));
    assertTrue(bound.getSql().contains("MATCH(title, ?)"));
    assertTrue(bound.getSql().toUpperCase().contains("ORDER BY _SCORE DESC"));
    assertEquals("功夫", bound.getParams().get(0));
}
```

- [ ] **Step 2: 实现 → 通过 → Commit**

```bash
git commit -m "feat: render MATCH family and ORDER BY _score"
```

---

### Task 6: 聚合 SQL 渲染

**Files:**
- Modify: `SelectItem.java`（支持 `COUNT(*) AS cnt` 等）、`SqlRenderer.java`
- Test: `SqlRendererAggTest.java`

**Interfaces:**
- Produces: `SelectItem.count(alias)`, `sum(field,alias)`, `avg/min/max`；`groupBy` 列表；`havingSql` + `havingParams`（having 仅允许别名/聚合表达式 + 绑定参数）

- [ ] **Step 1: 测试**

```java
@Test
void groupByWithCountSum() {
    QueryAst ast = new QueryAst();
    ast.setIndex("article");
    ast.addSelect(SelectItem.column("title"));
    ast.addSelect(SelectItem.count("cnt"));
    ast.addSelect(SelectItem.sum("star_num", "sumStar"));
    ast.addGroupBy("title");
    ast.setWhereRoot(ConditionNode.eq("status", 1));
    BoundSql bound = new SqlRenderer().render(ast);
    assertTrue(bound.getSql().contains("COUNT(*) AS cnt"));
    assertTrue(bound.getSql().contains("SUM(star_num) AS sumStar"));
    assertTrue(bound.getSql().contains("GROUP BY title"));
}
```

- [ ] **Step 2: 实现 → Commit**

```bash
git commit -m "feat: render GROUP BY aggregation select items"
```

---

### Task 7: JDBC URL、探测、执行器、ResultSet 映射

**Files:**
- Create: `JdbcUrlBuilder.java`, `SqlProbe.java`, `JdbcExecutor.java`, `ResultSetMapper.java`
- Test: `JdbcUrlBuilderTest.java`；`ResultSetMapperTest.java`（可用 H2 或 mock `ResultSet`）
- 集成测（可选，需本地 OS）：`JdbcExecutorIT.java` `@Tag("integration")`

**Interfaces:**
- Produces:
  - `String JdbcUrlBuilder.build(EasyOsProperties p)` → `jdbc:opensearch://http://host:9200`（按 schema）
  - `void SqlProbe.verify(Connection)` — 失败抛 `EasyOsException.sqlPluginMissing()`
  - `List<Map<String,Object>> JdbcExecutor.queryMaps(BoundSql)`
  - `<T> List<T> ResultSetMapper.toEntities(ResultSet, Class<T>, boolean mapUnderscore)`

- [ ] **Step 1: 单元测试 URL 拼装与 camelCase 映射**

```java
@Test
void buildJdbcUrl() {
    EasyOsProperties p = new EasyOsProperties();
    p.setSchema("https");
    p.setAddress("127.0.0.1:9200");
    assertEquals("jdbc:opensearch://https://127.0.0.1:9200", JdbcUrlBuilder.build(p));
}
```

- [ ] **Step 2: 实现 `JdbcExecutor`：DriverManager + try-with-resources；`printSql` 时打日志；捕获 SQLException 若 message/状态像插件缺失则转换 `sqlPluginMissing()`**

- [ ] **Step 3: Commit**

```bash
git commit -m "feat: add JDBC url builder, probe, executor, result mapper"
```

---

### Task 8: LambdaQueryWrapper

**Files:**
- Create: `toolkit/SFunction.java`, `FieldUtils.java`（解析 lambda 字段名）
- Create: `conditions/Wrapper.java`, `AbstractWrapper.java`, `LambdaQueryWrapper.java`
- Test: `LambdaQueryWrapperTest.java`（断言 `wrapper` → `QueryAst` / `BoundSql`）

**Interfaces:**
- Produces:
  - `LambdaQueryWrapper<T> eq(SFunction<T,?> col, Object val)` 等
  - `match` / `matchPhrase` / `multiMatch` / `orderByScoreDesc`
  - `select` / `selectCount` / `selectSum` / `groupBy`
  - `QueryAst toAst(String index)`；或 `BoundSql render(SqlRenderer, String index)`

- [ ] **Step 1: 测试链式条件转 SQL**

```java
@Test
void wrapperRendersMatchAndEq() {
    LambdaQueryWrapper<Doc> w = new LambdaQueryWrapper<>();
    w.match(Doc::getTitle, "功夫").eq(Doc::getStatus, 1).orderByScoreDesc();
    BoundSql sql = w.render(new SqlRenderer(), "article");
    assertTrue(sql.getSql().contains("MATCH(title, ?)"));
    assertTrue(sql.getParams().contains("功夫"));
    assertTrue(sql.getParams().contains(1));
}
```

- [ ] **Step 2: 实现（字段默认驼峰转下划线受 `mapUnderscoreToCamelCase` 配置控制，默认与 EE 一致可先实现「实体字段 → 列名」策略类 `ColumnNameConverter`）**

- [ ] **Step 3: Commit**

```bash
git commit -m "feat: add LambdaQueryWrapper building QueryAst"
```

---

### Task 9: OpenSearchClient 工厂与 DocumentWriter

**Files:**
- Create: `client/OpenSearchClientFactory.java`
- Create: `write/DocumentWriter.java`
- Test: `DocumentWriterTest.java`（可用 Mockito mock `OpenSearchClient`）

**Interfaces:**
- Produces:
  - `OpenSearchClient OpenSearchClientFactory.create(EasyOsProperties)`
  - `String insert(T entity)` / `boolean updateById(T entity)` / `boolean deleteById(Class<T> clazz, Object id)` / `int insertBatch(Collection<T>)`
  - 从 `@IndexName` / `@IndexId` 解析索引与主键；文档 body 用 Jackson

- [ ] **Step 1: 写 mock 测试 — insert 调用 `client.index`**

- [ ] **Step 2: 实现工厂（Apache HttpClient5 或 RestClientTransport，按所选 `opensearch-java` 版本官方示例）与 Writer**

- [ ] **Step 3: Commit**

```bash
git commit -m "feat: add OpenSearchClient factory and document writer"
```

---

### Task 10: BaseMapper 读 + 写组装

**Files:**
- Create: `mapper/BaseMapper.java`, `mapper/BaseMapperImpl.java`, `biz/PageInfo.java`
- Test: `BaseMapperImplTest.java`（mock JdbcExecutor + DocumentWriter）

**Interfaces:**
- Produces:

```java
public interface BaseMapper<T> {
    int insert(T entity);
    int insertBatch(Collection<T> list);
    int deleteById(Serializable id);
    int delete(LambdaQueryWrapper<T> wrapper);
    int updateById(T entity);
    int update(T entity, LambdaQueryWrapper<T> wrapper);
    T selectById(Serializable id);
    List<T> selectBatchIds(Collection<? extends Serializable> ids);
    T selectOne(LambdaQueryWrapper<T> wrapper);
    List<T> selectList(LambdaQueryWrapper<T> wrapper);
    Long selectCount(LambdaQueryWrapper<T> wrapper);
    List<Map<String, Object>> selectMaps(LambdaQueryWrapper<T> wrapper);
    PageInfo<T> page(LambdaQueryWrapper<T> wrapper, int pageNum, int pageSize);
    boolean createIndex();
    boolean deleteIndex();
    boolean existsIndex();
}
```

- [ ] **Step 1: 测试 `selectList` 走 `JdbcExecutor.queryMaps` 再映射实体；`insert` 走 Writer**

- [ ] **Step 2: 实现 `BaseMapperImpl`（持有 `Class<T>`, `EasyOsProperties`, `JdbcExecutor`, `DocumentWriter`, `IndexProcessor`）**

- [ ] **Step 3: `delete(wrapper)` / `update(entity, wrapper)` 首期可：先 `selectMaps` 取 id 再批量写删（简单正确）；或渲染 `DELETE/UPDATE` SQL——**若 OpenSearch SQL 写支持不稳，一律走 Client 按 id 批处理**（实现时以实测为准，默认 Client 批处理）**

- [ ] **Step 4: Commit**

```bash
git commit -m "feat: implement BaseMapper read/write facade"
```

---

### Task 11: 索引托管 manual + not_smoothly

**Files:**
- Create: `index/IndexSchemaBuilder.java`（实体 → settings/mappings JSON）
- Create: `index/IndexProcessor.java`, `ManualIndexProcessor.java`, `NotSmoothlyIndexProcessor.java`
- Test: `IndexSchemaBuilderTest.java`, `NotSmoothlyIndexProcessorTest.java`（mock client）

**Interfaces:**
- Produces: `void IndexProcessor.processOnStartup(Class<?> entityClass)`；`create/delete/exists` API
- `MANUAL`：startup no-op
- `NOT_SMOOTHLY`：不存在则 create；存在且 mapping 冲突策略：**删后建**（首期简单策略，文档写明数据会丢）

- [ ] **Step 1: 测试 schema 从 `@IndexField` 生成 properties**

- [ ] **Step 2: 实现处理器；`asyncProcessIndexBlocking=true` 时 startup 同步执行**

- [ ] **Step 3: Commit**

```bash
git commit -m "feat: add manual and not_smoothly index processors"
```

---

### Task 12: Spring Boot Starter

**Files:**
- Create: `easy-os-spring-boot-starter/.../EasyOsAutoConfiguration.java`
- Create: `EasyOsProperties` `@ConfigurationProperties(prefix="easy-open-search")`
- Create: `MapperScanner` / `MapperFactoryBean`（扫描 `BaseMapper` 接口并注入 `BaseMapperImpl`）
- Create: `META-INF/spring.factories`（Boot 2.7）
- Test: `@SpringBootTest` 加载 context（可用 mock bean）

**Interfaces:**
- Produces: 自动配置 `OpenSearchClient`、`DataSource`/Connection 供应、`SqlProbe`（`enable=true` 时启动探测）、按 `process-index-mode` 注册索引处理器

- [ ] **Step 1: 最小自动配置 + 属性绑定测试**

- [ ] **Step 2: Mapper 接口代理（JDK Proxy 调 `BaseMapperImpl`）**

- [ ] **Step 3: Commit**

```bash
git commit -m "feat: add Spring Boot 2.7 starter and mapper scan"
```

---

### Task 13: Sample + README（含插件安装）

**Files:**
- Create: `easy-os-spring-boot-sample` 示例实体/Mapper/`application.yml`
- Create: 根 `README.md`（中文）：快速开始、能力表、**SQL 插件确认/安装**、与 Easy-Es 关系（OS only）

- [ ] **Step 1: Sample 能展示 insert + match 查询 + groupBy selectMaps 代码**

- [ ] **Step 2: README 必须含 spec §7.3 安装命令**

- [ ] **Step 3: Commit**

```bash
git commit -m "docs: add sample module and README with SQL plugin setup"
```

---

### Task 14: 集成验证清单（人工 / IT）

**Files:**
- Create: `easy-os-core/src/test/java/.../IT` 或 sample 的 `src/test`
- Modify: README「验证」一节

- [ ] **Step 1: 对真实 OpenSearch（标准发行版）执行：**

1. insert 一条文档  
2. `selectList` + `eq`  
3. `match` + `orderByScoreDesc`  
4. `selectMaps` + `groupBy` + `count`  
5. `process-index-mode: not_smoothly` 启动自动建索引  
6. 停掉 SQL 插件或连错集群时，异常含安装提示  

- [ ] **Step 2: 记录版本矩阵到 README（已测 OS 版本 + jdbc + opensearch-java）**

- [ ] **Step 3: Commit**

```bash
git commit -m "test: add integration checklist and version matrix notes"
```

---

## Spec coverage self-check

| Spec 项 | Task |
|--------|------|
| OpenSearch only / 不兼容 ES | Global Constraints + Task 1 依赖 |
| 写 Client / 读 JDBC | Task 7–10 |
| MP 条件/CRUD/分页 | Task 3–4, 8, 10 |
| 聚合 ORM | Task 6, 8, 10 |
| 全文 + `_score` | Task 5, 8 |
| manual + not_smoothly | Task 11 |
| smoothly 二期 | 未实现（符合） |
| JDBC 随包 | Task 1, 7 |
| 插件安装提示 | Task 2, 7, 13 |
| Boot 2.7 Starter | Task 12 |
| 同级目录新项目 | Task 1 |

## Placeholder / 一致性备注

- `opensearch-java` 精确大版本在 Task 1 按目标集群锁定（2.18.0 为占位默认；OS 3 则改 3.x），全仓只允许一个版本。
- `delete(wrapper)` / `update(entity,wrapper)` 默认 **Client 按 id 批处理**，避免依赖 SQL DDL/DML 写能力。
- `SMOOTHLY` 仅枚举预留或文档标明二期，首期不要实现半成品。
