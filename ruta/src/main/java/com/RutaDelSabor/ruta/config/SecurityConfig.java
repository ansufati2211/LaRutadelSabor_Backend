package com.RutaDelSabor.ruta.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.RutaDelSabor.ruta.security.JwtRequestFilter;
import com.RutaDelSabor.ruta.security.UserDetailsServiceImpl;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtRequestFilter jwtRequestFilter;
    private final UserDetailsServiceImpl userDetailsService;

    public SecurityConfig(JwtRequestFilter jwtRequestFilter, UserDetailsServiceImpl userDetailsService) {
        this.jwtRequestFilter = jwtRequestFilter;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 1. CORS: Permite que el front se conecte
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // 2. CSRF: Deshabilitado para APIs REST
            .csrf(csrf -> csrf.disable())
            
            .authorizeHttpRequests(auth -> auth
                // --- RUTAS PÚBLICAS ---
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // Preflight checks
                .requestMatchers("/api/auth/**").permitAll()     // Login y Registro
                .requestMatchers("/api/webhook/**").permitAll()  // Chatbot
                .requestMatchers("/images/**", "/icon/**", "/css/**", "/js/**").permitAll()

                // --- LECTURA PÚBLICA ---
                .requestMatchers(HttpMethod.GET, "/api/productos/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/categorias/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/menu").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/comentarios").permitAll()

                // --- ÁREA ADMIN (Lectura y Gestión básica) ---
                .requestMatchers("/api/admin/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_VENDEDOR")
                
                // --- GESTIÓN DE PRODUCTOS (Crear/Editar = Admin y Vendedor) ---
                .requestMatchers(HttpMethod.POST, "/api/productos/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_VENDEDOR")
                .requestMatchers(HttpMethod.PUT, "/api/productos/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_VENDEDOR")
                
                // --- ELIMINAR (Solo Admin) ---
                .requestMatchers(HttpMethod.DELETE, "/api/productos/**").hasAuthority("ROLE_ADMIN")

                // --- ROLES ESPECÍFICOS ---
                .requestMatchers("/api/vendedor/**").hasAuthority("ROLE_VENDEDOR")
                .requestMatchers("/api/delivery/**").hasAuthority("ROLE_DELIVERY")

                .anyRequest().authenticated()
            )
            .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // --- CONFIGURACIÓN CORS (CRÍTICO PARA CONEXIÓN) ---
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Permitir orígenes (Ajusta según tus puertos)
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:5500",      // Tu Front local
            "http://127.0.0.1:5500",      // Tu Front local (IP)
            "http://localhost:8080",      // Postman / Backend local
            "https://larutadelsaborbackend-production.up.railway.app", // Producción Backend
            "*" // OJO: Usar "*" solo para desarrollo rápido si sigue fallando
        ));

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin"));
        configuration.setAllowCredentials(true); // Permitir cookies/credenciales si es necesario

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}