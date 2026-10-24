package com.minimart.config;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final String[] allowedOrigins;

    public CorsConfig(
        @Value("${app.allowed-origins:http://localhost:5173,http://localhost:3000,https://mini-mart-pos-orpin.vercel.app}") String origins
    ) {
        Set<String> originSet = new LinkedHashSet<>();
        Arrays.stream(origins.split(","))
            .map(String::trim)
            .map(s -> s.endsWith("/") ? s.substring(0, s.length() - 1) : s)
            .filter(s -> !s.isEmpty())
            .forEach(originSet::add);
        originSet.add("https://mini-mart-pos-orpin.vercel.app");
        this.allowedOrigins = originSet.toArray(String[]::new);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOrigins(allowedOrigins)
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .maxAge(3600);
    }
}
