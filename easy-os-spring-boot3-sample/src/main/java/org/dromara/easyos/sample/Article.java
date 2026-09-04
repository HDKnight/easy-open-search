package org.dromara.easyos.sample;

import lombok.Getter;
import lombok.Setter;
import org.dromara.easyos.annotation.IndexField;
import org.dromara.easyos.annotation.IndexId;
import org.dromara.easyos.annotation.IndexName;
import org.dromara.easyos.annotation.Score;
import org.dromara.easyos.annotation.rely.IdType;

@Setter
@Getter
@IndexName("article")
public class Article {
    @IndexId(type = IdType.UUID)
    private String id;
    @IndexField(type = "text")
    private String title;
    @IndexField(type = "integer")
    private Integer status;
    @IndexField(type = "integer")
    private Integer starNum;
    @Score
    private Double score;

}
