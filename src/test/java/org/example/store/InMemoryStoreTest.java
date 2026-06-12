package org.example.store;

import org.example.model.Record;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryStoreTest {

    private InMemoryStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryStore();
    }

    @Test
    @DisplayName("Record를 저장하면 동일한 id를 가진 Record가 반환된다")
    void save_저장된_Record_반환() {
        Record record = Record.of(Map.of("name", "Alice"));

        Record saved = store.save(record);

        assertNotNull(saved);
        assertEquals(record.getId(), saved.getId());
    }

    @Test
    @DisplayName("저장된 Record를 id로 조회할 수 있다")
    void findById_저장된_Record_조회() {
        Record record = Record.of(Map.of("name", "Alice"));
        store.save(record);

        Optional<Record> found = store.findById(record.getId());

        assertTrue(found.isPresent());
        assertEquals(record.getId(), found.get().getId());
        assertEquals("Alice", found.get().getField("name"));
    }

    @Test
    @DisplayName("존재하지 않는 id로 조회하면 Optional.empty()를 반환한다")
    void findById_없는_id_조회_시_empty_반환() {
        Optional<Record> found = store.findById("non-existent-id");

        assertTrue(found.isEmpty());
    }

    @Test
    @DisplayName("저장된 모든 Record를 반환한다")
    void findAll_저장된_모든_Record_반환() {
        store.save(Record.of(Map.of("name", "Alice")));
        store.save(Record.of(Map.of("name", "Bob")));

        List<Record> all = store.findAll();

        assertEquals(2, all.size());
    }

    @Test
    @DisplayName("저장된 Record가 없으면 빈 리스트를 반환한다")
    void findAll_저장된_Record_없으면_빈_리스트() {
        List<Record> all = store.findAll();

        assertNotNull(all);
        assertTrue(all.isEmpty());
    }

    @Test
    @DisplayName("update()로 필드를 수정하면 변경된 값이 반영되고 updatedAt이 갱신된다")
    void update_필드_수정_및_updatedAt_갱신() throws InterruptedException {
        Record record = Record.of(Map.of("name", "Alice", "age", 30));
        store.save(record);

        // updatedAt 변경을 확인하기 위해 1ms 대기
        Thread.sleep(1);

        Record updated = store.update(record.getId(), Map.of("name", "Alice Updated", "age", 31));

        assertEquals("Alice Updated", updated.getField("name"));
        assertEquals(31, updated.getField("age"));
        assertTrue(updated.getUpdatedAt().isAfter(record.getCreatedAt()));
    }

    @Test
    @DisplayName("존재하지 않는 id로 update() 호출 시 예외가 발생한다")
    void update_없는_id_예외_발생() {
        assertThrows(IllegalArgumentException.class,
                () -> store.update("non-existent-id", Map.of("name", "Ghost")));
    }

    @Test
    @DisplayName("delete()로 Record를 삭제하면 findById로 조회되지 않는다")
    void delete_삭제_후_조회_불가() {
        Record record = Record.of(Map.of("name", "Alice"));
        store.save(record);

        store.delete(record.getId());

        assertTrue(store.findById(record.getId()).isEmpty());
    }

    @Test
    @DisplayName("존재하지 않는 id로 delete() 호출 시 예외가 발생한다")
    void delete_없는_id_예외_발생() {
        assertThrows(IllegalArgumentException.class,
                () -> store.delete("non-existent-id"));
    }
}
