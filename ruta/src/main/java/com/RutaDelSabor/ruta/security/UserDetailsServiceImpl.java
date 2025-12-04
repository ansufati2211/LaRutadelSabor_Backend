package com.RutaDelSabor.ruta.security;

import com.RutaDelSabor.ruta.models.dao.IClienteDAO;
import com.RutaDelSabor.ruta.models.entities.Cliente;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private IClienteDAO clienteRepository;

    @Override
    @Transactional(readOnly = true) // Vital para leer el Rol (Lazy Loading) sin cerrar la sesión
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        // 1. Buscar usuario por correo
        Cliente cliente = clienteRepository.findByCorreo(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        // 2. Validar que tenga rol asignado
        if (cliente.getRol() == null) {
            System.err.println("ERROR CRÍTICO: El usuario " + username + " existe pero no tiene rol.");
            throw new UsernameNotFoundException("El usuario no tiene roles asignados");
        }

        // 3.dsdsd Obtener nombre del rol (Asegurando consistencia)
        // Nota: En tu entidad Rol, el campo es 'name', así que usamos getName()
        String nombreRolBD = cliente.getRol().getName(); 

        if (nombreRolBD == null || nombreRolBD.isEmpty()) {
            throw new UsernameNotFoundException("El rol del usuario está vacío en la base de datos");
        }

        // 4. Normalización de Seguridad (Tu lógica de "Red de Seguridad")
        // Esto arregla el error 403: Garantiza que SIEMPRE empiece con ROLE_
        String nombreRolFinal = nombreRolBD.toUpperCase().trim();

        if (!nombreRolFinal.startsWith("ROLE_")) {
            nombreRolFinal = "ROLE_" + nombreRolFinal;
        }

        // Log para depuración (Puedes quitarlo en producción)
        System.out.println("LOGIN EXITOSO: Usuario: " + username + " | Rol Final: " + nombreRolFinal);

        // 5. Crear la autoridad
        GrantedAuthority authority = new SimpleGrantedAuthority(nombreRolFinal);

        // 6. Retornar el usuario de Spring Security
        // Usamos cliente.getContraseña() porque así se llama en tu entidad Cliente
        return new User(cliente.getCorreo(), cliente.getContraseña(), Collections.singletonList(authority));
    }
}