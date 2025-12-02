package com.RutaDelSabor.ruta.dto;

// Añadir importaciones para @NotBlank, @Email, @Size si usas validation
import jakarta.validation.constraints.*; // Ejemplo si usas validation

public class RegisterRequest {
    @NotBlank
    private String nombre;
    @NotBlank
    private String apellido;
    @NotBlank @Email
    private String correo;
    @NotBlank @Size(min = 6)
    private String contraseña; // Cambiar a 'password' por convención
    private String telefono;
    // Otros campos si son necesarios para el registro

    // Getters y Setters
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    // ... para todos los campos
    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public String getContraseña() { return contraseña; }
    public void setContraseña(String contraseña) { this.contraseña = contraseña; }
     public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
}