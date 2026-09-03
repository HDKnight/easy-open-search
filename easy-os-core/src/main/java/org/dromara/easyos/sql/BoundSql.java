package org.dromara.easyos.sql;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BoundSql {
    private final String sql;
    private final List<Object> params;

    public BoundSql(String sql, List<Object> params) {
        this.sql = sql;
        this.params = params == null ? Collections.emptyList() : new ArrayList<>(params);
    }

    public String getSql() {
        return sql;
    }

    public List<Object> getParams() {
        return params;
    }
}
