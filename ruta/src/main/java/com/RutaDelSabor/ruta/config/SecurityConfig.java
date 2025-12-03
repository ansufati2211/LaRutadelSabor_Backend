SECURITYCONFIG.JS
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
import static org.springframework.security.config.Customizer.withDefaults;

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
            .cors(withDefaults())
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // 1. REGLAS CRÍTICAS (Orden específico: De lo más restrictivo a lo más general)
                
                // Permitir preflight requests (CORS) para que el navegador no bloquee
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                
                // Login y Auth siempre públicos
                .requestMatchers("/api/auth/**").permitAll()
                
                // 2. PROTEGER RUTAS DE ADMIN DENTRO DE PRODUCTOS
                // Esto soluciona el 403: Forzamos autenticación antes de que la regla pública capture la ruta
                .requestMatchers("/api/productos/admin/**").hasAnyRole("ADMIN", "VENDEDOR") 
                .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "VENDEDOR", "DELIVERY")

                // 3. RUTAS PÚBLICAS (Menú y Catálogo para clientes)
                // Ahora sí, el resto de productos son públicos
                .requestMatchers(HttpMethod.GET, "/api/productos/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/categorias/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/menu").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/comentarios").permitAll()
                
                // Webhook
                .requestMatchers(HttpMethod.POST, "/api/webhook/dialogflow").permitAll()

                // Todo lo demás requiere login
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authenticationProvider(authenticationProvider());

        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}