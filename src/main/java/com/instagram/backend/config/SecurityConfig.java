package com.instagram.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SecurityConfig implements WebMvcConfigurer {

    // Patterns (not exact origins) so wildcard subdomains like
    // https://*.up.railway.app are supported. Browsers always send an
    // Origin header on POST fetches, and Spring answers 403 "Invalid CORS
    // request" when it is not listed here — even for same-host calls
    // arriving via a proxy (Railway) with a different scheme/port.
    @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:4173,https://downloadinstavideo.website,https://*.up.railway.app}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(allowedOrigins.split(","))
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("*");
    }
}
