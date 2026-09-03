# easy-open-search 注解式自定义 SQL（@OsSelect）

**日期：** 2026-09-03  
**状态：** 已认可并实现

## 范围

- 仅注解式查询：`@OsSelect` + `@Param` + `#{name}`
- 不做：`@OsUpdate`、XML、`${}`、动态 SQL、分页注解

## 行为

- Mapper 代理优先识别方法上的 `@OsSelect`，走 `JdbcExecutor`
- 其余方法仍委托 `BaseMapperImpl`
- `print-sql` 与耗时日志沿用现有逻辑
