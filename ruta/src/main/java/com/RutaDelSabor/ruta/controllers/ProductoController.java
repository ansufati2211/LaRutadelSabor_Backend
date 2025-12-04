package com.RutaDelSabor.ruta.controllers;

import java.util.List;
import com.RutaDelSabor.ruta.dto.ErrorResponseDTO;
import com.RutaDelSabor.ruta.exception.ProductoNoEncontradoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.RutaDelSabor.ruta.models.entities.Producto;
import com.RutaDelSabor.ruta.models.entities.Categoria;
import com.RutaDelSabor.ruta.services.IProductoService;
import com.RutaDelSabor.ruta.services.ICategoriaService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private static final Logger log = LoggerFactory.getLogger(ProductoController.class);

    @Autowired
    private IProductoService productoService;

    @Autowired
    private ICategoriaService categoriaService;

    // --- Endpoints Públicos (Cualquiera puede verlos) ---

    @GetMapping
    public ResponseEntity<List<Producto>> getAllPublic() {
         log.info("Solicitud GET /api/productos (público)");
         try {
            List<Producto> productos = productoService.buscarTodosActivos();
            return ResponseEntity.ok(productos);
         } catch (Exception e) {
             log.error("Error al obtener productos activos:", e);
             return ResponseEntity.internalServerError().build();
         }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getByIdPublic(@PathVariable Long id) {
         log.info("Solicitud GET /api/productos/{} (público)", id);
         try {
            Producto producto = productoService.buscarPorIdActivo(id);
            return ResponseEntity.ok(producto);
         } catch (ProductoNoEncontradoException e) {
             log.warn("Producto activo no encontrado (público): {}", e.getMessage());
             return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponseDTO(e.getMessage()));
         } catch (Exception e) {
             log.error("Error al obtener producto activo ID {}:", id, e);
             return ResponseEntity.internalServerError().body(new ErrorResponseDTO("Error al obtener el producto."));
         }
    }

    // --- Endpoints de Administración y Gestión ---

    @GetMapping("/admin/all")
    // CORRECCIÓN: Usamos hasAnyAuthority para coincidir exacto con la BD
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_VENDEDOR')")
    public ResponseEntity<List<Producto>> getAllAdmin() {
        log.info("Admin/Vendedor: Solicitud GET /api/productos/admin/all");
        try {
            List<Producto> productos = productoService.buscarTodos();
            return ResponseEntity.ok(productos);
        } catch (Exception e) {
            log.error("Error al obtener lista admin de productos:", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/admin/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_VENDEDOR')")
    public ResponseEntity<?> getByIdAdmin(@PathVariable Long id) {
         log.info("Admin/Vendedor: Solicitud GET /api/productos/admin/{}", id);
         try {
            Producto producto = productoService.buscarPorId(id);
            return ResponseEntity.ok(producto);
         } catch (ProductoNoEncontradoException e) {
             log.warn("Admin: Producto no encontrado: {}", e.getMessage());
             return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponseDTO(e.getMessage()));
         } catch (Exception e) {
             log.error("Admin: Error al obtener producto ID {}:", id, e);
             return ResponseEntity.internalServerError().body(new ErrorResponseDTO("Error al obtener el producto."));
         }
    }

    @PostMapping("/admin")
    // CORRECCIÓN: Permitimos al VENDEDOR crear productos también (coherencia con admin.js)
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_VENDEDOR')")
    public ResponseEntity<?> createProducto(@Valid @RequestBody Producto producto) {
        log.info("Admin/Vendedor: Solicitud POST /api/productos/admin");
        
        if (producto.getId() != null) {
             return ResponseEntity.badRequest().body(new ErrorResponseDTO("No incluya ID al crear un nuevo producto."));
        }

        try {
            if (producto.getCategoria() != null && producto.getCategoria().getId() != null) {
                try {
                    Categoria categoria = categoriaService.FindByID(producto.getCategoria().getId());
                    producto.setCategoria(categoria);
                } catch (Exception e) {
                    log.warn("Categoría ID {} no encontrada al crear producto", producto.getCategoria().getId());
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponseDTO("La categoría especificada no existe."));
                }
            }

            Producto nuevoProducto = productoService.guardar(producto);
            return ResponseEntity.status(HttpStatus.CREATED).body(nuevoProducto);
        } catch (Exception e) {
             log.error("Admin: Error al crear producto:", e);
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponseDTO("Error al crear el producto: " + e.getMessage()));
        }
    }

    @PutMapping("/admin/{id}")
    // CORRECCIÓN: Permitimos al VENDEDOR editar productos
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_VENDEDOR')")
    public ResponseEntity<?> updateProducto(@PathVariable Long id, @Valid @RequestBody Producto productoDetalles) {
         log.info("Admin/Vendedor: Solicitud PUT /api/productos/admin/{}", id);
         try {
            Producto productoExistente = productoService.buscarPorId(id);

            productoExistente.setProducto(productoDetalles.getProducto());
            productoExistente.setDescripcion(productoDetalles.getDescripcion());
            productoExistente.setPrecio(productoDetalles.getPrecio());
            productoExistente.setStock(productoDetalles.getStock());
            productoExistente.setImagen(productoDetalles.getImagen());
            
            if (productoDetalles.getCategoria() != null && productoDetalles.getCategoria().getId() != null) {
                 try {
                     Categoria categoria = categoriaService.FindByID(productoDetalles.getCategoria().getId());
                     productoExistente.setCategoria(categoria);
                 } catch (Exception ex) {
                     return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponseDTO("La categoría especificada no existe."));
                 }
            } else {
                productoExistente.setCategoria(null); 
            }

            Producto actualizado = productoService.guardar(productoExistente);
            return ResponseEntity.ok(actualizado);

         } catch (ProductoNoEncontradoException e) {
             log.warn("Admin: Producto no encontrado para actualizar: {}", e.getMessage());
             return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponseDTO(e.getMessage()));
         } catch (Exception e) {
             log.error("Admin: Error al actualizar producto ID {}:", id, e);
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponseDTO("Error al actualizar el producto: " + e.getMessage()));
         }
    }

    @DeleteMapping("/admin/{id}")
    // CORRECCIÓN: SOLO ADMIN puede borrar (seguridad estricta)
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteProducto(@PathVariable Long id) {
         log.warn("Admin: Solicitud DELETE /api/productos/admin/{}", id);
         try {
            productoService.eliminarLogico(id);
            log.info("Admin: Borrado lógico de producto ID {} completado.", id);
            return ResponseEntity.noContent().build();
         } catch (ProductoNoEncontradoException e) {
             log.error("Admin: Error al borrar, producto ID {} no encontrado.", id);
             return ResponseEntity.notFound().build();
         } catch (Exception e) {
             log.error("Admin: Error inesperado al borrar producto ID {}:", id, e);
             return ResponseEntity.internalServerError().build();
         }
    }
}