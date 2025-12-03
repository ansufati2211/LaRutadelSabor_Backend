package com.RutaDelSabor.ruta.dto;

import jakarta.validation.constraints.*;

public class RegisterEmployeeRequest {
    @NotBlank private String nombre;
    @NotBlank private String apellido;
    @NotBlank @Email private String correo;
    @NotBlank @Size(min = 6) private String password;
    private String telefono;
    @NotBlank private String rol; // Nuevo campo para especificar el rol (VENDEDOR, DELIVERY, ADMIN)

    // Getters y Setters
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public String getPassword() { return password; }
    public void setPassword(String contraseña) { this.password = contraseña; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }
}