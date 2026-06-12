package org.example.store;

import org.example.model.Record;
import org.example.query.QueryFilter;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface DataStore {
    Record save(Record record);
    Optional<Record> findById(String id);
    List<Record> findAll();
    List<Record> query(QueryFilter filter);
    Record update(String id, Map<String, Object> fields);
    void delete(String id);
}
