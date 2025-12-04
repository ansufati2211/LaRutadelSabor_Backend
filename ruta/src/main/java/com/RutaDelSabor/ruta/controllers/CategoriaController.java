package com.RutaDelSabor.ruta.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.RutaDelSabor.ruta.models.entities.Categoria;
import com.RutaDelSabor.ruta.services.ICategoriaService;

@RestController
@RequestMapping("/api/categorias") // [CORRECCIÓN 1]: Base unificada (Plural)
public class CategoriaController {

    @Autowired
    private ICategoriaService m_Service;

    // --- ENDPOINTS PÚBLICOS ---

    // Ruta final: GET /api/categorias
    @GetMapping
    public List<Categoria> getAll() {
        return m_Service.GetAll();
    }

    // --- ENDPOINTS CRUD (Internos/Admin) ---

    // [CORRECCIÓN 2]: Rutas coinciden con admin.js (/api/categorias/admin)

    @PostMapping("/admin")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')") // Agregamos VENDEDOR por seguridad
    public Categoria saveCategoria(@RequestBody Categoria categoria) {
        return m_Service.Save(categoria);
    }

    @GetMapping("/admin/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public Categoria getCategoriaById(@PathVariable Long id) {
        return m_Service.FindByID(id);
    }

    @PutMapping("/admin/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public Categoria updateCategoria(@RequestBody Categoria categoria, @PathVariable Long id) {
        categoria.setId(id);
        return m_Service.Save(categoria);
    }

    @DeleteMapping("/admin/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')") // Solo Admin puede borrar físicamente
    public void deleteCategoria(@PathVariable Long id) {
        m_Service.Delete(id);
    }
}