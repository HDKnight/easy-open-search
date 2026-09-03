package org.dromara.easyos.mapper;

import org.dromara.easyos.biz.PageInfo;
import org.dromara.easyos.conditions.LambdaQueryWrapper;
import org.dromara.easyos.index.IndexProcessor;
import org.dromara.easyos.jdbc.JdbcExecutor;
import org.dromara.easyos.jdbc.ResultSetMapper;
import org.dromara.easyos.property.EasyOsProperties;
import org.dromara.easyos.sql.BoundSql;
import org.dromara.easyos.sql.SqlRenderer;
import org.dromara.easyos.sql.ast.ConditionNode;
import org.dromara.easyos.sql.ast.QueryAst;
import org.dromara.easyos.sql.ast.SelectItem;
import org.dromara.easyos.toolkit.EntityMeta;
import org.dromara.easyos.toolkit.FieldUtils;
import org.dromara.easyos.write.DocumentWriter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class BaseMapperImpl<T> implements BaseMapper<T> {
    private final Class<T> entityClass;
    private final EasyOsProperties properties;
    private final JdbcExecutor jdbcExecutor;
    private final DocumentWriter documentWriter;
    private final IndexProcessor indexProcessor;
    private final SqlRenderer sqlRenderer = new SqlRenderer();

    public BaseMapperImpl(Class<T> entityClass,
                          EasyOsProperties properties,
                          JdbcExecutor jdbcExecutor,
                          DocumentWriter documentWriter,
                          IndexProcessor indexProcessor) {
        this.entityClass = entityClass;
        this.properties = properties;
        this.jdbcExecutor = jdbcExecutor;
        this.documentWriter = documentWriter;
        this.indexProcessor = indexProcessor;
    }

    private EntityMeta meta() {
        return EntityMeta.of(entityClass, properties);
    }

    private boolean mapUnderscore() {
        return properties.getGlobalConfig().getDbConfig().isMapUnderscoreToCamelCase();
    }

    private String indexName() {
        return meta().getIndexName();
    }

    private String idColumn() {
        String name = meta().getIdField().getName();
        return mapUnderscore() ? FieldUtils.camelToUnderline(name) : name;
    }

    @Override
    public int insert(T entity) {
        return documentWriter.insert(entity);
    }

    @Override
    public int insertBatch(Collection<T> list) {
        return documentWriter.insertBatch(list);
    }

    @Override
    public int deleteById(Serializable id) {
        return documentWriter.deleteById(entityClass, id);
    }

    @Override
    public int delete(LambdaQueryWrapper<T> wrapper) {
        List<T> list = selectList(wrapper);
        int count = 0;
        for (T entity : list) {
            Object id = meta().readId(entity);
            if (id != null) {
                count += documentWriter.deleteById(entityClass, (Serializable) id);
            }
        }
        return count;
    }

    @Override
    public int updateById(T entity) {
        return documentWriter.updateById(entity);
    }

    @Override
    public T selectById(Serializable id) {
        LambdaQueryWrapper<T> wrapper = new LambdaQueryWrapper<>();
        wrapper.setMapUnderscoreToCamelCase(mapUnderscore());
        // use raw column eq via temporary QueryAst
        QueryAst ast = new QueryAst();
        ast.setIndex(indexName());
        ast.setWhereRoot(ConditionNode.eq(idColumn(), id));
        ast.setLimit(1);
        BoundSql bound = sqlRenderer.render(ast);
        List<Map<String, Object>> maps = jdbcExecutor.queryMaps(bound);
        List<T> list = ResultSetMapper.mapsToEntities(maps, entityClass, mapUnderscore());
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public List<T> selectBatchIds(Collection<? extends Serializable> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        QueryAst ast = new QueryAst();
        ast.setIndex(indexName());
        ast.setWhereRoot(ConditionNode.in(idColumn(), new ArrayList<>(ids)));
        BoundSql bound = sqlRenderer.render(ast);
        return ResultSetMapper.mapsToEntities(jdbcExecutor.queryMaps(bound), entityClass, mapUnderscore());
    }

    @Override
    public T selectOne(LambdaQueryWrapper<T> wrapper) {
        wrapper.setMapUnderscoreToCamelCase(mapUnderscore());
        wrapper.limit(1);
        List<T> list = selectList(wrapper);
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public List<T> selectList(LambdaQueryWrapper<T> wrapper) {
        wrapper.setMapUnderscoreToCamelCase(mapUnderscore());
        BoundSql bound = wrapper.render(sqlRenderer, indexName());
        return ResultSetMapper.mapsToEntities(jdbcExecutor.queryMaps(bound), entityClass, mapUnderscore());
    }

    @Override
    public Long selectCount(LambdaQueryWrapper<T> wrapper) {
        wrapper.setMapUnderscoreToCamelCase(mapUnderscore());
        QueryAst ast = wrapper.toAst(indexName());
        QueryAst countAst = new QueryAst();
        countAst.setIndex(ast.getIndex());
        countAst.setWhereRoot(ast.getWhereRoot());
        countAst.addSelect(SelectItem.count("cnt"));
        return jdbcExecutor.queryLong(sqlRenderer.render(countAst));
    }

    @Override
    public List<Map<String, Object>> selectMaps(LambdaQueryWrapper<T> wrapper) {
        wrapper.setMapUnderscoreToCamelCase(mapUnderscore());
        return jdbcExecutor.queryMaps(wrapper.render(sqlRenderer, indexName()));
    }

    @Override
    public PageInfo<T> page(LambdaQueryWrapper<T> wrapper, int pageNum, int pageSize) {
        Long total = selectCount(wrapper);
        wrapper.setMapUnderscoreToCamelCase(mapUnderscore());
        wrapper.limit(pageSize);
        wrapper.offset(Math.max(pageNum - 1, 0) * pageSize);
        List<T> list = selectList(wrapper);
        return new PageInfo<>(total == null ? 0 : total, pageNum, pageSize, list);
    }

    @Override
    public boolean createIndex() {
        return indexProcessor.createIndex(entityClass);
    }

    @Override
    public boolean deleteIndex() {
        return indexProcessor.deleteIndex(entityClass);
    }

    @Override
    public boolean existsIndex() {
        return indexProcessor.existsIndex(entityClass);
    }
}
