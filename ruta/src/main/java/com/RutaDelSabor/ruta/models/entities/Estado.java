package com.RutaDelSabor.ruta.models.entities;

import java.util.Date;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
@Entity
@Table(name = "historial_estado_pedido")
public class Estado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tipo_estado", nullable = false)
    private String tipoEstado;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "fecha_hora_cambio", nullable = false, updatable = false)
    private Date fechaHoraCambio;

    private String notas;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    @JsonIgnore
    private Pedido pedido;

    public Estado() {}

    @PrePersist
    protected void onCreate() {
        fechaHoraCambio = new Date();
    }

    // --- Getters y Setters Corregidos ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTipoEstado() { return tipoEstado; }
    public void setTipoEstado(String tipoEstado) { this.tipoEstado = tipoEstado; }

    public Date getFechaHoraCambio() { return fechaHoraCambio; }
    public void setFechaHoraCambio(Date fechaHoraCambio) { this.fechaHoraCambio = fechaHoraCambio; }
    
    public Pedido getPedido() { return pedido; }
    public void setPedido(Pedido pedido) { this.pedido = pedido; }

    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }
}