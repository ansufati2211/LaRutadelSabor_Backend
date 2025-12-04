package com.RutaDelSabor.ruta.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtRequestFilter.class);

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        log.debug("🔍 Procesando request: {} {}", request.getMethod(), requestURI);

        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt)) {
                log.debug("🔑 Token encontrado, validando...");

                if (validateToken(jwt)) {
                    String username = getUsernameFromJWT(jwt);
                    String role = getRoleFromJWT(jwt);

                    log.info("✅ Token válido - Usuario: {} | Rol del token: {}", username, role);

                    // Cargar el usuario completo desde la BD (con sus roles reales)
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    log.info("👤 UserDetails cargado - Authorities: {}", userDetails.getAuthorities());

                    // Crear la autenticación
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities() // Usa las authorities de la BD, no del token
                            );

                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    log.info("🎯 Autenticación establecida en SecurityContext para: {}", username);
                } else {
                    log.warn("❌ Token inválido");
                }
            } else {
                log.debug("ℹ️ No se encontró token en la request");
            }
        } catch (ExpiredJwtException ex) {
            log.error("⏰ Token JWT expirado: {}", ex.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Token expirado\",\"message\":\"" + ex.getMessage() + "\"}");
            return;
        } catch (Exception ex) {
            log.error("💥 Error al procesar token: {}", ex.getMessage(), ex);
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }

    private String getUsernameFromJWT(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(jwtSecret)
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }

    private String getRoleFromJWT(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(jwtSecret)
                    .parseClaimsJws(token)
                    .getBody();

            return (String) claims.get("role");
        } catch (Exception e) {
            log.warn("No se pudo extraer el rol del token: {}", e.getMessage());
            return null;
        }
    }

    private boolean validateToken(String authToken) {
        try {
            Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(authToken);
            return true;
        } catch (SignatureException ex) {
            log.error("❌ Firma JWT inválida");
        } catch (MalformedJwtException ex) {
            log.error("❌ Token JWT malformado");
        } catch (ExpiredJwtException ex) {
            log.error("❌ Token JWT expirado");
        } catch (Exception ex) {
            log.error("❌ Error validando token: {}", ex.getMessage());
        }
        return false;
    }
}