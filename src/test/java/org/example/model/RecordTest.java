package org.example.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RecordTest {

    @Test
    @DisplayName("필드로 Record를 생성하면 UUID 형식의 ID가 자동 부여된다")
    void 필드로_Record_생성_시_UUID_자동_부여() {
        Map<String, Object> fields = Map.of("name", "Alice", "age", 30);

        Record record = Record.of(fields);

        assertNotNull(record.getId());
        assertFalse(record.getId().isBlank());
        // UUID 형식 검증: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
        assertTrue(record.getId().matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
    }

    @Test
    @DisplayName("Record를 생성하면 createdAt이 현재 시각으로 자동 설정된다")
    void Record_생성_시_createdAt_자동_설정() {
        LocalDateTime before = LocalDateTime.now();

        Record record = Record.of(Map.of("name", "Alice"));

        LocalDateTime after = LocalDateTime.now();
        assertNotNull(record.getCreatedAt());
        assertFalse(record.getCreatedAt().isBefore(before));
        assertFalse(record.getCreatedAt().isAfter(after));
    }

    @Test
    @DisplayName("Record 생성 직후 updatedAt은 createdAt과 동일하다")
    void Record_생성_직후_updatedAt은_createdAt과_동일() {
        Record record = Record.of(Map.of("name", "Alice"));

        assertEquals(record.getCreatedAt(), record.getUpdatedAt());
    }

    @Test
    @DisplayName("저장된 필드를 키로 조회할 수 있다")
    void 저장된_필드를_키로_조회() {
        Record record = Record.of(Map.of("name", "Alice", "age", 30));

        assertEquals("Alice", record.getField("name"));
        assertEquals(30, record.getField("age"));
    }

    @Test
    @DisplayName("존재하지 않는 키로 필드 조회 시 null을 반환한다")
    void 존재하지_않는_키_조회_시_null_반환() {
        Record record = Record.of(Map.of("name", "Alice"));

        assertNull(record.getField("nonexistent"));
    }

    @Test
    @DisplayName("두 번 생성된 Record는 서로 다른 ID를 가진다")
    void 두_Record는_서로_다른_ID를_가진다() {
        Record record1 = Record.of(Map.of("name", "Alice"));
        Record record2 = Record.of(Map.of("name", "Bob"));

        assertNotEquals(record1.getId(), record2.getId());
    }

    @Test
    @DisplayName("restore()로 생성된 Record는 전달된 ID와 타임스탬프를 그대로 유지한다")
    void restore로_생성된_Record는_전달된_값을_유지() {
        String id = "test-id-1234";
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 12, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 6, 1, 9, 0);

        Record record = Record.restore(id, Map.of("name", "Alice"), createdAt, updatedAt);

        assertEquals(id, record.getId());
        assertEquals(createdAt, record.getCreatedAt());
        assertEquals(updatedAt, record.getUpdatedAt());
    }
}
