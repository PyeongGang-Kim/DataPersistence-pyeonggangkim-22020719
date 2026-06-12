package org.example.store;

import org.example.model.Record;
import org.example.query.QueryFilter;
import org.h2.tools.Server;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DatabaseStoreTest {

    private static final String JDBC_URL = "jdbc:h2:tcp://localhost:9093/mem:testdb;DB_CLOSE_DELAY=-1";
    private static final String USER = "sa";
    private static final String PASSWORD = "";
    private static Server h2Server;
    private DatabaseStore store;

    @BeforeAll
    static void startServer() throws Exception {
        h2Server = Server.createTcpServer("-tcp", "-tcpPort", "9093", "-ifNotExists").start();
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
    void setUp() throws Exception {
        try (Connection conn = DriverManager.getConnection(JDBC_URL, USER, PASSWORD);
             Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM record_fields");
            stmt.execute("DELETE FROM records");
        }
        store = new DatabaseStore(JDBC_URL, USER, PASSWORD);
    }

    @Test
    @DisplayName("save() 후 findById()로 DB에서 조회할 수 있다")
    void save_후_findById_조회() {
        Record record = Record.of(Map.of("name", "Alice", "city", "Seoul"));
        store.save(record);

        Optional<Record> found = store.findById(record.getId());

        assertTrue(found.isPresent());
        assertEquals(record.getId(), found.get().getId());
        assertEquals("Alice", found.get().getField("name"));
        assertEquals("Seoul", found.get().getField("city"));
    }

    @Test
    @DisplayName("findAll()로 저장된 모든 Record를 조회할 수 있다")
    void findAll_전체_조회() {
        store.save(Record.of(Map.of("name", "Alice")));
        store.save(Record.of(Map.of("name", "Bob")));

        List<Record> all = store.findAll();

        assertEquals(2, all.size());
    }

    @Test
    @DisplayName("query()로 AND 조건에 맞는 Record만 조회할 수 있다")
    void query_AND_조건_필터링() {
        store.save(Record.of(Map.of("name", "Alice", "city", "Seoul")));
        store.save(Record.of(Map.of("name", "Bob",   "city", "Busan")));
        store.save(Record.of(Map.of("name", "Carol",  "city", "Seoul")));

        List<Record> result = store.query(QueryFilter.of("city", "Seoul"));

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(r -> "Seoul".equals(r.getField("city"))));
    }

    @Test
    @DisplayName("update()로 필드를 수정하면 DB에 반영되고 updatedAt이 갱신된다")
    void update_필드_수정_DB_반영() throws InterruptedException {
        Record record = Record.of(Map.of("name", "Alice", "age", "30"));
        store.save(record);

        Thread.sleep(1);

        Record updated = store.update(record.getId(), Map.of("name", "Alice Updated", "age", "31"));

        assertEquals("Alice Updated", updated.getField("name"));
        assertEquals("31", updated.getField("age"));
        assertTrue(updated.getUpdatedAt().isAfter(record.getCreatedAt()));

        // DB에서 다시 조회해도 반영되어 있어야 한다
        Record fromDb = store.findById(record.getId()).orElseThrow();
        assertEquals("Alice Updated", fromDb.getField("name"));
    }

    @Test
    @DisplayName("delete()로 Record를 삭제하면 DB에서 제거된다")
    void delete_후_조회_불가() {
        Record record = Record.of(Map.of("name", "Alice"));
        store.save(record);

        store.delete(record.getId());

        assertTrue(store.findById(record.getId()).isEmpty());
    }

    @Test
    @DisplayName("존재하지 않는 id로 update() 호출 시 예외가 발생한다")
    void update_없는_id_예외() {
        assertThrows(IllegalArgumentException.class,
                () -> store.update("non-existent-id", Map.of("name", "Ghost")));
    }

    @Test
    @DisplayName("존재하지 않는 id로 delete() 호출 시 예외가 발생한다")
    void delete_없는_id_예외() {
        assertThrows(IllegalArgumentException.class,
                () -> store.delete("non-existent-id"));
    }

    @Test
    @DisplayName("영속성: 새 DatabaseStore 인스턴스로도 기존 저장된 데이터를 조회할 수 있다")
    void 영속성_새_인스턴스로_데이터_조회() {
        Record record = Record.of(Map.of("name", "Alice"));
        store.save(record);

        // 새 인스턴스(같은 DB)로 조회
        DatabaseStore anotherStore = new DatabaseStore(JDBC_URL, USER, PASSWORD);
        Optional<Record> found = anotherStore.findById(record.getId());

        assertTrue(found.isPresent());
        assertEquals("Alice", found.get().getField("name"));
    }
}
