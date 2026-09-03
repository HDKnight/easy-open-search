package org.dromara.easyos.mapper;

import org.dromara.easyos.annotation.OsSelect;
import org.dromara.easyos.annotation.Param;
import org.dromara.easyos.exception.EasyOsException;
import org.dromara.easyos.jdbc.JdbcExecutor;
import org.dromara.easyos.jdbc.ResultSetMapper;
import org.dromara.easyos.property.EasyOsProperties;
import org.dromara.easyos.sql.BoundSql;
import org.dromara.easyos.sql.NamedSqlParser;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 执行 Mapper 方法上的 {@link OsSelect} 自定义 SQL。
 */
public class OsSelectInvoker {
    private final Class<?> entityClass;
    private final EasyOsProperties properties;
    private final JdbcExecutor jdbcExecutor;

    public OsSelectInvoker(Class<?> entityClass, EasyOsProperties properties, JdbcExecutor jdbcExecutor) {
        this.entityClass = entityClass;
        this.properties = properties;
        this.jdbcExecutor = jdbcExecutor;
    }

    public Object invoke(Method method, Object[] args) {
        OsSelect osSelect = method.getAnnotation(OsSelect.class);
        if (osSelect == null) {
            throw new EasyOsException("Missing @OsSelect on " + method);
        }
        NamedSqlParser.ParsedSql parsed = NamedSqlParser.parse(osSelect.value());
        Map<String, Object> paramMap = buildParamMap(method, args);
        List<Object> values = new ArrayList<>(parsed.getParamNames().size());
        for (String name : parsed.getParamNames()) {
            values.add(resolveParam(paramMap, name, args));
        }
        BoundSql boundSql = new BoundSql(parsed.getJdbcSql(), values);
        List<Map<String, Object>> rows = jdbcExecutor.queryMaps(boundSql);
        return convertResult(method, rows);
    }

    private Map<String, Object> buildParamMap(Method method, Object[] args) {
        Map<String, Object> map = new HashMap<>();
        if (args == null || args.length == 0) {
            return map;
        }
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            Object value = args[i];
            map.put("param" + (i + 1), value);
            map.put("arg" + i, value);
            Param param = parameters[i].getAnnotation(Param.class);
            if (param != null && param.value() != null && !param.value().isEmpty()) {
                map.put(param.value(), value);
            } else if (parameters[i].isNamePresent()) {
                map.put(parameters[i].getName(), value);
            }
        }
        return map;
    }

    private Object resolveParam(Map<String, Object> paramMap, String name, Object[] args) {
        if (paramMap.containsKey(name)) {
            return paramMap.get(name);
        }
        // 单参数且未匹配到名字时，回退到该唯一实参（便于 #{id} + 单参）
        if (args != null && args.length == 1) {
            return args[0];
        }
        throw new EasyOsException("Cannot resolve #{" + name + "} for @OsSelect; add @Param(\"" + name + "\")");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object convertResult(Method method, List<Map<String, Object>> rows) {
        Class<?> returnClass = method.getReturnType();
        Type generic = method.getGenericReturnType();
        boolean mapUnderscore = properties.getGlobalConfig().getDbConfig().isMapUnderscoreToCamelCase();

        if (void.class.equals(returnClass) || Void.class.equals(returnClass)) {
            return null;
        }
        if (List.class.isAssignableFrom(returnClass)) {
            Class<?> elem = Object.class;
            if (generic instanceof ParameterizedType) {
                Type arg = ((ParameterizedType) generic).getActualTypeArguments()[0];
                if (arg instanceof Class) {
                    elem = (Class<?>) arg;
                } else if (arg instanceof ParameterizedType) {
                    // List<Map<...>>
                    Type raw = ((ParameterizedType) arg).getRawType();
                    if (raw == Map.class) {
                        return rows;
                    }
                }
            }
            if (Map.class.isAssignableFrom(elem)) {
                return rows;
            }
            if (elem == Object.class) {
                elem = entityClass;
            }
            return ResultSetMapper.mapsToEntities(rows, elem, mapUnderscore);
        }
        if (Map.class.isAssignableFrom(returnClass)) {
            return rows.isEmpty() ? null : rows.get(0);
        }
        if (isSimpleNumber(returnClass)) {
            return toSimpleNumber(rows, returnClass);
        }
        if (String.class.equals(returnClass)) {
            if (rows.isEmpty()) {
                return null;
            }
            Object first = rows.get(0).values().iterator().next();
            return first == null ? null : String.valueOf(first);
        }
        // 单实体
        List<?> list = ResultSetMapper.mapsToEntities(rows, returnClass, mapUnderscore);
        return list.isEmpty() ? null : list.get(0);
    }

    private static boolean isSimpleNumber(Class<?> type) {
        return type == Long.class || type == long.class
                || type == Integer.class || type == int.class
                || type == Double.class || type == double.class
                || type == Float.class || type == float.class;
    }

    private static Object toSimpleNumber(List<Map<String, Object>> rows, Class<?> type) {
        if (rows.isEmpty()) {
            if (type.isPrimitive()) {
                return type == long.class ? 0L
                        : type == int.class ? 0
                        : type == double.class ? 0D
                        : 0F;
            }
            return null;
        }
        Object first = rows.get(0).values().iterator().next();
        if (first == null) {
            return type.isPrimitive() ? 0 : null;
        }
        Number n = first instanceof Number ? (Number) first : Double.parseDouble(String.valueOf(first));
        if (type == Long.class || type == long.class) {
            return n.longValue();
        }
        if (type == Integer.class || type == int.class) {
            return n.intValue();
        }
        if (type == Double.class || type == double.class) {
            return n.doubleValue();
        }
        if (type == Float.class || type == float.class) {
            return n.floatValue();
        }
        return n;
    }
}
