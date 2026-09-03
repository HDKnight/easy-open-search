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
  address: 127.0.0.1:9200
  global-config:
    process-index-mode: manual   # manual | not_smoothly
```

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
