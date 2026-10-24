package com.minimart.config;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Chuyển đổi biến môi trường DATABASE_URL dạng postgres://user:pass@host:port/db (Render/Railway)
 * sang spring.datasource.url (jdbc:postgresql://...), username và password.
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String databaseUrl = environment.getProperty("DATABASE_URL");
        if (databaseUrl == null || databaseUrl.isBlank()) {
            return;
        }
        String trimmed = databaseUrl.trim();
        if (trimmed.startsWith("postgres://") || trimmed.startsWith("postgresql://")) {
            try {
                URI uri = new URI(trimmed);
                String host = uri.getHost();
                int port = uri.getPort() > 0 ? uri.getPort() : 5432;
                String path = uri.getPath();
                String query = uri.getQuery();
                String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + path
                    + (query != null && !query.isBlank() ? "?" + query : "");

                Map<String, Object> props = new HashMap<>();
                props.put("spring.datasource.url", jdbcUrl);
                props.put("spring.datasource.driver-class-name", "org.postgresql.Driver");

                String userInfo = uri.getUserInfo();
                if (userInfo != null && !userInfo.isBlank()) {
                    String[] parts = userInfo.split(":", 2);
                    props.put("spring.datasource.username", parts[0]);
                    if (parts.length > 1) {
                        props.put("spring.datasource.password", parts[1]);
                    }
                }
                environment.getPropertySources().addFirst(new MapPropertySource("renderDatabaseUrl", props));
            } catch (Exception ignored) {
                // Nếu chuỗi không phải URI hợp lệ thì để cấu hình mặc định xử lý
            }
        }
    }
}
