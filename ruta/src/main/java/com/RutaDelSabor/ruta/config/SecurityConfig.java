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
@EnableMethodSecurity // Permite usar @PreAuthorize en controladores si lo necesitas a futuro
public class SecurityConfig {

    @Autowired
    private JwtRequestFilter jwtRequestFilter;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    // Mantenemos NoOpPasswordEncoder porque tus usuarios en BD tienen claves sin encriptar (texto plano).
    // Si cambias a BCrypt, tendrás que actualizar todas las contraseñas en la BD.
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
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 1. Permitir Preflight (OPTIONS) - Vital para que el Frontend no de error de CORS
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/error", "/favicon.ico", "/assets/**", "/images/**", "/css/**", "/js/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        // 2. Rutas Públicas (Login y Webhooks)
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/webhook/**").permitAll()

                        // 3. TIENDA PÚBLICA (Cualquiera puede ver productos y menú sin loguearse)
                        .requestMatchers(HttpMethod.GET, "/api/productos/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/categorias/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/menu/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/comentarios/**").permitAll()

                        // 4. GESTIÓN DE PRODUCTOS (Solo Admin y Vendedor pueden crear/editar/borrar)
                        // Usamos hasAnyAuthority porque en tu BD los roles ya incluyen "ROLE_"
                        .requestMatchers(HttpMethod.POST, "/api/productos/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_VENDEDOR")
                        .requestMatchers(HttpMethod.PUT, "/api/productos/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_VENDEDOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/productos/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_VENDEDOR")

                        // Endpoints explícitos de administración
                        .requestMatchers("/api/productos/admin/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_VENDEDOR")
                        .requestMatchers("/api/categorias/admin/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_VENDEDOR")

                        // 5. Panel General de Administración (Incluye Pedidos)
                        .requestMatchers("/api/admin/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_VENDEDOR", "ROLE_DELIVERY")

                        // 6. Todo lo demás requiere login (ej: ver perfil de cliente, hacer pedido)
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Usamos setAllowedOriginPatterns("*") para permitir Railway, Localhost, Celulares, etc.
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin", "Access-Control-Allow-Origin"));
        configuration.setAllowCredentials(true);
        // Exponemos el header Authorization para que el frontend pueda leerlo si es necesario
        configuration.setExposedHeaders(List.of("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

}