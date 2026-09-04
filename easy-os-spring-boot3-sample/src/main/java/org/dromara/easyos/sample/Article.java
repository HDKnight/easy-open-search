package org.dromara.easyos.sample;

import org.dromara.easyos.annotation.IndexField;
import org.dromara.easyos.annotation.IndexId;
import org.dromara.easyos.annotation.IndexName;
import org.dromara.easyos.annotation.Score;
import org.dromara.easyos.annotation.rely.IdType;

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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getStarNum() {
        return starNum;
    }

    public void setStarNum(Integer starNum) {
        this.starNum = starNum;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }
}
