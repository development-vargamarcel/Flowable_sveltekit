package com.demo.bpm.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Configuration
public class CorsConfig {

    @Value("${RAILWAY_PUBLIC_DOMAIN:}")
    private String railwayPublicDomain;

    @Value("${CORS_ALLOWED_ORIGINS:}")
    private String corsAllowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        List<String> allowedOrigins = new ArrayList<>(Arrays.asList(
            "http://localhost:3000",
            "http://localhost:5173",
            "http://frontend:3000",
            "http://127.0.0.1:3000"
        ));

        // Add Railway domain if running on Railway
        if (railwayPublicDomain != null && !railwayPublicDomain.isEmpty()) {
            allowedOrigins.add("https://" + railwayPublicDomain);
            // Also allow the internal nginx proxy (requests come from localhost in container)
            allowedOrigins.add("http://127.0.0.1");
        }

        // Add any custom CORS origins from environment
        if (corsAllowedOrigins != null && !corsAllowedOrigins.isEmpty()) {
            Arrays.stream(corsAllowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .forEach(allowedOrigins::add);
        }

        log.info("CORS configuration initialized with allowed origins: {}", allowedOrigins);
        if (railwayPublicDomain != null && !railwayPublicDomain.isEmpty()) {
            log.info("Railway domain detected: {}", railwayPublicDomain);
        }

        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
