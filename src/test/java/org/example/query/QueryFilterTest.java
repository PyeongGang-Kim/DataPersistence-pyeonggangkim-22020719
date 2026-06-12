package org.example.query;

import org.example.model.Record;
import org.example.store.InMemoryStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class QueryFilterTest {

    private InMemoryStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryStore();
        store.save(Record.of(Map.of("name", "Alice", "city", "Seoul",  "age", 30)));
        store.save(Record.of(Map.of("name", "Bob",   "city", "Busan",  "age", 25)));
        store.save(Record.of(Map.of("name", "Carol",  "city", "Seoul", "age", 28)));
    }

    @Test
    @DisplayName("QueryFilter.of()로 단일 조건을 생성할 수 있다")
    void of_단일_조건_생성() {
        QueryFilter filter = QueryFilter.of("city", "Seoul");

        assertNotNull(filter);
        assertEquals("Seoul", filter.getConditions().get("city"));
        assertEquals(1, filter.getConditions().size());
    }

    @Test
    @DisplayName("and()로 조건을 추가하면 기존 조건이 유지된 채 새 조건이 추가된다")
    void and_조건_추가() {
        QueryFilter filter = QueryFilter.of("city", "Seoul").and("age", 30);

        assertEquals("Seoul", filter.getConditions().get("city"));
        assertEquals(30, filter.getConditions().get("age"));
        assertEquals(2, filter.getConditions().size());
    }

    @Test
    @DisplayName("단일 조건으로 query()를 실행하면 조건에 맞는 Record만 반환된다")
    void query_단일_조건_필터링() {
        QueryFilter filter = QueryFilter.of("city", "Seoul");

        List<Record> result = store.query(filter);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(r -> "Seoul".equals(r.getField("city"))));
    }

    @Test
    @DisplayName("AND 조건으로 query()를 실행하면 모든 조건을 만족하는 Record만 반환된다")
    void query_AND_조건_필터링() {
        QueryFilter filter = QueryFilter.of("city", "Seoul").and("age", 30);

        List<Record> result = store.query(filter);

        assertEquals(1, result.size());
        assertEquals("Alice", result.get(0).getField("name"));
    }

    @Test
    @DisplayName("조건에 맞는 Record가 없으면 빈 리스트를 반환한다")
    void query_일치하는_Record_없으면_빈_리스트() {
        QueryFilter filter = QueryFilter.of("city", "Incheon");

        List<Record> result = store.query(filter);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("전체 저장된 Record 중 조건을 만족하지 않는 Record는 결과에 포함되지 않는다")
    void query_미일치_Record_제외() {
        QueryFilter filter = QueryFilter.of("name", "Bob");

        List<Record> result = store.query(filter);

        assertEquals(1, result.size());
        assertEquals("Bob", result.get(0).getField("name"));
    }
}
