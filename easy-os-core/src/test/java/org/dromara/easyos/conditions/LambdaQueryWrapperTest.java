package org.dromara.easyos.conditions;

import org.dromara.easyos.sql.BoundSql;
import org.dromara.easyos.sql.SqlRenderer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LambdaQueryWrapperTest {

    static class Doc {
        private String title;
        private Integer status;

        public String getTitle() {
            return title;
        }

        public Integer getStatus() {
            return status;
        }
    }

    @Test
    void wrapperRendersMatchAndEq() {
        LambdaQueryWrapper<Doc> w = new LambdaQueryWrapper<>();
        w.match(Doc::getTitle, "功夫").eq(Doc::getStatus, 1).orderByScoreDesc();
        BoundSql sql = w.render(new SqlRenderer(), "article");
        assertTrue(sql.getSql().contains("MATCH(title, ?)"));
        assertTrue(sql.getParams().contains("功夫"));
        assertTrue(sql.getParams().contains(1));
        assertTrue(sql.getSql().toUpperCase().contains("ORDER BY _SCORE DESC"));
    }
}
