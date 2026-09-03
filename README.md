# easy-open-search

面向 **OpenSearch only** 的轻量 ORM（类 MyBatis-Plus）。

- **写 / 索引：** `opensearch-java`
- **读 / 聚合 / 全文：** OpenSearch `sql-jdbc`
- **不兼容 Elasticsearch**（ES SQL 不对开源场景开放）

## 模块

| 模块 | 说明 |
|------|------|
| `easy-os-annotation` | `@IndexName` / `@IndexId` / `@IndexField` / `@Score` |
| `easy-os-core` | Wrapper、SQL 渲染、JDBC、Client 写、索引托管 |
| `easy-os-spring-boot-starter` | Boot 2.7 自动配置 + `@MapperScan` |
| `easy-os-spring-boot-sample` | 示例 |

## 快速开始

```xml
<dependency>
  <groupId>org.dromara.easy-open-search</groupId>
  <artifactId>easy-os-spring-boot-starter</artifactId>
  <version>0.1.0-SNAPSHOT</version>
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

## 索引模式

| 模式 | 行为 |
|------|------|
| `manual` | 启动不自动处理，调用 `createIndex()` 等 |
| `not_smoothly` | 启动按实体建索引；已存在则删建（会丢数据） |
| `smoothly` | 二期 |

## 设计文档

见 `docs/superpowers/specs/2026-09-03-easy-open-search-design.md`
