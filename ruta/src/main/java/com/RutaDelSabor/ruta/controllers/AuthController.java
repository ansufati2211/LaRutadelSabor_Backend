package com.RutaDelSabor.ruta.controllers;

import com.RutaDelSabor.ruta.dto.AuthResponse;
import com.RutaDelSabor.ruta.dto.ErrorResponseDTO; // DTO para errores
import com.RutaDelSabor.ruta.dto.LoginRequest;
import com.RutaDelSabor.ruta.dto.RegisterRequest;
import com.RutaDelSabor.ruta.services.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus; // Para HttpStatus
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
// @CrossOrigin(origins = "*") // Mejor configurar globalmente en WebConfig
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            authService.register(registerRequest);
            // Devolver un mensaje claro, el login debe ser un paso separado
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body("Usuario registrado exitosamente. Por favor, inicie sesión.");
        } catch (RuntimeException e) {
            // Devolver error específico si el correo ya existe
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponseDTO(e.getMessage()));
        } catch (Exception e) {
            // Error genérico
             e.printStackTrace(); // Loggear error
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDTO("Ocurrió un error durante el registro."));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            String token = authService.login(loginRequest);
            // Devolver el token en la respuesta
            return ResponseEntity.ok(new AuthResponse(token, "Login exitoso"));
        } catch (RuntimeException e) {
            // Error específico para credenciales inválidas
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED) // 401 Unauthorized
                    .body(new ErrorResponseDTO("Correo o contraseña incorrectos."));
        } catch (Exception e) {
            // Error genérico
             e.printStackTrace(); // Loggear error
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDTO("Ocurrió un error durante el login."));
        }
    }
    // Endpoint protegido solo para ADMIN
    @PostMapping("/register/employee")
    //@org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
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