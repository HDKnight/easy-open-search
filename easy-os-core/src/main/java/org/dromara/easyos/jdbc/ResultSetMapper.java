package org.dromara.easyos.jdbc;

import org.dromara.easyos.annotation.Score;
import org.dromara.easyos.exception.EasyOsException;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ResultSetMapper {
    private ResultSetMapper() {
    }

    public static List<Map<String, Object>> toMaps(ResultSet rs) throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        ResultSetMetaData meta = rs.getMetaData();
        int count = meta.getColumnCount();
        while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= count; i++) {
                String label = meta.getColumnLabel(i);
                row.put(label, rs.getObject(i));
            }
            list.add(row);
        }
        return list;
    }

    public static <T> List<T> toEntities(ResultSet rs, Class<T> clazz, boolean mapUnderscoreToCamelCase)
            throws SQLException {
        List<Map<String, Object>> maps = toMaps(rs);
        List<T> result = new ArrayList<>(maps.size());
        for (Map<String, Object> map : maps) {
            result.add(mapToEntity(map, clazz, mapUnderscoreToCamelCase));
        }
        return result;
    }

    public static <T> List<T> mapsToEntities(List<Map<String, Object>> maps, Class<T> clazz,
                                             boolean mapUnderscoreToCamelCase) {
        List<T> result = new ArrayList<>(maps.size());
        for (Map<String, Object> map : maps) {
            result.add(mapToEntity(map, clazz, mapUnderscoreToCamelCase));
        }
        return result;
    }

    private static <T> T mapToEntity(Map<String, Object> map, Class<T> clazz, boolean mapUnderscoreToCamelCase) {
        try {
            T instance = clazz.getDeclaredConstructor().newInstance();
            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                Object value = null;
                if (field.getAnnotation(Score.class) != null) {
                    value = firstPresent(map, "_score", "score");
                } else {
                    value = firstPresent(map, field.getName(),
                            mapUnderscoreToCamelCase ? camelToUnderline(field.getName()) : null);
                }
                if (value != null) {
                    field.set(instance, convert(value, field.getType()));
                }
            }
            return instance;
        } catch (ReflectiveOperationException e) {
            throw new EasyOsException("Failed to map ResultSet to " + clazz.getName(), e);
        }
    }

    private static Object firstPresent(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            if (key == null) {
                continue;
            }
            if (map.containsKey(key)) {
                return map.get(key);
            }
            for (Map.Entry<String, Object> e : map.entrySet()) {
                if (e.getKey() != null && e.getKey().equalsIgnoreCase(key)) {
                    return e.getValue();
                }
            }
        }
        return null;
    }

    private static String camelToUnderline(String name) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c)) {
                sb.append('_').append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static Object convert(Object value, Class<?> target) {
        if (value == null || target.isInstance(value)) {
            return value;
        }
        if (target == String.class) {
            return String.valueOf(value);
        }
        if (target == Integer.class || target == int.class) {
            return value instanceof Number ? ((Number) value).intValue() : Integer.parseInt(String.valueOf(value));
        }
        if (target == Long.class || target == long.class) {
            return value instanceof Number ? ((Number) value).longValue() : Long.parseLong(String.valueOf(value));
        }
        if (target == Double.class || target == double.class) {
            return value instanceof Number ? ((Number) value).doubleValue() : Double.parseDouble(String.valueOf(value));
        }
        if (target == Float.class || target == float.class) {
            return value instanceof Number ? ((Number) value).floatValue() : Float.parseFloat(String.valueOf(value));
        }
        if (target == Boolean.class || target == boolean.class) {
            return value instanceof Boolean ? value : Boolean.parseBoolean(String.valueOf(value));
        }
        if (target == BigDecimal.class) {
            return value instanceof BigDecimal ? value : new BigDecimal(String.valueOf(value));
        }
        return value;
    }
}
