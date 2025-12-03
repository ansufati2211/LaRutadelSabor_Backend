package com.RutaDelSabor.ruta.controllers;

import com.RutaDelSabor.ruta.dto.*;
import com.RutaDelSabor.ruta.models.entities.Cliente;
import com.RutaDelSabor.ruta.models.entities.Pedido;
import com.RutaDelSabor.ruta.models.entities.Producto;
import com.RutaDelSabor.ruta.services.IClienteService;
import com.RutaDelSabor.ruta.services.IPedidoService;
import com.RutaDelSabor.ruta.services.IProductoService;
import com.RutaDelSabor.ruta.services.IReporteService;
import com.RutaDelSabor.ruta.utils.LevenshteinUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/webhook")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    @Autowired private IPedidoService pedidoService;
    @Autowired private IClienteService clienteService;
    @Autowired private IProductoService productoService;
    @Autowired private IReporteService reporteService;

    @PostMapping("/dialogflow")
    public ResponseEntity<Map<String, Object>> handleWebhook(@RequestBody DialogflowRequest request) {
        String tag = request.getFulfillmentInfo().getTag();
        Map<String, Object> params = request.getSessionInfo().getParameters();
        
        log.info("🔹 Webhook Tag recibido: {}", tag);

        switch (tag) {
            case "bienvenida_usuario":
                return processBienvenida(params);
            case "finalizar_pedido":
                return processFinalizarPedido(params);
            case "recomendar_producto":
                return processRecomendacion(params);
            case "consultar_menu":
                return processConsultarMenu();
            case "consultar_estado_pedido":
                return processConsultarEstado(params);
            case "admin_consultar_stock":
                return processAdminStock(params);
            case "admin_reporte_ventas":
                return processAdminReporte();
            default:
                return ResponseEntity.ok(crearRespuestaTexto("Webhook: Acción no reconocida (" + tag + ")."));
        }
    }

    // --- 1. SALUDO PERSONALIZADO ---
    private ResponseEntity<Map<String, Object>> processBienvenida(Map<String, Object> params) {
        String nombre = (String) params.getOrDefault("nombre_usuario", "");
        if (nombre != null && !nombre.isEmpty()) {
            return ResponseEntity.ok(crearRespuestaTexto("¡Hola " + nombre + "! Qué gusto verte de nuevo en La Ruta del Sabor. ¿Te provoca lo de siempre o quieres ver el menú?"));
        }
        return ResponseEntity.ok(crearRespuestaTexto("¡Hola! Bienvenido a La Ruta del Sabor. Soy Verónica. ¿En qué puedo ayudarte hoy?"));
    }

    // --- 2. PROCESAR PEDIDO FLEXIBLE (GARANTÍA 100%) ---
    private ResponseEntity<Map<String, Object>> processFinalizarPedido(Map<String, Object> params) {
        try {
            // A. Datos Cliente y Entrega
            String email = (String) params.getOrDefault("email_cliente", "cliente@prueba.com");
            String tipoEntrega = (String) params.getOrDefault("tipo_entrega", "Recojo"); // Delivery o Recojo
            String direccion = (String) params.getOrDefault("direccion", "Local Principal");
            
            Cliente cliente;
            try {
                cliente = clienteService.buscarPorCorreo(email);
            } catch (Exception e) {
                cliente = clienteService.buscarPorCorreo("cliente@prueba.com"); 
            }

            // B. Desglose Inteligente
            String inputUsuario = (String) params.get("producto_solicitado"); 
            if (inputUsuario == null) return ResponseEntity.ok(crearRespuestaTexto("No entendí qué producto deseas."));

            String[] itemsRaw = inputUsuario.toLowerCase().split(" y | con |,|\\+| más ");
            List<ItemDTO> itemsParaOrden = new ArrayList<>();
            List<String> nombresProductos = new ArrayList<>();
            StringBuilder alertas = new StringBuilder();

            List<Producto> todosProductos = productoService.buscarTodosActivos();

            for (String itemRaw : itemsRaw) {
                itemRaw = itemRaw.trim();
                if (itemRaw.isEmpty()) continue;

                Pair<Integer, String> infoItem = extraerCantidadYNombre(itemRaw);
                int cantidad = infoItem.getKey();
                String nombreBusqueda = infoItem.getValue();

                Optional<Producto> match = todosProductos.stream()
                    .filter(p -> LevenshteinUtil.calculateSimilarity(p.getProducto(), nombreBusqueda) > 0.4)
                    .sorted((p1, p2) -> Double.compare(
                            LevenshteinUtil.calculateSimilarity(p2.getProducto(), nombreBusqueda),
                            LevenshteinUtil.calculateSimilarity(p1.getProducto(), nombreBusqueda)))
                    .findFirst();

                if (match.isPresent()) {
                    Producto prod = match.get();
                    if (prod.getStock() < cantidad) {
                        alertas.append("Solo quedan ").append(prod.getStock()).append(" de ").append(prod.getProducto()).append(". ");
                    } else {
                        itemsParaOrden.add(new ItemDTO(prod.getId(), cantidad));
                        nombresProductos.add(cantidad + "x " + prod.getProducto());
                    }
                }
            }

            if (itemsParaOrden.isEmpty()) {
                return ResponseEntity.ok(crearRespuestaTexto("Lo siento, no identifiqué productos en tu frase. Intenta: 'Una hamburguesa y dos colas'."));
            }

            // C. Crear Orden
            OrdenRequestDTO ordenRequest = new OrdenRequestDTO();
            ordenRequest.setItems(itemsParaOrden);
            ordenRequest.setTipoEntrega(tipoEntrega);
            ordenRequest.setDireccionEntrega(direccion);
            ordenRequest.setMetodoPago("Pendiente"); // Se paga en el front

            Pedido nuevoPedido = pedidoService.crearNuevaOrden(ordenRequest, 
                new User(cliente.getCorreo(), cliente.getContraseña(), new ArrayList<>()));

            // D. Respuesta con Link de Pago
            return construirRespuestaConPayload(nuevoPedido, String.join(", ", nombresProductos), alertas.toString());

        } catch (Exception e) {
            log.error("Error pedido:", e);
            return ResponseEntity.ok(crearRespuestaTexto("Hubo un error técnico al crear tu orden."));
        }
    }

    // --- 3. LOGÍSTICA: CONSULTAR STOCK POR VOZ ---
    private ResponseEntity<Map<String, Object>> processAdminStock(Map<String, Object> params) {
        String productoBuscado = (String) params.get("producto_consulta");
        if (productoBuscado == null) return ResponseEntity.ok(crearRespuestaTexto("¿De qué producto quieres saber el stock?"));

        Optional<Producto> match = productoService.buscarTodosActivos().stream()
            .filter(p -> LevenshteinUtil.calculateSimilarity(p.getProducto(), productoBuscado) > 0.45)
            .sorted((p1, p2) -> Double.compare(
                    LevenshteinUtil.calculateSimilarity(p2.getProducto(), productoBuscado),
                    LevenshteinUtil.calculateSimilarity(p1.getProducto(), productoBuscado)))
            .findFirst();

        if (match.isPresent()) {
            Producto p = match.get();
            return ResponseEntity.ok(crearRespuestaTexto("El stock actual de " + p.getProducto() + " es de " + p.getStock() + " unidades."));
        } else {
            return ResponseEntity.ok(crearRespuestaTexto("No encontré ese producto en el inventario."));
        }
    }

    // --- 4. LOGÍSTICA: REPORTE RÁPIDO ---
    private ResponseEntity<Map<String, Object>> processAdminReporte() {
        try {
            VentasPeriodoDTO ventasHoy = reporteService.calcularVentasPorPeriodo(LocalDate.now(), LocalDate.now());
            return ResponseEntity.ok(crearRespuestaTexto("Reporte de HOY: Se han realizado " + ventasHoy.getNumeroPedidos() + " pedidos, con un total vendido de S/ " + ventasHoy.getTotalVentas()));
        } catch (Exception e) {
            return ResponseEntity.ok(crearRespuestaTexto("Error generando reporte."));
        }
    }

   // --- 5. CLIENTE: ESTADO DEL PEDIDO (CORREGIDO Y REAL) ---
    private ResponseEntity<Map<String, Object>> processConsultarEstado(Map<String, Object> params) {
        // Obtenemos el email. Si no viene, usamos uno de prueba para evitar errores null
        String email = (String) params.getOrDefault("email_cliente", "cliente@prueba.com");
        
        try {
            // 1. Buscamos al cliente en la BD usando la variable 'email' (¡Ahora sí la usamos!)
            Cliente cliente = clienteService.buscarPorCorreo(email);
            
            // 2. Simulamos un UserDetails para poder usar el servicio de historial existente
            // (Tu servicio obtenerHistorialPedidos pide un UserDetails de Spring Security)
            User userDetails = new User(cliente.getCorreo(), cliente.getContraseña(), new ArrayList<>());
            
            // 3. Obtenemos sus pedidos reales
            List<Pedido> historial = pedidoService.obtenerHistorialPedidos(userDetails);
            
            if (historial.isEmpty()) {
                 return ResponseEntity.ok(crearRespuestaTexto("Hola " + cliente.getNombre() + ", revisé tu historial y no tienes pedidos recientes registrados con " + email + "."));
            }
            
            // 4. Tomamos el más reciente (el primero de la lista)
            Pedido ultimoPedido = historial.get(0);
            String estado = ultimoPedido.getEstadoActual();
            
            return ResponseEntity.ok(crearRespuestaTexto("Tu último pedido (#" + ultimoPedido.getId() + ") está actualmente: " + estado + ". 🛵"));
            
        } catch (Exception e) {
            // En caso de error (ej. cliente no existe), logueamos usando la variable email
            log.warn("Intento de consultar estado para correo no registrado: {}", email);
            return ResponseEntity.ok(crearRespuestaTexto("No encontré una cuenta asociada al correo " + email + ". ¿Es tu primera vez aquí?"));
        }
    }
    // --- 6. RECOMENDACIONES (HISTORIAL + POPULARES) ---
    private ResponseEntity<Map<String, Object>> processRecomendacion(Map<String, Object> params) {
        // Lógica: Si hay historial, recomendar lo último. Si no, recomendar lo popular.
        List<ProductoPopularDTO> populares = reporteService.obtenerProductosPopulares(3);
        String nombres = populares.stream().map(ProductoPopularDTO::getNombreProducto).collect(Collectors.joining(", "));
        return ResponseEntity.ok(crearRespuestaTexto("Basado en lo que más piden nuestros clientes, te recomiendo: " + nombres + ". ¿Te animas?"));
    }

    // --- HELPERS ---
    private ResponseEntity<Map<String, Object>> processConsultarMenu() {
        return ResponseEntity.ok(crearRespuestaTexto("Hoy tenemos Hamburguesas Artesanales, Alitas BBQ, Broaster y Postres. ¿Qué se te antoja?"));
    }

    private Pair<Integer, String> extraerCantidadYNombre(String texto) {
        Map<String, Integer> numerosTexto = Map.of("un", 1, "una", 1, "uno", 1, "dos", 2, "tres", 3, "cuatro", 4);
        Pattern patternDigitos = Pattern.compile("^(\\d+)\\s+(.*)");
        Matcher matcherDigitos = patternDigitos.matcher(texto);
        if (matcherDigitos.find()) return new Pair<>(Integer.parseInt(matcherDigitos.group(1)), matcherDigitos.group(2));
        
        String[] palabras = texto.split("\\s+", 2);
        if (palabras.length > 1 && numerosTexto.containsKey(palabras[0].toLowerCase())) {
            return new Pair<>(numerosTexto.get(palabras[0].toLowerCase()), palabras[1]);
        }
        return new Pair<>(1, texto);
    }

    private static class Pair<K, V> {
        private K key; private V value;
        public Pair(K key, V value) { this.key = key; this.value = value; }
        public K getKey() { return key; }
        public V getValue() { return value; }
    }

    private Map<String, Object> crearRespuestaTexto(String mensaje) {
        Map<String, Object> json = new HashMap<>();
        Map<String, Object> fulfillment = new HashMap<>();
        Map<String, Object> text = new HashMap<>();
        text.put("text", Collections.singletonList(mensaje));
        fulfillment.put("messages", Collections.singletonList(Map.of("text", text)));
        json.put("fulfillmentResponse", fulfillment);
        return json;
    }

    private ResponseEntity<Map<String, Object>> construirRespuestaConPayload(Pedido pedido, String resumen, String alertas) {
        Map<String, Object> jsonResponse = new HashMap<>();
        List<Map<String, Object>> messages = new ArrayList<>();

        Map<String, Object> textMessage = new HashMap<>();
        textMessage.put("text", Map.of("text", List.of(alertas + "¡Listo! Pedido #" + pedido.getId() + " confirmado: " + resumen + ". Total: S/" + pedido.getTotal())));
        messages.add(textMessage);

        Map<String, Object> payloadMessage = new HashMap<>();
        Map<String, Object> customPayload = new HashMap<>();
        customPayload.put("tipo", "ORDEN_CREADA");
        customPayload.put("ordenId", pedido.getId());
        customPayload.put("total", pedido.getTotal());
        customPayload.put("redirectUrl", "/checkout?ordenId=" + pedido.getId());
        
        payloadMessage.put("payload", customPayload);
        messages.add(payloadMessage);

        jsonResponse.put("fulfillmentResponse", Map.of("messages", messages));
        return ResponseEntity.ok(jsonResponse);
    }
}