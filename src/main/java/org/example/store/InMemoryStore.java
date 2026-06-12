package org.example.store;

import org.example.model.Record;
import org.example.query.QueryFilter;

import java.time.LocalDateTime;
import java.util.*;

public class InMemoryStore implements DataStore {

    private final Map<String, Record> storage = new LinkedHashMap<>();

    @Override
    public Record save(Record record) {
        storage.put(record.getId(), record);
        return record;
    }

    @Override
    public Optional<Record> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<Record> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public List<Record> query(QueryFilter filter) {
        return storage.values().stream()
                .filter(record -> filter.getConditions().entrySet().stream()
                        .allMatch(entry -> entry.getValue().equals(record.getField(entry.getKey()))))
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public Record update(String id, Map<String, Object> fields) {
        Record record = storage.get(id);
        if (record == null) {
            throw new IllegalArgumentException("Record not found: " + id);
        }
        record.getFields().putAll(fields);
        record.setUpdatedAt(LocalDateTime.now());
        return record;
    }

    @Override
    public void delete(String id) {
        if (!storage.containsKey(id)) {
            throw new IllegalArgumentException("Record not found: " + id);
        }
        storage.remove(id);
    }
}
