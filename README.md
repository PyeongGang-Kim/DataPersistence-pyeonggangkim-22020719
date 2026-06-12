# DataPersistence PoC

데이터를 입력받아 DB에 저장하고, 저장된 데이터를 조건 기반 쿼리로 조회하는 기능을 검증하는 PoC 프로젝트입니다.

---

## 기술 스택

| 항목 | 내용 |
|------|------|
| 언어 | Java 17+ |
| 빌드 | Gradle (Kotlin DSL) |
| DB | H2 (TCP 서버 모드, 임베디드 기동) |
| 테스트 | JUnit 5 |

---

## 프로젝트 구조

```
src/main/java/org/example/
├── model/
│   └── Record.java              # 데이터 모델 (id, fields, createdAt, updatedAt)
├── store/
│   ├── DataStore.java           # 저장소 인터페이스
│   ├── InMemoryStore.java       # 메모리 기반 구현체 (테스트용)
│   └── DatabaseStore.java       # JDBC 기반 H2 구현체
├── db/
│   ├── H2ServerManager.java     # H2 TCP 서버 기동/정지
│   └── SchemaInitializer.java   # DDL 자동 생성
├── query/
│   └── QueryFilter.java         # AND 조건 필터 빌더
├── service/
│   └── DataService.java         # 비즈니스 로직
└── Main.java                    # 진입점 및 CLI 루프
```

---

## 빠른 시작

### 요구사항

- Java 17 이상

### 빌드 및 실행

```bash
# 빌드
./gradlew build

# 실행 (H2 서버 자동 기동 포함)
./gradlew run
```

앱을 실행하면 H2 TCP 서버(포트 9092)가 자동으로 함께 시작됩니다.  
데이터는 `./data/persistence.mv.db` 파일에 저장되며, 재시작 후에도 유지됩니다.

### 테스트 실행

```bash
# 전체 테스트
./gradlew test

# 특정 클래스만
./gradlew test --tests "org.example.store.InMemoryStoreTest"
./gradlew test --tests "org.example.store.DatabaseStoreTest"
./gradlew test --tests "org.example.service.DataServiceTest"
```

---

## CLI 사용법

앱 실행 후 아래 명령어를 입력합니다.

| 명령어 | 설명 | 예시 |
|--------|------|------|
| `save <key=value> ...` | 레코드 저장 | `save name=Alice city=Seoul` |
| `list` | 전체 조회 | `list` |
| `find <id>` | ID로 조회 | `find 550e8400-e29b-...` |
| `query <key=value> ...` | AND 조건 필터 조회 | `query city=Seoul` |
| `update <id> <key=value> ...` | 필드 수정 | `update 550e8400-... name=Bob` |
| `delete <id>` | 레코드 삭제 | `delete 550e8400-...` |
| `help` | 도움말 | `help` |
| `exit` | 종료 | `exit` |

### 사용 예시

```
save name=Alice city=Seoul age=30
[저장 완료] id=550e8400-e29b-41d4-a716-446655440000

list
id=550e8400-e29b-41d4-a716-446655440000 fields={name=Alice, city=Seoul, age=30} ...

query city=Seoul
id=550e8400-e29b-41d4-a716-446655440000 fields={name=Alice, city=Seoul, age=30} ...

update 550e8400-e29b-41d4-a716-446655440000 name=Alice city=Busan
[수정 완료] id=550e8400-e29b-41d4-a716-446655440000

delete 550e8400-e29b-41d4-a716-446655440000
[삭제 완료] id=550e8400-e29b-41d4-a716-446655440000
```

---

## DB 접속 정보 (외부 툴 연결)

앱이 실행 중일 때 DBeaver 등 외부 툴로 접속할 수 있습니다.

```
JDBC URL : jdbc:h2:tcp://localhost:9092/./data/persistence
User     : sa
Password : (없음)
Port     : 9092
```

---

## 데이터 모델

```
Record {
    id        : String              // UUID, 자동 생성
    fields    : Map<String, Object> // 동적 key-value 필드
    createdAt : LocalDateTime       // 생성 시각, 자동 설정
    updatedAt : LocalDateTime       // 수정 시각, 자동 갱신
}
```

DB 스키마는 앱 기동 시 자동으로 생성됩니다.

```sql
CREATE TABLE IF NOT EXISTS records (
    id         VARCHAR(36)  PRIMARY KEY,
    created_at TIMESTAMP    NOT NULL,
    updated_at TIMESTAMP    NOT NULL
);

CREATE TABLE IF NOT EXISTS record_fields (
    record_id   VARCHAR(36)  NOT NULL,
    field_key   VARCHAR(255) NOT NULL,
    field_value VARCHAR(4096),
    PRIMARY KEY (record_id, field_key),
    FOREIGN KEY (record_id) REFERENCES records(id) ON DELETE CASCADE
);
```

---

## 저장소 교체

`DataStore` 인터페이스를 통해 저장 구현체를 교체할 수 있습니다.  
`DataService`는 구현체에 의존하지 않으므로 서비스 코드 변경 없이 전환이 가능합니다.

```java
// 메모리 기반 (테스트용)
DataService service = new DataService(new InMemoryStore());

// H2 DB 기반 (영속)
DataService service = new DataService(new DatabaseStore(JDBC_URL, USER, PASSWORD));
```

---

## 마일스톤

| 단계 | 내용 | 상태 |
|------|------|------|
| M1 | 데이터 모델 및 `DataStore` 인터페이스 정의 | ✅ 완료 |
| M2 | `InMemoryStore` 구현 및 CRUD 테스트 | ✅ 완료 |
| M3 | `QueryFilter` 구현 및 필터 조회 테스트 | ✅ 완료 |
| M4 | `DatabaseStore` 구현, H2 서버 기동, 영속성 검증 | ✅ 완료 |
| M5 | CLI 진입점 구현 및 통합 시나리오 검증 | ✅ 완료 |
