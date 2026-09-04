# easy-open-search

面向 **OpenSearch only** 的轻量 ORM（类 MyBatis-Plus）。

- **写 / 索引：** `opensearch-java`
- **读 / 聚合 / 全文：** OpenSearch `sql-jdbc`
- **不兼容 Elasticsearch**（ES SQL 不对开源场景开放）

## 模块

| 模块 | 说明 |
|------|------|
| `easy-os-annotation` | `@IndexName` / `@IndexId` / `@IndexField` / `@Score` / `@OsSelect` |
| `easy-os-core` | Wrapper、SQL 渲染、JDBC、Client 写、索引托管 |
| `easy-os-spring-boot-starter` | **Spring Boot 2.7** 自动配置 + `@MapperScan`（Java 8+） |
| `easy-os-spring-boot3-starter` | **Spring Boot 3.x** 自动配置 + `@MapperScan`（Java 17+） |
| `easy-os-spring-boot-sample` | Boot 2 示例 |
| `easy-os-spring-boot3-sample` | Boot 3 示例 |

## 快速开始

**Spring Boot 2.7：**

```xml
<dependency>
  <groupId>io.github.hdknight</groupId>
  <artifactId>easy-os-spring-boot-starter</artifactId>
  <version>0.1.2</version>
</dependency>
<!-- Boot 2.7 必须显式覆盖，否则会出现 NoClassDefFoundError: jakarta/json/JsonException -->
<dependency>
  <groupId>jakarta.json</groupId>
  <artifactId>jakarta.json-api</artifactId>
  <version>2.1.3</version>
</dependency>
<dependency>
  <groupId>org.eclipse.parsson</groupId>
  <artifactId>parsson</artifactId>
  <version>1.1.5</version>
</dependency>
```

**Spring Boot 3.x（推荐，无需再手写 jakarta.json）：**

```xml
<dependency>
  <groupId>io.github.hdknight</groupId>
  <artifactId>easy-os-spring-boot3-starter</artifactId>
  <version>0.1.2</version>
</dependency>
```

```yaml
easy-open-search:
  # 是否启用
  enable: true
  # 是否打印 Banner
  banner: true
  # 是否打印 SQL（含耗时）
  print-sql: true
  # http / https
  schema: http
  # host:port
  address: 127.0.0.1:9200
  # 账号（可缺省）
  username: admin
  password: admin
  # 信任自签名证书（仅开发环境）
  trust-self-signed: true
  global-config:
    # manual | not_smoothly（smoothly 未实现）
    process-index-mode: manual
    # 异步建索引是否阻塞主线程（预留）
    async-process-index-blocking: true
    # 是否分布式（预留）
    distributed: true
    db-config:
      # 索引名前缀
      index-prefix: ""
      # 下划线转驼峰
      map-underscore-to-camel-case: true
      # none | uuid(UUIDv7) | customize
      id-type: uuid
```

```java
@IndexId(type = IdType.UUID) // 插入时自动生成 UUIDv7；也可只配全局 id-type
private String id;
```

### 自定义 SQL（@OsSelect）

```java
public interface ArticleMapper extends BaseMapper<Article> {
    @OsSelect("SELECT * FROM article WHERE status = #{status}")
    List<Article> listByStatus(@Param("status") Integer status);
}
```

支持返回 `T` / `List<T>` / `Map` / `List<Map>` / 数值类型；仅支持 `#{}`，不支持 `${}` / XML。

```java
@MapperScan("com.example.mapper")
@SpringBootApplication
public class App { }

public interface ArticleMapper extends BaseMapper<Article> {}

wrapper.match(Article::getTitle, "功夫")
       .eq(Article::getStatus, 1)
       .orderByScoreDesc();
List<Article> list = articleMapper.selectList(wrapper);
```

## 构建

```powershell
$env:JAVA_HOME="D:\Java\jdk1.8.0_202"
$env:Path="$env:JAVA_HOME\bin;D:\Program Files\Apache\maven-3.9.10\bin;$env:Path"
cd F:\code\java\workspace\opensource\easy-open-search
mvn clean test
```

项目已提供 `.mvn/jvm.config`（限制堆、关闭 CompressedOops），避免部分 Windows/JDK8 环境出现 `Chunk::new` 原生 OOM。

## OpenSearch SQL 插件（必读）


JDBC **驱动已随本项目依赖引入**；**服务端 SQL 插件**需集群侧可用。

标准 OpenSearch 发行版一般已捆绑 `opensearch-sql`。minimal 发行版需安装：

```bash
bin/opensearch-plugin list
# 期望看到 opensearch-sql

bin/opensearch-plugin install opensearch-sql
# 安装后重启节点
```

文档：

- https://docs.opensearch.org/latest/install-and-configure/plugins/
- https://docs.opensearch.org/latest/sql-and-ppl/sql/index/
- https://github.com/opensearch-project/sql-jdbc

启动时若探测失败，异常信息会包含上述安装提示。

## Spring Boot 2.7 集成注意（jakarta.json）

仅影响 **`easy-os-spring-boot-starter`（Boot 2）**。  
请使用上文「快速开始」中的显式依赖；或改用 **`easy-os-spring-boot3-starter`**。

## 依赖安全说明

库侧已做：

- 统一 **Jackson 2.17.x**（覆盖 Boot 2.7 / 旧传递依赖）
- 钉住 **SnakeYAML 1.33**、**commons-codec**
- 排除 **aws-java-sdk-core**（默认非 AWS；若需 SigV4 请自行加回）
- 排除 **yasson / glassfish jakarta.json**（本库用 Jackson JSON-P）

业务侧仍在 Boot 2.7 时，建议在 `dependencyManagement` 中同样 import `jackson-bom`。  
**Spring / Tomcat 5.3 / 9.0 线本身已 EOL**，IDE 红盾可能无法完全消掉，长期请迁 **Boot 3 + `easy-os-spring-boot3-starter`**。

## 索引模式

| 模式 | 行为 |
|------|------|
| `manual` | 启动不自动处理，调用 `createIndex()` 等 |
| `not_smoothly` | 启动按实体建索引；已存在则跳过创建 |
| `smoothly` | 二期 |

## 设计文档

见 `./docs/superpowers/specs/2026-09-03-easy-open-search-design.md`
