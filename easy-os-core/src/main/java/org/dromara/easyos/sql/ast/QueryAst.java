package org.dromara.easyos.sql.ast;

import java.util.ArrayList;
import java.util.List;

public class QueryAst {
    private String index;
    private final List<SelectItem> selectItems = new ArrayList<>();
    private ConditionNode whereRoot;
    private final List<String> groupBy = new ArrayList<>();
    private String havingSql;
    private final List<Object> havingParams = new ArrayList<>();
    private final List<OrderItem> orders = new ArrayList<>();
    private Integer limit;
    private Integer offset;
    private boolean includeScore;

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public List<SelectItem> getSelectItems() {
        return selectItems;
    }

    public void addSelect(SelectItem item) {
        this.selectItems.add(item);
    }

    public ConditionNode getWhereRoot() {
        return whereRoot;
    }

    public void setWhereRoot(ConditionNode whereRoot) {
        this.whereRoot = whereRoot;
    }

    public List<String> getGroupBy() {
        return groupBy;
    }

    public void addGroupBy(String field) {
        this.groupBy.add(field);
    }

    public String getHavingSql() {
        return havingSql;
    }

    public void setHaving(String havingSql, Object... params) {
        this.havingSql = havingSql;
        this.havingParams.clear();
        if (params != null) {
            CollectionsAddAll(params);
        }
    }

    private void CollectionsAddAll(Object[] params) {
        for (Object p : params) {
            this.havingParams.add(p);
        }
    }

    public List<Object> getHavingParams() {
        return havingParams;
    }

    public List<OrderItem> getOrders() {
        return orders;
    }

    public void addOrder(OrderItem order) {
        this.orders.add(order);
    }

    public void orderByScoreDesc() {
        this.includeScore = true;
        this.orders.add(OrderItem.scoreDesc());
    }

    public void orderByScoreAsc() {
        this.includeScore = true;
        this.orders.add(OrderItem.scoreAsc());
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public Integer getOffset() {
        return offset;
    }

    public void setOffset(Integer offset) {
        this.offset = offset;
    }

    public boolean isIncludeScore() {
        return includeScore;
    }

    public void setIncludeScore(boolean includeScore) {
        this.includeScore = includeScore;
    }
}
