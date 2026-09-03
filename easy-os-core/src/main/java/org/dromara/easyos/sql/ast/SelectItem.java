package org.dromara.easyos.sql.ast;

import java.util.ArrayList;
import java.util.List;

public class SelectItem {
    public enum Kind {
        COLUMN, COUNT, SUM, AVG, MIN, MAX, SCORE
    }

    private final Kind kind;
    private final String field;
    private final String alias;

    private SelectItem(Kind kind, String field, String alias) {
        this.kind = kind;
        this.field = field;
        this.alias = alias;
    }

    public static SelectItem column(String field) {
        return new SelectItem(Kind.COLUMN, field, null);
    }

    public static SelectItem column(String field, String alias) {
        return new SelectItem(Kind.COLUMN, field, alias);
    }

    public static SelectItem count(String alias) {
        return new SelectItem(Kind.COUNT, null, alias);
    }

    public static SelectItem sum(String field, String alias) {
        return new SelectItem(Kind.SUM, field, alias);
    }

    public static SelectItem avg(String field, String alias) {
        return new SelectItem(Kind.AVG, field, alias);
    }

    public static SelectItem min(String field, String alias) {
        return new SelectItem(Kind.MIN, field, alias);
    }

    public static SelectItem max(String field, String alias) {
        return new SelectItem(Kind.MAX, field, alias);
    }

    public static SelectItem score() {
        return new SelectItem(Kind.SCORE, "_score", "_score");
    }

    public Kind getKind() {
        return kind;
    }

    public String getField() {
        return field;
    }

    public String getAlias() {
        return alias;
    }

    public String toSqlFragment() {
        switch (kind) {
            case COLUMN:
                return alias == null ? field : field + " AS " + alias;
            case COUNT:
                return "COUNT(*) AS " + alias;
            case SUM:
                return "SUM(" + field + ") AS " + alias;
            case AVG:
                return "AVG(" + field + ") AS " + alias;
            case MIN:
                return "MIN(" + field + ") AS " + alias;
            case MAX:
                return "MAX(" + field + ") AS " + alias;
            case SCORE:
                return "_score";
            default:
                throw new IllegalStateException("Unknown select kind: " + kind);
        }
    }
}
