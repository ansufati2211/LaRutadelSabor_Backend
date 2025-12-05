package com.RutaDelSabor.ruta.models.entities;

import java.util.Date;
import java.util.List;
import java.math.BigDecimal;
import java.util.ArrayList;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "pedido")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "fecha_pedido", nullable = false)
    private Date fechaPedido;

    @Column(length = 255)
    private String direccion;

    @Column(length = 255)
    private String referencia;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "tiempo_estimado")
    private Date tiempoEstimado;

    @Column(precision = 10, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "monto_agregado", precision = 10, scale = 2)
    private BigDecimal montoAgregado = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "estado_actual", length = 50)
    private String estadoActual = "RECIBIDO";

    @Column(name = "ultimos_digitos_tarjeta", length = 4)
    private String ultimosDigitosTarjeta;
    
    @Column(name = "titular_tarjeta", length = 100)
    private String titularTarjeta;

    @Column(name = "numero_yape", length = 20)
    private String numeroYape;

    @Column(name = "aud_anulado", nullable = false)
    private boolean audAnulado = false;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", nullable = false, updatable = false)
    private Date createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_at", nullable = false)
    private Date updatedAt;

    // --- Relaciones ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    @JsonIgnore
    private Cliente cliente;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PedidoDetallado> detalles = new ArrayList<>();

    @OneToOne(mappedBy = "pedido", cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = true)
    private Comprobante comprobante;
    
    @OneToOne(mappedBy = "pedido", cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = true)
    private MetodoPago pago;

    @OneToOne(mappedBy = "pedido", cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = true)
    private Entrega entrega;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("fechaHoraCambio DESC")
    private List<Estado> historialEstados = new ArrayList<>();
    
    public Pedido() {}

    @PrePersist
    protected void onCreate() {
        Date now = new Date();
        createdAt = now;
        updatedAt = now;
        if (fechaPedido == null) fechaPedido = now;
        if (estadoActual == null) estadoActual = "RECIBIDO";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Date();
    }

    public void addEstadoHistorial(Estado nuevoEstado) {
        if (this.historialEstados == null) {
            this.historialEstados = new ArrayList<>();
        }
        nuevoEstado.setPedido(this);
        this.historialEstados.add(nuevoEstado);
        this.estadoActual = nuevoEstado.getTipoEstado(); // CORREGIDO
        this.updatedAt = new Date();
    }

    // --- Getters y Setters Corregidos ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Date getFechaPedido() { return fechaPedido; }
    public void setFechaPedido(Date fechaPedido) { this.fechaPedido = fechaPedido; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }

    public Date getTiempoEstimado() { return tiempoEstimado; }
    public void setTiempoEstimado(Date tiempoEstimado) { this.tiempoEstimado = tiempoEstimado; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public BigDecimal getMontoAgregado() { return montoAgregado; }
    public void setMontoAgregado(BigDecimal montoAgregado) { this.montoAgregado = montoAgregado; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public String getEstadoActual() { return estadoActual; }
    public void setEstadoActual(String estadoActual) { this.estadoActual = estadoActual; }
    
    public String getUltimosDigitosTarjeta() { return ultimosDigitosTarjeta; }
    public void setUltimosDigitosTarjeta(String ultimosDigitosTarjeta) { this.ultimosDigitosTarjeta = ultimosDigitosTarjeta; }
    
    public String getTitularTarjeta() { return titularTarjeta; }
    public void setTitularTarjeta(String titularTarjeta) { this.titularTarjeta = titularTarjeta; }

    public String getNumeroYape() { return numeroYape; }
    public void setNumeroYape(String numeroYape) { this.numeroYape = numeroYape; }

    public boolean isAudAnulado() { return audAnulado; }
    public void setAudAnulado(boolean audAnulado) { this.audAnulado = audAnulado; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }

    public List<PedidoDetallado> getDetalles() { return detalles; }
    public void setDetalles(List<PedidoDetallado> detalles) { this.detalles = detalles; }

    public Comprobante getComprobante() { return comprobante; }
    public void setComprobante(Comprobante comprobante) { this.comprobante = comprobante; }
    
    public MetodoPago getPago() { return pago; }
    public void setPago(MetodoPago pago) { this.pago = pago; }

    public Entrega getEntrega() { return entrega; }
    public void setEntrega(Entrega entrega) { this.entrega = entrega; }

    public List<Estado> getHistorialEstados() { return historialEstados; }
    public void setHistorialEstados(List<Estado> historialEstados) { this.historialEstados = historialEstados; }
}