package com.RutaDelSabor.ruta.controllers;

import com.RutaDelSabor.ruta.dto.*;
import com.RutaDelSabor.ruta.models.entities.Cliente;
import com.RutaDelSabor.ruta.models.entities.Pedido;
import com.RutaDelSabor.ruta.models.entities.Producto;
import com.RutaDelSabor.ruta.services.IClienteService;
import com.RutaDelSabor.ruta.services.IPedidoService;
import com.RutaDelSabor.ruta.services.IProductoService;
import com.RutaDelSabor.ruta.services.IReporteService;
import com.RutaDelSabor.ruta.utils.LevenshteinUtil; // <--- IMPORTANTE
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;

import java.util.*;
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

        if ("finalizar_pedido".equals(tag)) {
            return processFinalizarPedido(params);
        } else if ("recomendar_producto".equals(tag)) {
            return processRecomendacion();
        } else if ("consultar_menu".equals(tag)) { // <--- NUEVO
            return processConsultarMenu();
        }

        return ResponseEntity.ok(crearRespuestaTexto("Webhook: Acción no reconocida (" + tag + ")."));
    }

    // --- 1. PROCESAR PEDIDO CON BÚSQUEDA INTELIGENTE ---
    private ResponseEntity<Map<String, Object>> processFinalizarPedido(Map<String, Object> params) {
        try {
            // A. Obtener o crear Cliente "Invitado" si no viene email
            String email = (String) params.getOrDefault("email_cliente", "cliente@prueba.com");
            Cliente cliente;
            try {
                cliente = clienteService.buscarPorCorreo(email);
            } catch (Exception e) {
                // Fallback si no existe el cliente
                log.warn("Cliente {} no encontrado, usando cliente por defecto.", email);
                cliente = clienteService.buscarPorCorreo("cliente@prueba.com"); 
            }

            // B. Identificar Producto (FUZZY MATCHING)
            String inputUsuario = (String) params.get("producto_solicitado"); 
            Integer cantidad = params.get("cantidad") != null ? ((Number) params.get("cantidad")).intValue() : 1;

            if (inputUsuario == null) return ResponseEntity.ok(crearRespuestaTexto("No entendí qué producto deseas."));

            // 🧠 AQUÍ ESTÁ LA MAGIA: Buscamos el producto más parecido
            Optional<Producto> mejorCoincidencia = productoService.buscarTodosActivos().stream()
                .filter(p -> LevenshteinUtil.calculateSimilarity(p.getProducto(), inputUsuario) > 0.4) // Umbral de similitud 40%
                .sorted((p1, p2) -> Double.compare(
                        LevenshteinUtil.calculateSimilarity(p2.getProducto(), inputUsuario),
                        LevenshteinUtil.calculateSimilarity(p1.getProducto(), inputUsuario))) // Ordenar por mayor similitud
                .findFirst();

            if (mejorCoincidencia.isEmpty()) {
                return ResponseEntity.ok(crearRespuestaTexto("Lo siento, no tenemos '" + inputUsuario + "' en el menú. ¿Quizás quisiste decir otra cosa?"));
            }

            Producto productoReal = mejorCoincidencia.get();

            // C. Validar Stock Real
            if (productoReal.getStock() < cantidad) {
                return ResponseEntity.ok(crearRespuestaTexto("¡Uy! Solo me quedan " + productoReal.getStock() + " unidades de " + productoReal.getProducto() + "."));
            }

            // D. Crear la Orden
            OrdenRequestDTO ordenRequest = new OrdenRequestDTO();
            ordenRequest.setItems(List.of(new ItemDTO(productoReal.getId(), cantidad)));
            String direccion = (String) params.getOrDefault("direccion", "Recoger en tienda");
            ordenRequest.setDireccionEntrega(direccion);
            ordenRequest.setMetodoPago("Pendiente Web");

            Pedido nuevoPedido = pedidoService.crearNuevaOrden(ordenRequest, 
                new User(cliente.getCorreo(), cliente.getContraseña(), new ArrayList<>()));

            // E. Respuesta con Payload para el Frontend
            return construirRespuestaConPayload(nuevoPedido, productoReal.getProducto());

        } catch (Exception e) {
            log.error("Error crítico en webhook:", e);
            return ResponseEntity.ok(crearRespuestaTexto("Tuve un problema técnico procesando la orden. Intenta de nuevo."));
        }
    }

    // --- 2. RECOMENDACIONES ---
    private ResponseEntity<Map<String, Object>> processRecomendacion() {
        List<ProductoPopularDTO> populares = reporteService.obtenerProductosPopulares(3);
        
        if (populares.isEmpty()) return ResponseEntity.ok(crearRespuestaTexto("Hoy todo está delicioso. ¡Prueba nuestra especialidad de la casa!"));

        String nombres = populares.stream().map(ProductoPopularDTO::getNombreProducto).collect(Collectors.joining(", "));
        return ResponseEntity.ok(crearRespuestaTexto("Nuestros clientes aman: " + nombres + ". ¿Te anoto uno?"));
    }
    
    // --- 3. CONSULTAR MENÚ (Dinámico) ---
    private ResponseEntity<Map<String, Object>> processConsultarMenu() {
        // Podrías devolver una lista de categorías aquí
        return ResponseEntity.ok(crearRespuestaTexto("Tenemos Hamburguesas, Pizzas y Bebidas. ¿Qué te provoca hoy?"));
    }

    // --- HELPERS ---
    
    private Map<String, Object> crearRespuestaTexto(String mensaje) {
        Map<String, Object> json = new HashMap<>();
        Map<String, Object> fulfillment = new HashMap<>();
        Map<String, Object> text = new HashMap<>();
        text.put("text", Collections.singletonList(mensaje));
        fulfillment.put("messages", Collections.singletonList(Map.of("text", text)));
        json.put("fulfillmentResponse", fulfillment);
        return json;
    }

    private ResponseEntity<Map<String, Object>> construirRespuestaConPayload(Pedido pedido, String nombreProducto) {
        Map<String, Object> jsonResponse = new HashMap<>();
        List<Map<String, Object>> messages = new ArrayList<>();

        // 1. Mensaje de Texto
        Map<String, Object> textMessage = new HashMap<>();
        textMessage.put("text", Map.of("text", List.of("¡Listo! He generado la orden #" + pedido.getId() + " por " + nombreProducto + ". Por favor completa el pago abajo.")));
        messages.add(textMessage);

        // 2. Payload Personalizado (Para que tu Front muestre el botón de pago)
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