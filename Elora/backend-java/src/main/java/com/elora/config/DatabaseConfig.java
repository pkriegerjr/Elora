package com.elora.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.cdimascio.dotenv.Dotenv;

import javax.sql.DataSource;

public class DatabaseConfig {

    private static HikariDataSource dataSource;

    public static void init() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

        String url = getEnv(dotenv, "DATABASE_URL", "jdbc:postgresql://localhost:5432/elora_db");
        String user = getEnv(dotenv, "DATABASE_USER", "postgres");
        String pass = getEnv(dotenv, "DATABASE_PASSWORD", "postgres");

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(pass);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setDriverClassName("org.postgresql.Driver");

        dataSource = new HikariDataSource(config);
        System.out.println("PostgreSQL conectado: " + url);
    }

    public static DataSource getDataSource() {
        if (dataSource == null) throw new IllegalStateException("DatabaseConfig.init() não chamado");
        return dataSource;
    }

    private static String getEnv(Dotenv dotenv, String key, String fallback) {
        String v = System.getenv(key);
        if (v != null) return v;
        v = dotenv.get(key);
        return v != null ? v : fallback;
    }
}
