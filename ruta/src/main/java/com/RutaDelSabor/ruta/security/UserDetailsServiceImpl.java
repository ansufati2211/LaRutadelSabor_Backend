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
import org.springframework.transaction.annotation.Transactional; // <--- IMPORTANTE

import java.util.Collections;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private IClienteDAO clienteRepository;

    @Override
    @Transactional(readOnly = true) // <--- CRÍTICO: Mantiene la sesión de base de datos abierta para leer el Rol
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        // 1. Buscar usuario
        Cliente cliente = clienteRepository.findByCorreo(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        // 2. Validar que tenga rol
        if (cliente.getRol() == null) {
            System.out.println("ERROR LOGIN: El usuario " + username + " no tiene rol asignado.");
            throw new UsernameNotFoundException("El usuario no tiene un rol asignado");
        }

        // 3. Obtener nombre del rol (OJO: Verifica si en tu Entidad Rol es .getName() o .getNombre())
        // Si tu clase Rol tiene "private String nombre;", cambia esto a .getNombre()
        String nombreRolBD = cliente.getRol().getName(); // Asumo que es getNombre() por tus logs anteriores

        if (nombreRolBD == null) {
            throw new UsernameNotFoundException("El rol del usuario está vacío");
        }

        // 4. Normalización (Tu lógica estaba bien, agregamos logs para depurar)
        String nombreRolFinal = nombreRolBD.toUpperCase().trim();

        if (!nombreRolFinal.startsWith("ROLE_")) {
            nombreRolFinal = "ROLE_" + nombreRolFinal;
        }

        System.out.println("LOGIN INFO: Usuario: " + username + " | Rol en BD: " + nombreRolBD + " | Authority generada: " + nombreRolFinal);

        // 5. Crear la autoridad
        GrantedAuthority authority = new SimpleGrantedAuthority(nombreRolFinal);

        return new User(cliente.getCorreo(), cliente.getContraseña(), Collections.singletonList(authority));
    }
}