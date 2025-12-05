package com.RutaDelSabor.ruta.models.entities;

import java.math.BigDecimal;
import java.util.Date;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore; // <--- IMPORTANTE
@Entity
@Table(name = "metodo_pago")
public class MetodoPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "metodo_pago", nullable = false)
    private String metodoPago;

    @Column(precision = 10, scale = 2)
    private BigDecimal monto;
    
    @Column(name = "fecha_pago")
    private Date fechaPago;
    
    @Column(name = "estado_pago")
    private String estadoPago;

    @Column(name = "aud_anulado", nullable = false)
    private boolean audAnulado = false;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", nullable = false, updatable = false)
    private Date createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_at", nullable = false)
    private Date updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false, unique = true)
    @JsonIgnore
    private Pedido pedido;

    public MetodoPago() {}

    @PrePersist
    protected void onCreate() {
        createdAt = new Date();
        updatedAt = new Date();
        if (estadoPago == null) estadoPago = "PENDIENTE";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Date();
    }

    // --- Getters y Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }

    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }

    public Date getFechaPago() { return fechaPago; }
    public void setFechaPago(Date fechaPago) { this.fechaPago = fechaPago; }

    public String getEstadoPago() { return estadoPago; }
    public void setEstadoPago(String estadoPago) { this.estadoPago = estadoPago; }

    public boolean isAudAnulado() { return audAnulado; }
    public void setAudAnulado(boolean audAnulado) { this.audAnulado = audAnulado; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    public Pedido getPedido() { return pedido; }
    public void setPedido(Pedido pedido) { this.pedido = pedido; }
}