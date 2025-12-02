package com.RutaDelSabor.ruta.services;

import com.RutaDelSabor.ruta.dto.LoginRequest;
import com.RutaDelSabor.ruta.dto.RegisterRequest;
import com.RutaDelSabor.ruta.models.dao.IClienteDAO;
import com.RutaDelSabor.ruta.models.dao.IRolDAO;
import com.RutaDelSabor.ruta.models.entities.Cliente;
import com.RutaDelSabor.ruta.models.entities.Rol;
import com.RutaDelSabor.ruta.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private IClienteDAO clienteRepository;

    @Autowired
    private IRolDAO rolRepository;

    // REGISTRO DE CLIENTES
    public Cliente register(RegisterRequest request) {
        if (clienteRepository.findByCorreo(request.getCorreo()).isPresent()) {
            throw new RuntimeException("El correo electrónico ya está registrado");
        }
        
        // CORRECCIÓN CRÍTICA: Buscar "ROLE_USER" en vez de "USER"
        Rol userRol = rolRepository.findByName("ROLE_USER") 
            .orElseThrow(() -> new RuntimeException("Error: Rol 'ROLE_USER' no encontrado en la BD."));

        Cliente nuevoCliente = new Cliente();
        nuevoCliente.setNombre(request.getNombre());
        nuevoCliente.setApellido(request.getApellido());
        nuevoCliente.setCorreo(request.getCorreo());
        if (request.getTelefono() != null) {
             nuevoCliente.setTelefono(String.valueOf(request.getTelefono()));
        }
        // passwordEncoder ahora es NoOp (texto plano) gracias al cambio anterior
        nuevoCliente.setContraseña(passwordEncoder.encode(request.getContraseña()));
        nuevoCliente.setRol(userRol);

        return clienteRepository.save(nuevoCliente);
    }
    
    // LOGIN
    public String login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getCorreo(), request.getContraseña())
        );

        final UserDetails userDetails = userDetailsService.loadUserByUsername(request.getCorreo());
        return jwtUtil.generateToken(userDetails);
    }

    // REGISTRO DE EMPLEADOS (ADMIN, ETC)
    public com.RutaDelSabor.ruta.models.entities.Cliente registerEmployee(com.RutaDelSabor.ruta.dto.RegisterEmployeeRequest request) {
        if (clienteRepository.findByCorreo(request.getCorreo()).isPresent()) {
            throw new RuntimeException("El correo electrónico ya está registrado");
        }

        // Asegurar prefijo ROLE_
        String nombreRol = request.getRol().toUpperCase().trim();
        if (!nombreRol.startsWith("ROLE_")) {
            nombreRol = "ROLE_" + nombreRol;
        }

        String finalNombreRol = nombreRol;
        Rol rolAsignado = rolRepository.findByName(finalNombreRol)
            .orElseThrow(() -> new RuntimeException("Error: Rol '" + finalNombreRol + "' no encontrado."));

        com.RutaDelSabor.ruta.models.entities.Cliente nuevoEmpleado = new com.RutaDelSabor.ruta.models.entities.Cliente();
        nuevoEmpleado.setNombre(request.getNombre());
        nuevoEmpleado.setApellido(request.getApellido());
        nuevoEmpleado.setCorreo(request.getCorreo());
        if (request.getTelefono() != null) {
            nuevoEmpleado.setTelefono(String.valueOf(request.getTelefono()));
        }
        nuevoEmpleado.setContraseña(passwordEncoder.encode(request.getContraseña()));
        nuevoEmpleado.setRol(rolAsignado);

        return clienteRepository.save(nuevoEmpleado);
    }
}