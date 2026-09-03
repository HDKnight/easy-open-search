package org.dromara.easyos.sql.ast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ConditionNode {
    public enum Op {
        EQ, NE, GT, GE, LT, LE, LIKE, IN, BETWEEN, IS_NULL, IS_NOT_NULL,
        MATCH, MATCH_PHRASE, MULTI_MATCH, SCORE_MATCH,
        AND, OR, NOT
    }

    private final Op op;
    private final String field;
    private final Object value;
    private final Object value2;
    private final List<String> fields;
    private final List<ConditionNode> children;
    private final Double boost;

    private ConditionNode(Op op, String field, Object value, Object value2,
                          List<String> fields, List<ConditionNode> children, Double boost) {
        this.op = op;
        this.field = field;
        this.value = value;
        this.value2 = value2;
        this.fields = fields;
        this.children = children;
        this.boost = boost;
    }

    public static ConditionNode eq(String field, Object value) {
        return new ConditionNode(Op.EQ, field, value, null, null, null, null);
    }

    public static ConditionNode ne(String field, Object value) {
        return new ConditionNode(Op.NE, field, value, null, null, null, null);
    }

    public static ConditionNode gt(String field, Object value) {
        return new ConditionNode(Op.GT, field, value, null, null, null, null);
    }

    public static ConditionNode ge(String field, Object value) {
        return new ConditionNode(Op.GE, field, value, null, null, null, null);
    }

    public static ConditionNode lt(String field, Object value) {
        return new ConditionNode(Op.LT, field, value, null, null, null, null);
    }

    public static ConditionNode le(String field, Object value) {
        return new ConditionNode(Op.LE, field, value, null, null, null, null);
    }

    public static ConditionNode like(String field, Object value) {
        return new ConditionNode(Op.LIKE, field, value, null, null, null, null);
    }

    public static ConditionNode in(String field, List<?> values) {
        return new ConditionNode(Op.IN, field, values, null, null, null, null);
    }

    public static ConditionNode between(String field, Object from, Object to) {
        return new ConditionNode(Op.BETWEEN, field, from, to, null, null, null);
    }

    public static ConditionNode isNull(String field) {
        return new ConditionNode(Op.IS_NULL, field, null, null, null, null, null);
    }

    public static ConditionNode isNotNull(String field) {
        return new ConditionNode(Op.IS_NOT_NULL, field, null, null, null, null, null);
    }

    public static ConditionNode match(String field, String text) {
        return new ConditionNode(Op.MATCH, field, text, null, null, null, null);
    }

    public static ConditionNode matchPhrase(String field, String text) {
        return new ConditionNode(Op.MATCH_PHRASE, field, text, null, null, null, null);
    }

    public static ConditionNode multiMatch(String text, String... fields) {
        return new ConditionNode(Op.MULTI_MATCH, null, text, null, Arrays.asList(fields), null, null);
    }

    public static ConditionNode scoreMatch(String field, String text, double boost) {
        return new ConditionNode(Op.SCORE_MATCH, field, text, null, null, null, boost);
    }

    public static ConditionNode and(ConditionNode... nodes) {
        return new ConditionNode(Op.AND, null, null, null, null, Arrays.asList(nodes), null);
    }

    public static ConditionNode or(ConditionNode... nodes) {
        return new ConditionNode(Op.OR, null, null, null, null, Arrays.asList(nodes), null);
    }

    public static ConditionNode not(ConditionNode node) {
        return new ConditionNode(Op.NOT, null, null, null, null, Collections.singletonList(node), null);
    }

    public Op getOp() {
        return op;
    }

    public String getField() {
        return field;
    }

    public Object getValue() {
        return value;
    }

    public Object getValue2() {
        return value2;
    }

    public List<String> getFields() {
        return fields;
    }

    public List<ConditionNode> getChildren() {
        return children;
    }

    public Double getBoost() {
        return boost;
    }
}
