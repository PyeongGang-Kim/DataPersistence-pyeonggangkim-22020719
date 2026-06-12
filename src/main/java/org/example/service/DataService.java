package org.example.service;

import org.example.model.Record;
import org.example.query.QueryFilter;
import org.example.store.DataStore;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DataService {

    private final DataStore store;

    public DataService(DataStore store) {
        this.store = store;
    }

    public Record create(Map<String, Object> fields) {
        return store.save(Record.of(fields));
    }

    public List<Record> findAll() {
        return store.findAll();
    }

    public Optional<Record> findById(String id) {
        return store.findById(id);
    }

    public List<Record> query(QueryFilter filter) {
        return store.query(filter);
    }

    public Record update(String id, Map<String, Object> fields) {
        return store.update(id, fields);
    }

    public void delete(String id) {
        store.delete(id);
    }
}
