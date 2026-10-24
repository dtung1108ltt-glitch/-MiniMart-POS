package com.minimart.config;

import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final String[] allowedOrigins;

    public CorsConfig(@Value("${app.allowed-origins:http://localhost:5173,http://localhost:3000,[https://mini-mart-pos-orpin.vercel.app](https://mini-mart-pos-orpin.vercel.app)}") String origins) {
        this.allowedOrigins = Arrays.stream(origins.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toArray(String[]::new);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOriginPatterns(allowedOrigins.length > 0 ? allowedOrigins : new String[]{"*"})
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .maxAge(3600);
    }
}
