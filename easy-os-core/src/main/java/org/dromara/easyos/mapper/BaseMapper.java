package org.dromara.easyos.mapper;

import org.dromara.easyos.biz.PageInfo;
import org.dromara.easyos.conditions.LambdaQueryWrapper;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface BaseMapper<T> {
    int insert(T entity);

    int insertBatch(Collection<T> list);

    int deleteById(Serializable id);

    int delete(LambdaQueryWrapper<T> wrapper);

    int updateById(T entity);

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
