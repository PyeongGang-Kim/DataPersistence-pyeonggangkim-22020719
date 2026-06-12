package org.example.query;

import java.util.HashMap;
import java.util.Map;

public class QueryFilter {

    private final Map<String, Object> conditions;

    private QueryFilter(Map<String, Object> conditions) {
        this.conditions = conditions;
    }

    public static QueryFilter of(String key, Object value) {
        // TODO: RED - 구현 전 스텁
        return null;
    }

    public QueryFilter and(String key, Object value) {
        // TODO: RED - 구현 전 스텁
        return null;
    }

    public Map<String, Object> getConditions() {
        return conditions;
    }
}
