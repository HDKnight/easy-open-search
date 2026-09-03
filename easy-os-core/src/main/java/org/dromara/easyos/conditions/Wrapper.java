package org.dromara.easyos.conditions;

import org.dromara.easyos.sql.BoundSql;
import org.dromara.easyos.sql.SqlRenderer;
import org.dromara.easyos.sql.ast.QueryAst;
import org.dromara.easyos.toolkit.SFunction;

import java.util.Collection;

public interface Wrapper<T, Children extends Wrapper<T, Children>> {

    Children eq(SFunction<T, ?> column, Object val);

    Children ne(SFunction<T, ?> column, Object val);

    Children gt(SFunction<T, ?> column, Object val);

    Children ge(SFunction<T, ?> column, Object val);

    Children lt(SFunction<T, ?> column, Object val);

    Children le(SFunction<T, ?> column, Object val);

    Children like(SFunction<T, ?> column, Object val);

    Children in(SFunction<T, ?> column, Collection<?> values);

    Children between(SFunction<T, ?> column, Object from, Object to);

    Children isNull(SFunction<T, ?> column);

    Children isNotNull(SFunction<T, ?> column);

    Children and(Children other);

    Children or(Children other);

    Children match(SFunction<T, ?> column, String text);

    Children matchPhrase(SFunction<T, ?> column, String text);

    Children multiMatch(String text, SFunction<T, ?>... columns);

    Children orderByAsc(SFunction<T, ?> column);

    Children orderByDesc(SFunction<T, ?> column);

    Children orderByScoreDesc();

    Children orderByScoreAsc();

    Children select(SFunction<T, ?>... columns);

    Children selectCount(String alias);

    Children selectSum(SFunction<T, ?> column, String alias);

    Children selectAvg(SFunction<T, ?> column, String alias);

    Children selectMin(SFunction<T, ?> column, String alias);

    Children selectMax(SFunction<T, ?> column, String alias);

    Children groupBy(SFunction<T, ?>... columns);

    Children having(String havingSql, Object... params);

    Children last(String sqlSegment);

    Children limit(int limit);

    Children offset(int offset);

    QueryAst toAst(String index);

    BoundSql render(SqlRenderer renderer, String index);
}
