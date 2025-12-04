package com.RutaDelSabor.ruta.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    // BORRA EL MÉTODO addCorsMappings
    // Dejamos que SecurityConfig se encargue de todo.
}