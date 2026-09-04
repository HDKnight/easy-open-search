package org.dromara.easyos.sample;

import org.dromara.easyos.annotation.OsSelect;
import org.dromara.easyos.annotation.Param;
import org.dromara.easyos.mapper.BaseMapper;

import java.util.List;
import java.util.Map;

public interface ArticleMapper extends BaseMapper<Article> {

    @OsSelect("SELECT * FROM article WHERE status = #{status}")
    List<Article> listByStatus(@Param("status") Integer status);

    @OsSelect("SELECT * FROM article WHERE id = #{id}")
    Article findByIdSql(@Param("id") String id);

    @OsSelect("SELECT title, status, star_num FROM article WHERE status = #{status}")
    List<Map<String, Object>> mapsByStatus(@Param("status") Integer status);
}
