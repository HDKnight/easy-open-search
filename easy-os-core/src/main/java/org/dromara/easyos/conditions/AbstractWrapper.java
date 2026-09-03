package org.dromara.easyos.conditions;

import org.dromara.easyos.sql.BoundSql;
import org.dromara.easyos.sql.SqlRenderer;
import org.dromara.easyos.sql.ast.ConditionNode;
import org.dromara.easyos.sql.ast.OrderItem;
import org.dromara.easyos.sql.ast.QueryAst;
import org.dromara.easyos.sql.ast.SelectItem;
import org.dromara.easyos.toolkit.FieldUtils;
import org.dromara.easyos.toolkit.SFunction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@SuppressWarnings("unchecked")
public abstract class AbstractWrapper<T, Children extends AbstractWrapper<T, Children>>
        implements Wrapper<T, Children> {

    protected final Children typedThis = (Children) this;
    protected final List<ConditionNode> conditions = new ArrayList<>();
    protected final List<SelectItem> selectItems = new ArrayList<>();
    protected final List<String> groupByFields = new ArrayList<>();
    protected final List<OrderItem> orders = new ArrayList<>();
    protected String havingSql;
    protected Object[] havingParams;
    protected Integer limit;
    protected Integer offset;
    protected boolean includeScore;
    protected boolean mapUnderscoreToCamelCase = true;
    protected String lastSql;

    public Children setMapUnderscoreToCamelCase(boolean enabled) {
        this.mapUnderscoreToCamelCase = enabled;
        return typedThis;
    }

    protected String column(SFunction<T, ?> fn) {
        String name = FieldUtils.resolveFieldName(fn);
        return mapUnderscoreToCamelCase ? FieldUtils.camelToUnderline(name) : name;
    }

    protected Children add(ConditionNode node) {
        conditions.add(node);
        return typedThis;
    }

    @Override
    public Children eq(SFunction<T, ?> column, Object val) {
        return add(ConditionNode.eq(column(column), val));
    }

    @Override
    public Children ne(SFunction<T, ?> column, Object val) {
        return add(ConditionNode.ne(column(column), val));
    }

    @Override
    public Children gt(SFunction<T, ?> column, Object val) {
        return add(ConditionNode.gt(column(column), val));
    }

    @Override
    public Children ge(SFunction<T, ?> column, Object val) {
        return add(ConditionNode.ge(column(column), val));
    }

    @Override
    public Children lt(SFunction<T, ?> column, Object val) {
        return add(ConditionNode.lt(column(column), val));
    }

    @Override
    public Children le(SFunction<T, ?> column, Object val) {
        return add(ConditionNode.le(column(column), val));
    }

    @Override
    public Children like(SFunction<T, ?> column, Object val) {
        return add(ConditionNode.like(column(column), val));
    }

    @Override
    public Children in(SFunction<T, ?> column, Collection<?> values) {
        return add(ConditionNode.in(column(column), new ArrayList<>(values)));
    }

    @Override
    public Children between(SFunction<T, ?> column, Object from, Object to) {
        return add(ConditionNode.between(column(column), from, to));
    }

    @Override
    public Children isNull(SFunction<T, ?> column) {
        return add(ConditionNode.isNull(column(column)));
    }

    @Override
    public Children isNotNull(SFunction<T, ?> column) {
        return add(ConditionNode.isNotNull(column(column)));
    }

    @Override
    public Children and(Children other) {
        return add(ConditionNode.and(other.buildWhere()));
    }

    @Override
    public Children or(Children other) {
        return add(ConditionNode.or(other.buildWhere()));
    }

    @Override
    public Children match(SFunction<T, ?> column, String text) {
        return add(ConditionNode.match(column(column), text));
    }

    @Override
    public Children matchPhrase(SFunction<T, ?> column, String text) {
        return add(ConditionNode.matchPhrase(column(column), text));
    }

    @Override
    public Children multiMatch(String text, SFunction<T, ?>... columns) {
        String[] fields = Arrays.stream(columns).map(this::column).toArray(String[]::new);
        return add(ConditionNode.multiMatch(text, fields));
    }

    @Override
    public Children orderByAsc(SFunction<T, ?> column) {
        orders.add(OrderItem.asc(column(column)));
        return typedThis;
    }

    @Override
    public Children orderByDesc(SFunction<T, ?> column) {
        orders.add(OrderItem.desc(column(column)));
        return typedThis;
    }

    @Override
    public Children orderByScoreDesc() {
        includeScore = true;
        orders.add(OrderItem.scoreDesc());
        return typedThis;
    }

    @Override
    public Children orderByScoreAsc() {
        includeScore = true;
        orders.add(OrderItem.scoreAsc());
        return typedThis;
    }

    @Override
    public Children select(SFunction<T, ?>... columns) {
        for (SFunction<T, ?> c : columns) {
            selectItems.add(SelectItem.column(column(c)));
        }
        return typedThis;
    }

    @Override
    public Children selectCount(String alias) {
        selectItems.add(SelectItem.count(alias));
        return typedThis;
    }

    @Override
    public Children selectSum(SFunction<T, ?> column, String alias) {
        selectItems.add(SelectItem.sum(column(column), alias));
        return typedThis;
    }

    @Override
    public Children selectAvg(SFunction<T, ?> column, String alias) {
        selectItems.add(SelectItem.avg(column(column), alias));
        return typedThis;
    }

    @Override
    public Children selectMin(SFunction<T, ?> column, String alias) {
        selectItems.add(SelectItem.min(column(column), alias));
        return typedThis;
    }

    @Override
    public Children selectMax(SFunction<T, ?> column, String alias) {
        selectItems.add(SelectItem.max(column(column), alias));
        return typedThis;
    }

    @Override
    public Children groupBy(SFunction<T, ?>... columns) {
        for (SFunction<T, ?> c : columns) {
            groupByFields.add(column(c));
        }
        return typedThis;
    }

    @Override
    public Children having(String havingSql, Object... params) {
        this.havingSql = havingSql;
        this.havingParams = params;
        return typedThis;
    }

    @Override
    public Children last(String sqlSegment) {
        this.lastSql = sqlSegment;
        return typedThis;
    }

    @Override
    public Children limit(int limit) {
        this.limit = limit;
        return typedThis;
    }

    @Override
    public Children offset(int offset) {
        this.offset = offset;
        return typedThis;
    }

    protected ConditionNode buildWhere() {
        if (conditions.isEmpty()) {
            return null;
        }
        if (conditions.size() == 1) {
            return conditions.get(0);
        }
        return ConditionNode.and(conditions.toArray(new ConditionNode[0]));
    }

    @Override
    public QueryAst toAst(String index) {
        QueryAst ast = new QueryAst();
        ast.setIndex(index);
        for (SelectItem item : selectItems) {
            ast.addSelect(item);
        }
        ast.setWhereRoot(buildWhere());
        for (String g : groupByFields) {
            ast.addGroupBy(g);
        }
        if (havingSql != null) {
            ast.setHaving(havingSql, havingParams);
        }
        for (OrderItem order : orders) {
            ast.addOrder(order);
        }
        ast.setIncludeScore(includeScore);
        ast.setLimit(limit);
        ast.setOffset(offset);
        return ast;
    }

    @Override
    public BoundSql render(SqlRenderer renderer, String index) {
        BoundSql bound = renderer.render(toAst(index));
        if (lastSql == null || lastSql.trim().isEmpty()) {
            return bound;
        }
        // last() is for controlled appendices such as vendor hints; values must not come from user input.
        return new BoundSql(bound.getSql() + " " + lastSql, bound.getParams());
    }
}
