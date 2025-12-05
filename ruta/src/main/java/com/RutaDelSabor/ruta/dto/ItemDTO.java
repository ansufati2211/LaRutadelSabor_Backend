package com.RutaDelSabor.ruta.dto;

import java.math.BigDecimal;

public class ItemDTO {
    private Long id;        // Unificamos: en backend y frontend será 'id'
    private String nombre;
    private BigDecimal precio;
    private String imagen;
    private int cantidad;
    private int stock;

    public ItemDTO() {}

    // Constructor simple (Para compatibilidad con lógica antigua si se requiere)
    public ItemDTO(Long id, int cantidad) {
        this.id = id;
        this.cantidad = cantidad;
    }

    // Constructor completo (Para el Webhook y el Carrito)
    public ItemDTO(Long id, String nombre, BigDecimal precio, String imagen, int cantidad, int stock) {
        this.id = id;
        this.nombre = nombre;
        this.precio = precio;
        this.imagen = imagen;
        this.cantidad = cantidad;
        this.stock = stock;
    }

    // --- GETTERS Y SETTERS ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    // ¡IMPORTANTE! Este método soluciona el error en PedidoServiceImpl
    // PedidoServiceImpl busca "getProductoId()", así que se lo damos.
    public Long getProductoId() { return id; }
    public void setProductoId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }

    public String getImagen() { return imagen; }
    public void setImagen(String imagen) { this.imagen = imagen; }

    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
}