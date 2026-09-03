package org.dromara.easyos.sql;

import org.dromara.easyos.sql.ast.ConditionNode;
import org.dromara.easyos.sql.ast.OrderItem;
import org.dromara.easyos.sql.ast.QueryAst;
import org.dromara.easyos.sql.ast.SelectItem;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlRendererTest {

    private final SqlRenderer renderer = new SqlRenderer();

    private static String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }

    @Test
    void renderEqAndLimitUsesPlaceholders() {
        QueryAst ast = new QueryAst();
        ast.setIndex("article");
        ast.setWhereRoot(ConditionNode.eq("status", 1));
        ast.setLimit(10);
        ast.setOffset(20);
        BoundSql bound = renderer.render(ast);
        assertEquals(
                "SELECT * FROM article WHERE status = ? LIMIT ? OFFSET ?",
                normalize(bound.getSql()));
        assertEquals(Arrays.asList(1, 10, 20), bound.getParams());
    }

    @Test
    void likeWrapsPercentInParamNotSql() {
        QueryAst ast = new QueryAst();
        ast.setIndex("article");
        ast.setWhereRoot(ConditionNode.like("title", "功夫"));
        BoundSql bound = renderer.render(ast);
        assertTrue(bound.getSql().contains("title LIKE ?"));
        assertEquals("%功夫%", bound.getParams().get(0));
    }

    @Test
    void inExpandsPlaceholders() {
        QueryAst ast = new QueryAst();
        ast.setIndex("article");
        ast.setWhereRoot(ConditionNode.in("id", Arrays.asList("a", "b")));
        BoundSql bound = renderer.render(ast);
        assertEquals("SELECT * FROM article WHERE id IN (?, ?)", normalize(bound.getSql()));
        assertEquals(Arrays.asList("a", "b"), bound.getParams());
    }

    @Test
    void orNestingUsesParentheses() {
        QueryAst ast = new QueryAst();
        ast.setIndex("article");
        ast.setWhereRoot(ConditionNode.or(
                ConditionNode.eq("status", 1),
                ConditionNode.eq("status", 2)));
        BoundSql bound = renderer.render(ast);
        assertEquals("SELECT * FROM article WHERE (status = ? OR status = ?)", normalize(bound.getSql()));
    }

    @Test
    void orderByField() {
        QueryAst ast = new QueryAst();
        ast.setIndex("article");
        ast.addOrder(OrderItem.desc("id"));
        BoundSql bound = renderer.render(ast);
        assertEquals("SELECT * FROM article ORDER BY id DESC", normalize(bound.getSql()));
    }

    @Test
    void matchAndOrderByScore() {
        QueryAst ast = new QueryAst();
        ast.setIndex("article");
        ast.setIncludeScore(true);
        ast.setWhereRoot(ConditionNode.match("title", "功夫"));
        ast.orderByScoreDesc();
        BoundSql bound = renderer.render(ast);
        String sql = normalize(bound.getSql()).toUpperCase();
        assertTrue(sql.contains("_SCORE"));
        assertTrue(bound.getSql().contains("MATCH(title, ?)"));
        assertTrue(sql.contains("ORDER BY _SCORE DESC"));
        assertEquals(Collections.singletonList("功夫"), bound.getParams());
    }

    @Test
    void groupByWithCountSum() {
        QueryAst ast = new QueryAst();
        ast.setIndex("article");
        ast.addSelect(SelectItem.column("title"));
        ast.addSelect(SelectItem.count("cnt"));
        ast.addSelect(SelectItem.sum("star_num", "sumStar"));
        ast.addGroupBy("title");
        ast.setWhereRoot(ConditionNode.eq("status", 1));
        BoundSql bound = renderer.render(ast);
        String sql = normalize(bound.getSql());
        assertTrue(sql.contains("COUNT(*) AS cnt"));
        assertTrue(sql.contains("SUM(star_num) AS sumStar"));
        assertTrue(sql.contains("GROUP BY title"));
        assertEquals(Collections.singletonList(1), bound.getParams());
    }
}
