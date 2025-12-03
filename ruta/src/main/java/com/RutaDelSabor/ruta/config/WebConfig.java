package com.RutaDelSabor.ruta.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    // Ya no necesitas addCorsMappings aquí porque lo maneja SecurityConfig
}