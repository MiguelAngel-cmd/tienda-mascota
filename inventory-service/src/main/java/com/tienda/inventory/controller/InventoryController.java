package com.tienda.inventory.controller;

import com.tienda.inventory.dto.*;
import com.tienda.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/inventory")
@Tag(name = "Inventario", description = "Existencias, movimientos y alertas de stock")
public class InventoryController {

    private final InventoryService service;

    public InventoryController(InventoryService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar el inventario de todos los productos")
    public List<InventoryResponse> listar() {
        return service.listar();
    }

    @GetMapping("/alerts")
    @Operation(summary = "Productos con stock por debajo del mínimo")
    public List<InventoryResponse> alertas() {
        return service.alertas();
    }

    @GetMapping("/{productId}")
    @Operation(summary = "Consultar el inventario de un producto")
    public InventoryResponse obtener(@PathVariable Long productId) {
        return service.obtenerPorProducto(productId);
    }

    @PostMapping
    @Operation(summary = "Registrar inventario de un producto (valida que exista en Product-Service)")
    public ResponseEntity<InventoryResponse> crear(@Valid @RequestBody InventoryRequest req) {
        InventoryResponse creado = service.crear(req);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{productId}").buildAndExpand(creado.productId()).toUri();
        return ResponseEntity.created(location).body(creado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar stock actual y mínimo (registra un AJUSTE)")
    public InventoryResponse actualizar(@PathVariable Long id, @Valid @RequestBody InventoryUpdateRequest req) {
        return service.actualizar(id, req);
    }

    @PostMapping("/{productId}/movements")
    @Operation(summary = "Registrar un movimiento (ENTRADA, SALIDA o AJUSTE)")
    public ResponseEntity<InventoryResponse> movimiento(@PathVariable Long productId,
                                                        @Valid @RequestBody MovementRequest req) {
        return ResponseEntity.status(201).body(service.registrarMovimiento(productId, req));
    }

    @GetMapping("/{productId}/movements")
    @Operation(summary = "Historial de movimientos de un producto")
    public List<MovementResponse> movimientos(@PathVariable Long productId) {
        return service.movimientos(productId);
    }
}
