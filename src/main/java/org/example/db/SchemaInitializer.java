package org.example.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class SchemaInitializer {

    private SchemaInitializer() {}

    public static void initialize(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS records (
                        id VARCHAR(36) PRIMARY KEY,
                        created_at TIMESTAMP NOT NULL,
                        updated_at TIMESTAMP NOT NULL
                    )""");
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS record_fields (
                        record_id VARCHAR(36) NOT NULL,
                        field_key VARCHAR(255) NOT NULL,
                        field_value VARCHAR(4096),
                        PRIMARY KEY (record_id, field_key),
                        FOREIGN KEY (record_id) REFERENCES records(id) ON DELETE CASCADE
                    )""");
        }
    }
}
