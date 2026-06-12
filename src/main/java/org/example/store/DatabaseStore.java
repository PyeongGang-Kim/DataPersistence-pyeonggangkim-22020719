package org.example.store;

import org.example.db.SchemaInitializer;
import org.example.model.Record;
import org.example.query.QueryFilter;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class DatabaseStore implements DataStore {

    private final String jdbcUrl;
    private final String user;
    private final String password;

    public DatabaseStore(String jdbcUrl, String user, String password) {
        this.jdbcUrl = jdbcUrl;
        this.user = user;
        this.password = password;
        try (Connection conn = getConnection()) {
            SchemaInitializer.initialize(conn);
        } catch (SQLException e) {
            throw new RuntimeException("스키마 초기화 실패", e);
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, user, password);
    }

    @Override
    public Record save(Record record) {
        String insertRecord = "INSERT INTO records (id, created_at, updated_at) VALUES (?, ?, ?)";
        String insertField  = "INSERT INTO record_fields (record_id, field_key, field_value) VALUES (?, ?, ?)";
        try (Connection conn = getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(insertRecord)) {
                ps.setString(1, record.getId());
                ps.setTimestamp(2, Timestamp.valueOf(record.getCreatedAt()));
                ps.setTimestamp(3, Timestamp.valueOf(record.getUpdatedAt()));
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(insertField)) {
                for (Map.Entry<String, Object> entry : record.getFields().entrySet()) {
                    ps.setString(1, record.getId());
                    ps.setString(2, entry.getKey());
                    ps.setString(3, entry.getValue() != null ? entry.getValue().toString() : null);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return record;
    }

    @Override
    public Optional<Record> findById(String id) {
        String sql = """
                SELECT r.id, r.created_at, r.updated_at, f.field_key, f.field_value
                FROM records r
                LEFT JOIN record_fields f ON r.id = f.record_id
                WHERE r.id = ?
                """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return buildRecords(rs).stream().findFirst();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Record> findAll() {
        String sql = """
                SELECT r.id, r.created_at, r.updated_at, f.field_key, f.field_value
                FROM records r
                LEFT JOIN record_fields f ON r.id = f.record_id
                ORDER BY r.created_at
                """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return buildRecords(rs);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Record> query(QueryFilter filter) {
        return findAll().stream()
                .filter(record -> filter.getConditions().entrySet().stream()
                        .allMatch(entry -> {
                            Object fieldValue = record.getField(entry.getKey());
                            if (fieldValue == null) return false;
                            return entry.getValue().toString().equals(fieldValue.toString());
                        }))
                .collect(Collectors.toList());
    }

    @Override
    public Record update(String id, Map<String, Object> fields) {
        if (findById(id).isEmpty()) {
            throw new IllegalArgumentException("Record not found: " + id);
        }
        LocalDateTime now = LocalDateTime.now();
        String deleteFields = "DELETE FROM record_fields WHERE record_id = ?";
        String insertField  = "INSERT INTO record_fields (record_id, field_key, field_value) VALUES (?, ?, ?)";
        String updateRecord = "UPDATE records SET updated_at = ? WHERE id = ?";
        try (Connection conn = getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(deleteFields)) {
                ps.setString(1, id);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(insertField)) {
                for (Map.Entry<String, Object> entry : fields.entrySet()) {
                    ps.setString(1, id);
                    ps.setString(2, entry.getKey());
                    ps.setString(3, entry.getValue() != null ? entry.getValue().toString() : null);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            try (PreparedStatement ps = conn.prepareStatement(updateRecord)) {
                ps.setTimestamp(1, Timestamp.valueOf(now));
                ps.setString(2, id);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return findById(id).orElseThrow();
    }

    @Override
    public void delete(String id) {
        if (findById(id).isEmpty()) {
            throw new IllegalArgumentException("Record not found: " + id);
        }
        String sql = "DELETE FROM records WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private List<Record> buildRecords(ResultSet rs) throws SQLException {
        Map<String, Map<String, Object>> fieldsMap = new LinkedHashMap<>();
        Map<String, LocalDateTime[]> timestamps = new LinkedHashMap<>();
        while (rs.next()) {
            String id = rs.getString("id");
            if (!fieldsMap.containsKey(id)) {
                fieldsMap.put(id, new HashMap<>());
                timestamps.put(id, new LocalDateTime[]{
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getTimestamp("updated_at").toLocalDateTime()
                });
            }
            String key   = rs.getString("field_key");
            String value = rs.getString("field_value");
            if (key != null) {
                fieldsMap.get(id).put(key, value);
            }
        }
        List<Record> records = new ArrayList<>();
        for (Map.Entry<String, Map<String, Object>> entry : fieldsMap.entrySet()) {
            LocalDateTime[] ts = timestamps.get(entry.getKey());
            records.add(Record.restore(entry.getKey(), entry.getValue(), ts[0], ts[1]));
        }
        return records;
    }
}
