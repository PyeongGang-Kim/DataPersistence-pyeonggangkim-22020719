package org.example;

import org.example.db.H2ServerManager;
import org.example.model.Record;
import org.example.query.QueryFilter;
import org.example.service.DataService;
import org.example.store.DatabaseStore;

import java.util.*;

public class Main {

    private static final String JDBC_URL = "jdbc:h2:tcp://localhost:9092/./data/persistence";
    private static final String USER     = "sa";
    private static final String PASSWORD = "";

    public static void main(String[] args) {
        H2ServerManager.start();
        Runtime.getRuntime().addShutdownHook(new Thread(H2ServerManager::stop));

        DataService service = new DataService(new DatabaseStore(JDBC_URL, USER, PASSWORD));
        Scanner scanner = new Scanner(System.in);

        printHelp();
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) continue;
            try {
                handleCommand(service, line);
            } catch (Exception e) {
                System.out.println("[오류] " + e.getMessage());
            }
        }
    }

    private static void handleCommand(DataService service, String line) {
        String[] tokens = line.split("\\s+", 2);
        String cmd  = tokens[0].toLowerCase();
        String rest = tokens.length > 1 ? tokens[1] : "";

        switch (cmd) {
            case "save" -> {
                Map<String, Object> fields = parseFields(rest);
                Record r = service.create(fields);
                System.out.println("[저장 완료] id=" + r.getId());
            }
            case "list" -> {
                List<Record> all = service.findAll();
                if (all.isEmpty()) { System.out.println("(저장된 데이터 없음)"); return; }
                all.forEach(Main::printRecord);
            }
            case "find" -> {
                service.findById(rest.trim())
                        .ifPresentOrElse(Main::printRecord,
                                () -> System.out.println("(없음)"));
            }
            case "query" -> {
                Map<String, Object> conds = parseFields(rest);
                if (conds.isEmpty()) { System.out.println("[오류] 조건을 입력하세요."); return; }
                Iterator<Map.Entry<String, Object>> it = conds.entrySet().iterator();
                Map.Entry<String, Object> first = it.next();
                QueryFilter filter = QueryFilter.of(first.getKey(), first.getValue());
                while (it.hasNext()) {
                    Map.Entry<String, Object> e = it.next();
                    filter = filter.and(e.getKey(), e.getValue());
                }
                List<Record> result = service.query(filter);
                if (result.isEmpty()) { System.out.println("(조건에 맞는 데이터 없음)"); return; }
                result.forEach(Main::printRecord);
            }
            case "update" -> {
                String[] parts = rest.split("\\s+", 2);
                if (parts.length < 2) { System.out.println("[오류] update <id> <key=value ...>"); return; }
                Record r = service.update(parts[0], parseFields(parts[1]));
                System.out.println("[수정 완료] id=" + r.getId());
            }
            case "delete" -> {
                service.delete(rest.trim());
                System.out.println("[삭제 완료] id=" + rest.trim());
            }
            case "help"  -> printHelp();
            case "exit"  -> { System.out.println("종료합니다."); System.exit(0); }
            default      -> System.out.println("[오류] 알 수 없는 명령어. 'help'를 입력하세요.");
        }
    }

    private static Map<String, Object> parseFields(String input) {
        Map<String, Object> fields = new LinkedHashMap<>();
        for (String token : input.trim().split("\\s+")) {
            String[] kv = token.split("=", 2);
            if (kv.length == 2 && !kv[0].isEmpty()) {
                fields.put(kv[0], kv[1]);
            }
        }
        return fields;
    }

    private static void printRecord(Record r) {
        System.out.println("id=" + r.getId()
                + " fields=" + r.getFields()
                + " createdAt=" + r.getCreatedAt()
                + " updatedAt=" + r.getUpdatedAt());
    }

    private static void printHelp() {
        System.out.println("""
                ── DataPersistence CLI ──────────────────────────────
                  save   <key=value> [key=value ...]   레코드 저장
                  list                                  전체 조회
                  find   <id>                           ID 조회
                  query  <key=value> [key=value ...]   조건 조회 (AND)
                  update <id> <key=value> [...]         필드 수정
                  delete <id>                           레코드 삭제
                  help                                  도움말
                  exit                                  종료
                ─────────────────────────────────────────────────────""");
    }
}
