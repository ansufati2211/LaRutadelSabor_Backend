package com.RutaDelSabor.ruta.config;

import com.RutaDelSabor.ruta.security.UserDetailsServiceImpl;
import com.RutaDelSabor.ruta.security.JwtRequestFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private JwtRequestFilter jwtRequestFilter;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @SuppressWarnings("deprecation")
    @Bean
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 1. CORS: Permitir acceso desde cualquier origen (Frontend, Dialogflow, etc.)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // 2. CSRF: Desactivar (No necesario para APIs REST stateless)
            .csrf(csrf -> csrf.disable())
            
            .authorizeHttpRequests(auth -> auth
                // --- RUTAS PÚBLICAS (Sin Login) ---
                
                // A. Webhook de Dialogflow (¡CRÍTICO!)
                // Permitimos todo en /api/webhook/** para evitar problemas de 403
                .requestMatchers("/api/webhook/**").permitAll()
                
                // B. Autenticación
                .requestMatchers("/api/auth/**").permitAll()
                
                // C. Recursos Públicos (Menú, Productos, Imágenes)
                .requestMatchers(HttpMethod.GET, "/api/productos/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/categorias/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/menu/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/comentarios/**").permitAll()
                
                // D. Swagger / UI Docs (Opcional, pero útil si lo tienes)
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                
                // E. Preflight CORS (OPTIONS)
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // --- RUTAS PROTEGIDAS (Requieren Token) ---
                
                // F. Administración (Solo escritura de productos/categorías)
                .requestMatchers(HttpMethod.POST, "/api/productos/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_VENDEDOR", "ADMIN", "VENDEDOR")
                .requestMatchers(HttpMethod.PUT, "/api/productos/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_VENDEDOR", "ADMIN", "VENDEDOR")
                .requestMatchers(HttpMethod.DELETE, "/api/productos/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_VENDEDOR", "ADMIN", "VENDEDOR")
                
                // G. Endpoints específicos de Admin
                .requestMatchers("/api/admin/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_VENDEDOR", "ROLE_DELIVERY", "ADMIN", "VENDEDOR", "DELIVERY")
                .requestMatchers("/api/productos/admin/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_VENDEDOR", "ADMIN", "VENDEDOR")

                // H. Cualquier otra cosa -> Requiere estar logueado (Usuario normal)
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // Configuración CORS Global
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Permitir todos los orígenes (*) es lo más fácil para debuggear,
        // pero en producción idealmente pones tus dominios.
        // Para que Dialogflow conecte sin líos, permitir todo es seguro aquí porque es una API pública.
        configuration.setAllowedOriginPatterns(List.of("*")); 
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}