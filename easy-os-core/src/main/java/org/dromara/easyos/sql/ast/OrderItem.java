package org.dromara.easyos.sql.ast;

public class OrderItem {
    private final String expression;
    private final boolean ascending;

    public OrderItem(String expression, boolean ascending) {
        this.expression = expression;
        this.ascending = ascending;
    }

    public static OrderItem asc(String field) {
        return new OrderItem(field, true);
    }

    public static OrderItem desc(String field) {
        return new OrderItem(field, false);
    }

    public static OrderItem scoreDesc() {
        return new OrderItem("_score", false);
    }

    public static OrderItem scoreAsc() {
        return new OrderItem("_score", true);
    }

    public String getExpression() {
        return expression;
    }

    public boolean isAscending() {
        return ascending;
    }

    public String toSqlFragment() {
        return expression + (ascending ? " ASC" : " DESC");
    }
}
