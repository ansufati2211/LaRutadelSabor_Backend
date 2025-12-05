package com.RutaDelSabor.ruta.models.entities;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonIgnore; // <--- IMPORTANTE
@Entity
@Table(name = "comprobante")
public class Comprobante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tipo_comprobante", nullable = false, length = 20)
    private String tipoComprobante;

    @Column(length = 10)
    private String serie;

    @Column(length = 20)
    private String numero;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "fecha_emision")
    private Date fechaEmision;

    @Column(name = "monto_total", precision = 10, scale = 2)
    private BigDecimal montoTotal;

    @Column(length = 11)
    private String ruc;

    @Column(name = "aud_anulado", nullable = false)
    private boolean audAnulado = false;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", nullable = false, updatable = false)
    private Date createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_at", nullable = false)
    private Date updatedAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false, unique = true)
    @JsonIgnore
    private Pedido pedido;

    public Comprobante() {}

    @PrePersist
    protected void onCreate() {
        Date now = new Date();
        createdAt = now;
        updatedAt = now;
        if (fechaEmision == null) fechaEmision = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Date();
    }

    // --- Getters y Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTipoComprobante() { return tipoComprobante; }
    public void setTipoComprobante(String tipoComprobante) { this.tipoComprobante = tipoComprobante; }

    public String getSerie() { return serie; }
    public void setSerie(String serie) { this.serie = serie; }

    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }

    public Date getFechaEmision() { return fechaEmision; }
    public void setFechaEmision(Date fechaEmision) { this.fechaEmision = fechaEmision; }

    public BigDecimal getMontoTotal() { return montoTotal; }
    public void setMontoTotal(BigDecimal montoTotal) { this.montoTotal = montoTotal; }

    public String getRuc() { return ruc; }
    public void setRuc(String ruc) { this.ruc = ruc; }

    public boolean isAudAnulado() { return audAnulado; }
    public void setAudAnulado(boolean audAnulado) { this.audAnulado = audAnulado; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    public Pedido getPedido() { return pedido; }
    public void setPedido(Pedido pedido) { this.pedido = pedido; }
}