package com.RutaDelSabor.ruta.controllers;

import com.RutaDelSabor.ruta.dto.AuthResponse;
import com.RutaDelSabor.ruta.dto.ErrorResponseDTO;
import com.RutaDelSabor.ruta.dto.LoginRequest;
import com.RutaDelSabor.ruta.dto.RegisterRequest;
import com.RutaDelSabor.ruta.security.JwtUtil; // Importante
import com.RutaDelSabor.ruta.services.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager; // Importante
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken; // Importante
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails; // Importante
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private AuthenticationManager authenticationManager; // El motor de seguridad

    @Autowired
    private JwtUtil jwtUtil; // Tu generador de tokens arreglado

    // --- REGISTRO (Se mantiene igual, delegado al servicio) ---
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            authService.register(registerRequest);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body("Usuario registrado exitosamente. Por favor, inicie sesión.");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponseDTO(e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDTO("Ocurrió un error durante el registro."));
        }
    }

    // --- LOGIN (Lógica Mejorada y Fusionada) ---
    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            // 1. Autenticar usando Spring Security
            // Esto llama internamente a UserDetailsServiceImpl.loadUserByUsername()
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getCorreo(),
                            loginRequest.getPassword()
                    )
            );

            // 2. Si llegamos aquí, la contraseña es correcta.
            // Obtenemos el usuario cargado (que YA TIENE los roles gracias a tu UserDetailsServiceImpl)
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            // 3. Generar el Token (Tu JwtUtil ahora meterá los roles dentro)
            String token = jwtUtil.generateToken(userDetails);

            // 4. Devolver respuesta
            return ResponseEntity.ok(new AuthResponse(token, "Login exitoso"));

        } catch (BadCredentialsException e) {
            // Este error lo lanza authenticationManager si la contraseña está mal
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponseDTO("Correo o contraseña incorrectos."));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDTO("Error técnico en el login: " + e.getMessage()));
        }
    }

    // --- REGISTRO DE EMPLEADOS (Protegido) ---
    @PostMapping("/register/employee")
    public ResponseEntity<?> registerEmployee(@Valid @RequestBody com.RutaDelSabor.ruta.dto.RegisterEmployeeRequest request) {
        try {
            authService.registerEmployee(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body("Empleado registrado exitosamente con rol: " + request.getRol());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponseDTO(e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDTO("Error al registrar empleado."));
        }
    }
}