package com.minibank.db;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;

import javax.sql.DataSource;
import java.sql.Connection;

public final class MigrationRunner {
    private static final String CHANGELOG_PATH = "db/changelog/db.changelog-master.yaml";

    private MigrationRunner() {
    }

    public static void runMigrations(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection()) {
            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(conn));

            try (Liquibase liquibase = new Liquibase(
                    CHANGELOG_PATH,
                    new ClassLoaderResourceAccessor(),
                    database)) {
                liquibase.update("");
            }
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при выполнении миграций Liquibase", e);
        }
    }
}
