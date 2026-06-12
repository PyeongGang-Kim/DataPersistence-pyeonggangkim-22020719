package org.example.model;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Record {

    private final String id;
    private final Map<String, Object> fields;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Record(String id, Map<String, Object> fields, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.fields = fields;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Record of(Map<String, Object> fields) {
        LocalDateTime now = LocalDateTime.now();
        return new Record(UUID.randomUUID().toString(), new HashMap<>(fields), now, now);
    }

    public static Record restore(String id, Map<String, Object> fields, LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new Record(id, new HashMap<>(fields), createdAt, updatedAt);
    }

    public String getId() { return id; }
    public Map<String, Object> getFields() { return fields; }
    public Object getField(String key) { return fields.get(key); }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
