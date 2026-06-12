# PRD: DataPersistence PoC

## 1. 개요

### 배경
데이터를 입력받아 저장하고, 저장된 데이터를 쿼리로 조회할 수 있는 기능을 검증하는 PoC(Proof of Concept) 프로젝트다. 다양한 데이터 저장 전략의 실현 가능성과 조회 성능을 탐색하는 것이 목적이다.

### 목표
- 구조화된 데이터를 입력받아 영속적으로 저장한다.
- 저장된 데이터를 조건 기반 쿼리로 조회한다.
- 저장 계층을 교체 가능한 구조로 설계하여 다양한 저장 방식을 비교한다.

### 범위 (PoC)
이 프로젝트는 프로덕션 투입이 아닌 기술 검증용이다. 인증, 보안, 대용량 처리, 분산 환경은 이번 PoC의 범위 밖이다.

---

## 2. 사용자 스토리

| ID | 역할 | 행동 | 기대 결과 |
|----|------|------|-----------|
| US-01 | 사용자 | 데이터를 입력한다 | 데이터가 저장소에 기록된다 |
| US-02 | 사용자 | 전체 데이터를 조회한다 | 저장된 모든 레코드가 출력된다 |
| US-03 | 사용자 | 특정 필드 값으로 필터링한다 | 조건에 맞는 레코드만 반환된다 |
| US-04 | 사용자 | 특정 레코드를 ID로 조회한다 | 해당 레코드가 반환된다 |
| US-05 | 사용자 | 저장된 데이터를 수정한다 | 수정 내용이 저장소에 반영된다 |
| US-06 | 사용자 | 특정 레코드를 삭제한다 | 해당 레코드가 저장소에서 제거된다 |

---

## 3. 기능 요구사항

### 3.1 데이터 모델
- **Record**: 단일 저장 단위로, 고유 ID와 하나 이상의 필드(key-value)를 가진다.
- ID는 시스템이 자동 생성한다 (UUID 또는 순번).
- 생성 시각(`createdAt`)과 수정 시각(`updatedAt`)을 자동으로 기록한다.

```
Record {
    id          : String      // 고유 식별자
    fields      : Map<String, Object>  // 사용자 정의 데이터
    createdAt   : LocalDateTime
    updatedAt   : LocalDateTime
}
```

### 3.2 데이터 입력 (Create)
- CLI 또는 코드 호출을 통해 레코드를 생성한다.
- 필드는 동적으로 정의 가능하다 (스키마 고정 불필요).

### 3.3 데이터 조회 (Query)
- **전체 조회**: 저장된 모든 레코드를 반환한다.
- **ID 조회**: 특정 ID를 가진 레코드를 반환한다.
- **필드 필터**: 특정 필드 값이 일치하거나 포함하는 레코드를 반환한다.
- **복합 필터**: AND 조건으로 여러 필드를 동시에 필터링한다.

### 3.4 데이터 수정 (Update)
- ID를 기반으로 특정 레코드의 필드를 수정한다.
- `updatedAt`이 자동 갱신된다.

### 3.5 데이터 삭제 (Delete)
- ID를 기반으로 특정 레코드를 삭제한다.

### 3.6 저장 계층 추상화
저장 방식을 교체할 수 있도록 인터페이스를 통해 추상화한다. PoC에서는 아래 두 가지 구현체를 검증한다.

| 구현체 | 설명 |
|--------|------|
| `InMemoryStore` | JVM 메모리 내 Map 기반 저장. 테스트 및 빠른 검증용. 재시작 시 데이터 소멸. |
| `DatabaseStore` | JDBC를 통한 관계형 DB 저장. 재시작 후에도 데이터 유지. |

### 3.7 데이터베이스 구성
- 선택 DB: **H2 (서버 모드)** — Docker 등 외부 인프라 없이 JAR 하나로 구동 가능하며, TCP 서버 모드로 실행하여 실제 외부 DB처럼 동작한다.
- 앱이 시작될 때 H2 TCP 서버를 프로그래밍 방식으로 자동 기동하고, 앱 종료 시 함께 정지한다.
- 데이터는 파일(`./data/persistence.mv.db`)에 저장되므로 재시작 후에도 유지된다.
- 추후 PostgreSQL/MySQL 등 외부 DB로 전환 시 JDBC URL과 드라이버 의존성만 교체하면 되도록 설계한다.
- DB 스키마는 애플리케이션 기동 시 자동 생성(DDL auto-create)한다.

**H2 서버 기동 구조**
```
애플리케이션 기동
  └─ H2 TCP 서버 자동 시작 (포트 9092)
       └─ JDBC 연결: jdbc:h2:tcp://localhost:9092/./data/persistence
            └─ 데이터 파일: ./data/persistence.mv.db
```

**외부 툴 접속 정보 (DBeaver 등)**
```
JDBC URL : jdbc:h2:tcp://localhost:9092/./data/persistence
User     : sa
Password : (없음)
```

**테이블 설계 (초안)**
```sql
CREATE TABLE IF NOT EXISTS records (
    id          VARCHAR(36)  PRIMARY KEY,
    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP    NOT NULL
);

CREATE TABLE IF NOT EXISTS record_fields (
    record_id   VARCHAR(36)  NOT NULL REFERENCES records(id) ON DELETE CASCADE,
    field_key   VARCHAR(255) NOT NULL,
    field_value TEXT,
    PRIMARY KEY (record_id, field_key)
);
```

---

## 4. 비기능 요구사항

| 항목 | 기준 |
|------|------|
| 응답 속도 | 단일 레코드 조회 < 10ms (InMemory 기준) |
| 데이터 건수 | PoC 기준 최대 10,000건 처리 검증 |
| 언어 / 런타임 | Java 17+, Gradle |
| 외부 의존성 | H2 (임베디드 DB), 필요 시 Jackson (JSON 직렬화) |
| 테스트 | 핵심 기능에 대한 JUnit 5 단위 테스트 포함 |

---

## 5. 시스템 구조 (제안)

```
org.example
├── model
│   └── Record.java              // 데이터 모델
├── store
│   ├── DataStore.java           // 저장소 인터페이스 (CRUD + Query)
│   ├── InMemoryStore.java       // 메모리 기반 구현체 (테스트용)
│   └── DatabaseStore.java       // JDBC 기반 DB 구현체
├── db
│   ├── H2ServerManager.java     // H2 TCP 서버 기동/정지 관리
│   └── SchemaInitializer.java   // 기동 시 DDL 자동 생성
├── query
│   └── QueryFilter.java         // 필터 조건 정의
├── service
│   └── DataService.java         // 비즈니스 로직, 저장소 위임
└── Main.java                    // 진입점 및 CLI 루프
```

---

## 6. 인터페이스 설계 (핵심)

```java
public interface DataStore {
    Record save(Record record);
    Optional<Record> findById(String id);
    List<Record> findAll();
    List<Record> query(QueryFilter filter);
    Record update(String id, Map<String, Object> fields);
    void delete(String id);
}
```

---

## 7. 성공 기준

| 항목 | 기준 |
|------|------|
| CRUD 동작 | 6개 사용자 스토리 모두 동작 확인 |
| 저장 전환 | `InMemoryStore` ↔ `DatabaseStore` 전환 시 서비스 코드 변경 없음 |
| 영속성 | `DatabaseStore` 사용 시 재시작 후 데이터 유지 |
| DB 스키마 | 애플리케이션 기동 시 테이블 자동 생성 |
| 단일 구동 | Docker 없이 `java -jar` 한 번으로 H2 서버 + 앱 동시 기동 |
| 테스트 통과 | 핵심 기능 단위 테스트 전체 GREEN |

---

## 8. 마일스톤

| 단계 | 내용 |
|------|------|
| M1 | 데이터 모델 및 `DataStore` 인터페이스 정의 |
| M2 | `InMemoryStore` 구현 및 CRUD 테스트 작성 |
| M3 | `QueryFilter` 구현 및 필터 조회 테스트 작성 |
| M4 | `DatabaseStore` 구현 (H2 JDBC), 스키마 자동 생성 및 영속성 검증 |
| M5 | CLI 진입점 구현 및 통합 시나리오 검증 |

---

## 9. 미결 사항 (TBD)

- ID 생성 전략: UUID vs 자동 증가 순번 — 구현 시 결정
- 쿼리 확장: OR 조건, 범위 조건(날짜/숫자) 필요 여부 — PoC 결과 후 검토
- ORM 도입 여부: 순수 JDBC vs MyBatis vs JPA(Hibernate) — 복잡도와 학습 비용 고려하여 결정
- H2 → 외부 DB 전환 시점: PoC 검증 완료 후 필요성 판단
