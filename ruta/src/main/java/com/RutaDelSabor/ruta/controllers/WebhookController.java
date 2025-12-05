package com.RutaDelSabor.ruta.controllers;

import com.RutaDelSabor.ruta.dto.*;
import com.RutaDelSabor.ruta.models.entities.*;
import com.RutaDelSabor.ruta.models.dao.IComentarioDAO;
import com.RutaDelSabor.ruta.services.*;
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

    // Servicios necesarios
    @Autowired private IPedidoService pedidoService;
    @Autowired private IClienteService clienteService;
    @Autowired private IProductoService productoService;
    @Autowired private IReporteService reporteService;
    @Autowired private IComentarioDAO comentarioDAO;

    @PostMapping("/dialogflow")
    public ResponseEntity<Map<String, Object>> handleWebhook(@RequestBody DialogflowRequest request) {
        // Validación de seguridad para evitar caídas
        if (request == null || request.getFulfillmentInfo() == null) {
            return ResponseEntity.ok(crearRespuestaTexto("Error técnico: Webhook vacío."));
        }

        String tag = request.getFulfillmentInfo().getTag();
        Map<String, Object> params = (request.getSessionInfo() != null && request.getSessionInfo().getParameters() != null)
                ? request.getSessionInfo().getParameters()
                : new HashMap<>();
        
        log.info("🔹 Webhook Tag recibido: {}", tag);

        switch (tag) {
            case "bienvenida_usuario": 
                return processBienvenida(params);
            case "navegar_pagina": 
                return processNavegacion(params);
            case "agregar_carrito":
            case "finalizar_pedido": // Soporte para ambos nombres de tag
                return processAgregarAlCarrito(params);
            case "guardar_comentario": 
                return processGuardarComentario(params);
            case "consultar_estado_pedido": 
                return processConsultarEstado(params);
            case "recomendar_producto": 
                return processRecomendacion(params);
            case "consultar_menu": 
                return processConsultarMenu();
            default: 
                return ResponseEntity.ok(crearRespuestaTexto("Acción no reconocida por el servidor: " + tag));
        }
    }

    // --- 1. NAVEGACIÓN (Controla el Frontend) ---
    private ResponseEntity<Map<String, Object>> processNavegacion(Map<String, Object> params) {
        String destino = (String) params.getOrDefault("pagina_destino", "menu");
        String url = "menu.html";
        String msj = "Te llevo al menú.";

        if (destino != null) {
            if (destino.toLowerCase().contains("nosotros")) { 
                url = "nosotros.html"; 
                msj = "Conoce nuestra historia."; 
            }
            else if (destino.toLowerCase().contains("contacto") || destino.toLowerCase().contains("ubicacion")) { 
                url = "index.html#contacto"; // Anclaje a la sección contacto
                msj = "Aquí tienes nuestros datos de contacto."; 
            }
            else if (destino.toLowerCase().contains("carrito")) { 
                url = "carrito.html"; 
                msj = "Vamos a revisar tu orden."; 
            }
        }

        // Payload que el JS leerá para cambiar de página
        Map<String, Object> payload = new HashMap<>();
        payload.put("tipo", "NAVEGACION");
        payload.put("url", url);
        return ResponseEntity.ok(crearRespuestaConPayload(msj, payload));
    }

    // --- 2. AGREGAR AL CARRITO (Inteligencia + LocalStorage) ---
    private ResponseEntity<Map<String, Object>> processAgregarAlCarrito(Map<String, Object> params) {
        try {
            String input = (String) params.get("producto_solicitado");
            if (input == null) return ResponseEntity.ok(crearRespuestaTexto("No entendí qué producto deseas agregar."));

            // Divide frases complejas como "hamburguesa y coca cola"
            String[] itemsRaw = input.toLowerCase().split(" y | con |,|\\+| más ");
            
            List<ItemDTO> itemsEncontrados = new ArrayList<>();
            List<Producto> todosProductos = productoService.buscarTodosActivos();
            List<String> nombresEncontrados = new ArrayList<>();
            StringBuilder alertas = new StringBuilder();

            for (String raw : itemsRaw) {
                if (raw.trim().isEmpty()) continue;
                
                // Extrae "2" de "2 hamburguesas"
                Pair<Integer, String> info = extraerCantidadYNombre(raw.trim());
                String busqueda = info.getValue();
                
                // Búsqueda inteligente (Levenshtein)
                Optional<Producto> match = todosProductos.stream()
                    .filter(p -> LevenshteinUtil.calculateSimilarity(p.getProducto(), busqueda) > 0.4) // Umbral de similitud
                    .sorted((p1, p2) -> Double.compare(
                        LevenshteinUtil.calculateSimilarity(p2.getProducto(), busqueda),
                        LevenshteinUtil.calculateSimilarity(p1.getProducto(), busqueda)))
                    .findFirst();

                if (match.isPresent()) {
                    Producto p = match.get();
                    
                    // Verificar Stock
                    if (p.getStock() < info.getKey()) {
                        alertas.append("Solo quedan ").append(p.getStock()).append(" de ").append(p.getProducto()).append(". ");
                    } else {
                        // Construir DTO para el Frontend
                        String img = (p.getImagen() != null && !p.getImagen().isEmpty()) ? p.getImagen() : "icon/logo.png";
                        
                        itemsEncontrados.add(new ItemDTO(
                            p.getId(), 
                            p.getProducto(), 
                            p.getPrecio(), 
                            img, 
                            info.getKey(), 
                            p.getStock()
                        ));
                        nombresEncontrados.add(info.getKey() + "x " + p.getProducto());
                    }
                }
            }

            if (itemsEncontrados.isEmpty()) {
                return ResponseEntity.ok(crearRespuestaTexto("Lo siento, no encontré esos productos en el menú. Intenta usar nombres como 'Hamburguesa Clásica'."));
            }

            // Payload para que el JS guarde en LocalStorage
            Map<String, Object> payload = new HashMap<>();
            payload.put("tipo", "AGREGAR_CARRITO");
            payload.put("items", itemsEncontrados);
            
            String mensajeFinal = alertas.toString() + "¡Listo! He añadido " + String.join(", ", nombresEncontrados) + " a tu carrito.";
            return ResponseEntity.ok(crearRespuestaConPayload(mensajeFinal, payload));

        } catch (Exception e) {
            log.error("Error procesando carrito", e);
            return ResponseEntity.ok(crearRespuestaTexto("Tuve un problema técnico procesando tu pedido."));
        }
    }

    // --- 3. GUARDAR COMENTARIOS ---
    private ResponseEntity<Map<String, Object>> processGuardarComentario(Map<String, Object> params) {
        String texto = (String) params.getOrDefault("texto_comentario", "Sin comentario");
        int calif = 5;
        try { 
            // Dialogflow puede mandar el número como string o double
            calif = Double.valueOf(params.getOrDefault("calificacion", "5").toString()).intValue();
        } catch(Exception e) { log.warn("Error parseando calificación"); }

        Comentario c = new Comentario();
        c.setTexto(texto);
        c.setPuntuacion(calif);
        c.setFechaComentario(new Date());
        c.setMedio("Chatbot");
        
        String email = (String) params.get("email_cliente");
        if(email != null && !email.isEmpty()){
             try {
                Cliente cli = clienteService.buscarPorCorreo(email);
                c.setCliente(cli);
                comentarioDAO.save(c); // Guardado real en BD
             } catch(Exception e) { 
                 log.warn("Cliente no encontrado para comentario, guardando como anónimo si la BD lo permite."); 
                 // Si tu BD requiere cliente obligatorio, aquí podrías asignar un cliente genérico ID=1
             }
        }
        return ResponseEntity.ok(crearRespuestaTexto("¡Gracias por tu comentario! Nos ayuda a mejorar. ⭐"));
    }

    // --- 4. CONSULTA DE ESTADO ---
    private ResponseEntity<Map<String, Object>> processConsultarEstado(Map<String, Object> params) {
        String email = (String) params.get("email_cliente");
        if(email == null || email.isEmpty()) return ResponseEntity.ok(crearRespuestaTexto("Para ver tu estado, necesito que inicies sesión en la web."));

        try {
            Cliente cliente = clienteService.buscarPorCorreo(email);
            // Simulación de UserDetails para reutilizar tu servicio existente
            User userDetails = new User(cliente.getCorreo(), cliente.getContraseña(), new ArrayList<>());
            List<Pedido> historial = pedidoService.obtenerHistorialPedidos(userDetails);
            
            if (historial.isEmpty()) return ResponseEntity.ok(crearRespuestaTexto("No tienes pedidos recientes."));
            
            Pedido ultimo = historial.get(0);
            return ResponseEntity.ok(crearRespuestaTexto("Tu pedido #" + ultimo.getId() + " está: " + ultimo.getEstadoActual()));
        } catch (Exception e) {
            return ResponseEntity.ok(crearRespuestaTexto("No encontré pedidos asociados a tu cuenta."));
        }
    }

    // --- HELPERS (Utilidades) ---

    private ResponseEntity<Map<String, Object>> processBienvenida(Map<String, Object> params) {
        String nombre = (String) params.getOrDefault("nombre_usuario", "");
        String saludo = (!nombre.isEmpty()) ? "¡Hola " + nombre + "! " : "¡Hola! ";
        return ResponseEntity.ok(crearRespuestaTexto(saludo + "Soy Verónica. ¿Te ayudo a pedir algo rico o quieres ver el menú?"));
    }
    
    private ResponseEntity<Map<String, Object>> processConsultarMenu() {
        return ResponseEntity.ok(crearRespuestaTexto("Tenemos hamburguesas, alitas, broasters y refrescos. ¿Qué se te antoja?"));
    }

    private ResponseEntity<Map<String, Object>> processRecomendacion(Map<String, Object> params) {
        try {
            List<ProductoPopularDTO> pops = reporteService.obtenerProductosPopulares(3);
            String txt = pops.stream().map(ProductoPopularDTO::getNombreProducto).collect(Collectors.joining(", "));
            return ResponseEntity.ok(crearRespuestaTexto("Nuestros clientes aman: " + txt));
        } catch (Exception e) {
            return ResponseEntity.ok(crearRespuestaTexto("Te recomiendo nuestra Hamburguesa Clásica. ¡Es la favorita!"));
        }
    }

    private Map<String, Object> crearRespuestaTexto(String mensaje) {
        return crearRespuestaConPayload(mensaje, null);
    }

    private Map<String, Object> crearRespuestaConPayload(String mensaje, Map<String, Object> payload) {
        Map<String, Object> json = new HashMap<>();
        Map<String, Object> fulfillment = new HashMap<>();
        List<Map<String, Object>> messages = new ArrayList<>();
        
        // 1. Mensaje de Texto
        Map<String, Object> textObj = new HashMap<>();
        textObj.put("text", List.of(mensaje));
        messages.add(Map.of("text", textObj));
        
        // 2. Payload (Datos para el Frontend)
        if (payload != null) {
            messages.add(Map.of("payload", payload));
        }
        
        fulfillment.put("messages", messages);
        json.put("fulfillmentResponse", fulfillment);
        return json;
    }

    // Utilidad para extraer "2 hamburguesas" -> Cantidad: 2, Nombre: "hamburguesas"
    private Pair<Integer, String> extraerCantidadYNombre(String texto) {
        Map<String, Integer> numerosTexto = Map.of("un", 1, "una", 1, "uno", 1, "dos", 2, "tres", 3, "cuatro", 4);
        Pattern p = Pattern.compile("^(\\d+)\\s+(.*)");
        Matcher m = p.matcher(texto);
        if (m.find()) return new Pair<>(Integer.parseInt(m.group(1)), m.group(2));
        
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
}