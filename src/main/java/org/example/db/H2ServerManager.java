package org.example.db;

import org.h2.tools.Server;

import java.sql.SQLException;

public class H2ServerManager {

    private static Server server;

    private H2ServerManager() {}

    public static void start() {
        try {
            server = Server.createTcpServer("-tcp", "-tcpPort", "9092", "-ifNotExists").start();
        } catch (SQLException e) {
            throw new RuntimeException("H2 서버 기동 실패", e);
        }
    }

    public static void stop() {
        if (server != null) {
            server.stop();
            server = null;
        }
    }
}
