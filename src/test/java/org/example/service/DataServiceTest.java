package org.example.service;

import org.example.model.Record;
import org.example.query.QueryFilter;
import org.example.store.DatabaseStore;
import org.example.store.InMemoryStore;
import org.h2.tools.Server;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DataServiceTest {

    private static final String JDBC_URL = "jdbc:h2:tcp://localhost:9094/mem:servicedb;DB_CLOSE_DELAY=-1";
    private static final String USER = "sa";
    private static final String PASSWORD = "";
    private static Server h2Server;

    @BeforeAll
    static void startServer() throws Exception {
        h2Server = Server.createTcpServer("-tcp", "-tcpPort", "9094", "-ifNotExists").start();
        try (Connection conn = DriverManager.getConnection(JDBC_URL, USER, PASSWORD);
             Statement stmt = conn.createStatement()) {
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

    @AfterAll
    static void stopServer() {
        if (h2Server != null) h2Server.stop();
    }

    @BeforeEach
    void clearDb() throws Exception {
        try (Connection conn = DriverManager.getConnection(JDBC_URL, USER, PASSWORD);
             Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM record_fields");
            stmt.execute("DELETE FROM records");
        }
    }

    // ── InMemoryStore 기반 시나리오 ──────────────────────────────────────

    @Test
    @DisplayName("[InMemory] create()로 생성한 Record를 findById()로 조회할 수 있다")
    void inMemory_create_and_findById() {
        DataService service = new DataService(new InMemoryStore());

        Record created = service.create(Map.of("name", "Alice", "city", "Seoul"));

        assertNotNull(created);
        Optional<Record> found = service.findById(created.getId());
        assertTrue(found.isPresent());
        assertEquals("Alice", found.get().getField("name"));
    }

    @Test
    @DisplayName("[InMemory] findAll()로 저장된 모든 Record를 조회할 수 있다")
    void inMemory_findAll() {
        DataService service = new DataService(new InMemoryStore());
        service.create(Map.of("name", "Alice"));
        service.create(Map.of("name", "Bob"));

        assertEquals(2, service.findAll().size());
    }

    @Test
    @DisplayName("[InMemory] query()로 AND 조건 필터 조회가 가능하다")
    void inMemory_query() {
        DataService service = new DataService(new InMemoryStore());
        service.create(Map.of("name", "Alice", "city", "Seoul"));
        service.create(Map.of("name", "Bob",   "city", "Busan"));

        List<Record> result = service.query(QueryFilter.of("city", "Seoul"));

        assertEquals(1, result.size());
        assertEquals("Alice", result.get(0).getField("name"));
    }

    @Test
    @DisplayName("[InMemory] update()로 필드를 수정할 수 있다")
    void inMemory_update() {
        DataService service = new DataService(new InMemoryStore());
        Record created = service.create(Map.of("name", "Alice"));

        Record updated = service.update(created.getId(), Map.of("name", "Alice Updated"));

        assertEquals("Alice Updated", updated.getField("name"));
    }

    @Test
    @DisplayName("[InMemory] delete()로 Record를 삭제할 수 있다")
    void inMemory_delete() {
        DataService service = new DataService(new InMemoryStore());
        Record created = service.create(Map.of("name", "Alice"));

        service.delete(created.getId());

        assertTrue(service.findById(created.getId()).isEmpty());
    }

    // ── DatabaseStore 기반 시나리오 ──────────────────────────────────────

    @Test
    @DisplayName("[DB] create()로 생성한 Record를 findById()로 DB에서 조회할 수 있다")
    void db_create_and_findById() {
        DataService service = new DataService(new DatabaseStore(JDBC_URL, USER, PASSWORD));

        Record created = service.create(Map.of("name", "Alice", "city", "Seoul"));

        assertNotNull(created);
        Optional<Record> found = service.findById(created.getId());
        assertTrue(found.isPresent());
        assertEquals("Alice", found.get().getField("name"));
    }

    @Test
    @DisplayName("[DB] query()로 AND 조건 필터 조회가 가능하다")
    void db_query() {
        DataService service = new DataService(new DatabaseStore(JDBC_URL, USER, PASSWORD));
        service.create(Map.of("name", "Alice", "city", "Seoul"));
        service.create(Map.of("name", "Bob",   "city", "Busan"));

        List<Record> result = service.query(QueryFilter.of("city", "Seoul"));

        assertEquals(1, result.size());
        assertEquals("Alice", result.get(0).getField("name"));
    }

    @Test
    @DisplayName("[저장소 교체] InMemoryStore → DatabaseStore 전환 시 DataService 코드 변경 없음")
    void 저장소_교체_서비스_코드_불변() {
        // InMemoryStore로 동작 확인
        DataService inMemoryService = new DataService(new InMemoryStore());
        Record r1 = inMemoryService.create(Map.of("name", "Alice"));
        assertEquals("Alice", inMemoryService.findById(r1.getId()).orElseThrow().getField("name"));

        // DatabaseStore로 동일한 서비스 코드 동작 확인
        DataService dbService = new DataService(new DatabaseStore(JDBC_URL, USER, PASSWORD));
        Record r2 = dbService.create(Map.of("name", "Alice"));
        assertEquals("Alice", dbService.findById(r2.getId()).orElseThrow().getField("name"));
    }
}
