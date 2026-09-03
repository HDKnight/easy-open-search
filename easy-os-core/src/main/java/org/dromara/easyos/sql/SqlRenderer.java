package org.dromara.easyos.sql;

import org.dromara.easyos.exception.EasyOsException;
import org.dromara.easyos.sql.ast.ConditionNode;
import org.dromara.easyos.sql.ast.OrderItem;
import org.dromara.easyos.sql.ast.QueryAst;
import org.dromara.easyos.sql.ast.SelectItem;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SqlRenderer {

    public BoundSql render(QueryAst ast) {
        if (ast == null || ast.getIndex() == null || ast.getIndex().trim().isEmpty()) {
            throw new EasyOsException("QueryAst.index must not be blank");
        }
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT ");
        sql.append(renderSelect(ast));
        sql.append(" FROM ").append(ast.getIndex());

        if (ast.getWhereRoot() != null) {
            StringBuilder where = new StringBuilder();
            renderCondition(ast.getWhereRoot(), where, params);
            sql.append(" WHERE ").append(where);
        }

        if (!ast.getGroupBy().isEmpty()) {
            sql.append(" GROUP BY ").append(String.join(", ", ast.getGroupBy()));
        }

        if (ast.getHavingSql() != null && !ast.getHavingSql().trim().isEmpty()) {
            sql.append(" HAVING ").append(ast.getHavingSql());
            params.addAll(ast.getHavingParams());
        }

        if (!ast.getOrders().isEmpty()) {
            sql.append(" ORDER BY ");
            sql.append(ast.getOrders().stream().map(OrderItem::toSqlFragment).collect(Collectors.joining(", ")));
        }

        if (ast.getLimit() != null) {
            sql.append(" LIMIT ?");
            params.add(ast.getLimit());
            if (ast.getOffset() != null) {
                sql.append(" OFFSET ?");
                params.add(ast.getOffset());
            }
        } else if (ast.getOffset() != null) {
            throw new EasyOsException("OFFSET requires LIMIT");
        }

        return new BoundSql(sql.toString(), params);
    }

    private String renderSelect(QueryAst ast) {
        List<SelectItem> items = new ArrayList<>(ast.getSelectItems());
        if (items.isEmpty()) {
            if (ast.isIncludeScore()) {
                return "*, _score";
            }
            return "*";
        }
        boolean hasScore = items.stream().anyMatch(i -> i.getKind() == SelectItem.Kind.SCORE);
        if (ast.isIncludeScore() && !hasScore) {
            items.add(SelectItem.score());
        }
        return items.stream().map(SelectItem::toSqlFragment).collect(Collectors.joining(", "));
    }

    private void renderCondition(ConditionNode node, StringBuilder sql, List<Object> params) {
        switch (node.getOp()) {
            case EQ:
                sql.append(node.getField()).append(" = ?");
                params.add(node.getValue());
                break;
            case NE:
                sql.append(node.getField()).append(" <> ?");
                params.add(node.getValue());
                break;
            case GT:
                sql.append(node.getField()).append(" > ?");
                params.add(node.getValue());
                break;
            case GE:
                sql.append(node.getField()).append(" >= ?");
                params.add(node.getValue());
                break;
            case LT:
                sql.append(node.getField()).append(" < ?");
                params.add(node.getValue());
                break;
            case LE:
                sql.append(node.getField()).append(" <= ?");
                params.add(node.getValue());
                break;
            case LIKE:
                sql.append(node.getField()).append(" LIKE ?");
                params.add(wrapLike(node.getValue()));
                break;
            case IN:
                @SuppressWarnings("unchecked")
                List<Object> values = (List<Object>) node.getValue();
                if (values == null || values.isEmpty()) {
                    throw new EasyOsException("IN values must not be empty");
                }
                sql.append(node.getField()).append(" IN (");
                for (int i = 0; i < values.size(); i++) {
                    if (i > 0) {
                        sql.append(", ");
                    }
                    sql.append("?");
                    params.add(values.get(i));
                }
                sql.append(")");
                break;
            case BETWEEN:
                sql.append(node.getField()).append(" BETWEEN ? AND ?");
                params.add(node.getValue());
                params.add(node.getValue2());
                break;
            case IS_NULL:
                sql.append(node.getField()).append(" IS NULL");
                break;
            case IS_NOT_NULL:
                sql.append(node.getField()).append(" IS NOT NULL");
                break;
            case MATCH:
                sql.append("MATCH(").append(node.getField()).append(", ?)");
                params.add(node.getValue());
                break;
            case MATCH_PHRASE:
                sql.append("MATCH_PHRASE(").append(node.getField()).append(", ?)");
                params.add(node.getValue());
                break;
            case MULTI_MATCH:
                sql.append("MULTI_MATCH('query'=?, 'fields'='")
                        .append(String.join(",", node.getFields()))
                        .append("')");
                params.add(node.getValue());
                break;
            case SCORE_MATCH:
                sql.append("SCORE(MATCH(").append(node.getField()).append(", ?), ?)");
                params.add(node.getValue());
                params.add(node.getBoost());
                break;
            case AND:
                renderComposite(node, " AND ", sql, params);
                break;
            case OR:
                renderComposite(node, " OR ", sql, params);
                break;
            case NOT:
                sql.append("NOT (");
                renderCondition(node.getChildren().get(0), sql, params);
                sql.append(")");
                break;
            default:
                throw new EasyOsException("Unsupported condition: " + node.getOp());
        }
    }

    private void renderComposite(ConditionNode node, String sep, StringBuilder sql, List<Object> params) {
        List<ConditionNode> children = node.getChildren();
        if (children == null || children.isEmpty()) {
            throw new EasyOsException("Composite condition requires children");
        }
        sql.append("(");
        for (int i = 0; i < children.size(); i++) {
            if (i > 0) {
                sql.append(sep);
            }
            renderCondition(children.get(i), sql, params);
        }
        sql.append(")");
    }

    private Object wrapLike(Object value) {
        if (value == null) {
            return null;
        }
        String s = String.valueOf(value);
        if (s.contains("%") || s.contains("_")) {
            return s;
        }
        return "%" + s + "%";
    }
}
