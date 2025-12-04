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

import static org.springframework.security.config.Customizer.withDefaults;

@SuppressWarnings("unused")
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
            // 1. Configuración CORS Integrada (Crítico para que el front se conecte)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // Permitir preflight requests (OPTIONS) siempre
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                
                // --- RUTAS PÚBLICAS (Login y Webhooks) ---
                // Importante: Permitimos todo en webhook para evitar bloqueos por método o headers
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/webhook/**").permitAll()

                // --- REGLAS DE ADMIN (Usamos hasAuthority para aceptar 'ADMIN' sin prefijo) ---
                .requestMatchers("/api/productos/admin/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_VENDEDOR", "ADMIN", "VENDEDOR")
                .requestMatchers("/api/categorias/admin/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_VENDEDOR", "ADMIN", "VENDEDOR")
                .requestMatchers("/api/admin/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_VENDEDOR", "ROLE_DELIVERY", "ADMIN", "VENDEDOR", "DELIVERY")
                
                // Métodos de escritura protegidos globalmente (POST, PUT, DELETE) para productos
                .requestMatchers(HttpMethod.POST, "/api/productos/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_VENDEDOR", "ADMIN", "VENDEDOR")
                .requestMatchers(HttpMethod.PUT, "/api/productos/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_VENDEDOR", "ADMIN", "VENDEDOR")
                .requestMatchers(HttpMethod.DELETE, "/api/productos/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_VENDEDOR", "ADMIN", "VENDEDOR")

                // --- RUTAS PÚBLICAS DE LECTURA ---
                .requestMatchers(HttpMethod.GET, "/api/productos/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/categorias/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/menu").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/comentarios").permitAll()

                // Cualquier otra cosa requiere autenticación
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // Bean de Configuración CORS CORREGIDO
   @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // --- CAMBIO OBLIGATORIO: Usar * para permitir Dialogflow ---
        configuration.setAllowedOriginPatterns(List.of("*")); 
        
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}