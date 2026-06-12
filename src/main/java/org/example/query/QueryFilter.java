package org.example.query;

import java.util.HashMap;
import java.util.Map;

public class QueryFilter {

    private final Map<String, Object> conditions;

    private QueryFilter(Map<String, Object> conditions) {
        this.conditions = conditions;
    }

    public static QueryFilter of(String key, Object value) {
        Map<String, Object> conditions = new HashMap<>();
        conditions.put(key, value);
        return new QueryFilter(conditions);
    }

    public QueryFilter and(String key, Object value) {
        Map<String, Object> merged = new HashMap<>(this.conditions);
        merged.put(key, value);
        return new QueryFilter(merged);
    }

    public Map<String, Object> getConditions() {
        return conditions;
    }
}
