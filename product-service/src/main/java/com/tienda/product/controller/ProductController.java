package com.tienda.product.controller;

import com.tienda.product.dto.ProductRequest;
import com.tienda.product.dto.ProductResponse;
import com.tienda.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/products")
@Tag(name = "Productos", description = "CRUD del catálogo de productos")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar todos los productos")
    public List<ProductResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar un producto por id")
    public ProductResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping
    @Operation(summary = "Crear un producto (201 Created)")
    public ResponseEntity<ProductResponse> crear(@Valid @RequestBody ProductRequest req) {
        ProductResponse creado = service.crear(req);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creado.id()).toUri();
        return ResponseEntity.created(location).body(creado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un producto")
    public ProductResponse actualizar(@PathVariable Long id, @Valid @RequestBody ProductRequest req) {
        return service.actualizar(id, req);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un producto (204 No Content)")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
