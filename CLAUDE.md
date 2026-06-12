# CLAUDE.md — DataPersistence PoC

## 프로젝트 개요

데이터를 입력받아 DB에 저장하고, 저장된 데이터를 조건 기반 쿼리로 조회하는 기능을 검증하는 PoC 프로젝트다.
자세한 요구사항은 [PRD.md](./PRD.md)를 참고한다.

---

## 기술 스택

| 항목 | 내용 |
|------|------|
| 언어 | Java 17+ |
| 빌드 | Gradle (Kotlin DSL) |
| DB | H2 (서버 모드, 임베디드 기동) |
| 테스트 | JUnit 5 |

---

## 패키지 구조

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
│   └── QueryFilter.java         # 필터 조건 정의
├── service/
│   └── DataService.java         # 비즈니스 로직
└── Main.java                    # 진입점 및 CLI 루프

src/test/java/org/example/
└── (각 클래스에 대응하는 테스트 클래스)
```

---

## 마일스톤

| 단계 | 내용 | 상태 |
|------|------|------|
| M1 | 데이터 모델 및 `DataStore` 인터페이스 정의 | 미시작 |
| M2 | `InMemoryStore` 구현 및 CRUD 테스트 작성 | 미시작 |
| M3 | `QueryFilter` 구현 및 필터 조회 테스트 작성 | 미시작 |
| M4 | `DatabaseStore` 구현, H2 서버 기동, 영속성 검증 | 미시작 |
| M5 | CLI 진입점 구현 및 통합 시나리오 검증 | 미시작 |

---

## 개발 방법론: TDD

이 프로젝트는 **TDD(Test-Driven Development)** 방식으로 진행한다.
모든 기능 구현은 아래 사이클을 반드시 준수한다.

### TDD 사이클

```
RED  →  (사용자 검증)  →  GREEN  →  (사용자 검증)  →  커밋 & 푸시
```

#### RED 단계
1. 구현할 기능의 테스트 코드를 먼저 작성한다.
2. 테스트는 반드시 실패하는 상태여야 한다 (구현체 없음).
3. **작성된 테스트 코드를 사용자에게 제시하고 검증을 요청한다.**
4. 사용자의 승인 없이 GREEN 구현으로 넘어가지 않는다.

#### GREEN 단계
1. 사용자 승인 후 테스트를 통과시키는 최소한의 구현 코드를 작성한다.
2. 과도한 추상화, 미래를 위한 설계는 하지 않는다. 테스트를 통과하는 것에만 집중한다.
3. 구현 완료 후 `./gradlew test`를 실행하여 전체 테스트가 GREEN인지 확인한다.
4. **구현 결과를 사용자에게 제시하고 검증을 요청한다.**

#### 커밋 & 푸시
1. 사용자 검증 통과 후에만 커밋 & 푸시를 진행한다.
2. 커밋 메시지는 아래 형식을 따른다.

---

## 커밋 메시지 규칙

```
<type>(<scope>): <subject>

type:
  feat     새로운 기능
  test     테스트 코드 추가/수정
  refactor 기능 변경 없는 코드 개선
  fix      버그 수정
  chore    빌드, 설정 변경

예시:
  test(store): InMemoryStore CRUD 테스트 작성 (RED)
  feat(store): InMemoryStore CRUD 구현 (GREEN)
  test(query): QueryFilter 필터 조회 테스트 작성 (RED)
  feat(query): QueryFilter AND 조건 구현 (GREEN)
```

---

## 테스트 실행

```bash
./gradlew test
./gradlew test --tests "org.example.store.InMemoryStoreTest"
```

---

## DB 접속 정보 (H2 서버 모드)

앱 기동 시 H2 TCP 서버가 자동으로 함께 시작된다.

```
JDBC URL : jdbc:h2:tcp://localhost:9092/./data/persistence
User     : sa
Password : (없음)
포트     : 9092
데이터파일: ./data/persistence.mv.db
```

---

## 주요 규칙

- RED → 사용자 검증 → GREEN → 사용자 검증 → 커밋 순서를 절대 건너뛰지 않는다.
- 커밋 & 푸시는 사용자가 명시적으로 승인한 후에만 실행한다.
- 테스트 없이 구현 코드를 먼저 작성하지 않는다.
- PRD 범위를 벗어나는 기능(인증, 보안, 대용량 처리 등)은 구현하지 않는다.
