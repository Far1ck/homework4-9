package com.minibank.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

public final class ConnectionPool {
    private static final HikariDataSource DATA_SOURCE;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://localhost:5432/minibank");
        config.setUsername("globus_student");
        config.setPassword("secret");

        config.setMaximumPoolSize(16);
        config.setMinimumIdle(4);
        config.setConnectionTimeout(5000L);

        DATA_SOURCE = new HikariDataSource(config);
    }

    private ConnectionPool() {
    }

    public static DataSource getDataSource() {
        return DATA_SOURCE;
    }

    public static void close() {
        if (!DATA_SOURCE.isClosed()) {
            DATA_SOURCE.close();
        }
    }
}
