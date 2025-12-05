package com.RutaDelSabor.ruta.models.entities;

import java.math.BigDecimal;
import java.util.Date;
import jakarta.persistence.*;

@Entity
@Table(name = "producto")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100, nullable = false)
    private String producto;

    @Column(length = 500)
    private String descripcion;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal precio;

    @Column(nullable = false)
    private int stock;

    // Aumentamos a TEXT para soportar URLs largas, pero filtramos en el GET
    @Column(columnDefinition = "TEXT") 
    private String imagen;

    @Column(name = "aud_anulado", nullable = false)
    private boolean audAnulado = false;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", nullable = false, updatable = false)
    private Date createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_at", nullable = false)
    private Date updatedAt;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    public Producto() {}

    @PrePersist
    protected void onCreate() {
        Date now = new Date();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Date();
    }

    // --- Getters y Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getProducto() { return producto; }
    public void setProducto(String producto) { this.producto = producto; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    // --- GETTER MODIFICADO (Protección) ---
    public String getImagen() {
        // Si la imagen es sospechosamente larga (posible Base64 corrupto), devolvemos una genérica
        if (this.imagen != null && this.imagen.length() > 500) {
            return "https://placehold.co/100x100?text=Imagen+Error";
        }
        return imagen;
    }
    // ESTE ES EL SETTER QUE FALTABA:
    public void setImagen(String imagen) { this.imagen = imagen; }
    // -------------------------------------

    public boolean isAudAnulado() { return audAnulado; }
    public void setAudAnulado(boolean audAnulado) { this.audAnulado = audAnulado; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }
}