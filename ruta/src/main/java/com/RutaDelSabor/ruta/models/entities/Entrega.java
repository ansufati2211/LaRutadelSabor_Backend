package com.RutaDelSabor.ruta.models.entities;

import java.util.Date;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore; // <--- IMPORTANTE
@Entity
@Table(name = "entrega")
public class Entrega {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "metodo_entrega", nullable = false)
    private String metodoEntrega;

    @Column(name = "fecha_hora_estimada")
    private Date fechaHoraEstimada;
    
    @Column(name = "fecha_hora_real")
    private Date fechaHoraReal;
    
    @Column(name = "estado_entrega")
    private String estadoEntrega;

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

    public Entrega() {}

    @PrePersist
    protected void onCreate() {
        createdAt = new Date();
        updatedAt = new Date();
        if (estadoEntrega == null) estadoEntrega = "PENDIENTE";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Date();
    }

    // --- Getters y Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getMetodoEntrega() { return metodoEntrega; }
    public void setMetodoEntrega(String metodoEntrega) { this.metodoEntrega = metodoEntrega; }

    public Date getFechaHoraEstimada() { return fechaHoraEstimada; }
    public void setFechaHoraEstimada(Date fechaHoraEstimada) { this.fechaHoraEstimada = fechaHoraEstimada; }

    public Date getFechaHoraReal() { return fechaHoraReal; }
    public void setFechaHoraReal(Date fechaHoraReal) { this.fechaHoraReal = fechaHoraReal; }

    public String getEstadoEntrega() { return estadoEntrega; }
    public void setEstadoEntrega(String estadoEntrega) { this.estadoEntrega = estadoEntrega; }

    public boolean isAudAnulado() { return audAnulado; }
    public void setAudAnulado(boolean audAnulado) { this.audAnulado = audAnulado; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    public Pedido getPedido() { return pedido; }
    public void setPedido(Pedido pedido) { this.pedido = pedido; }
}