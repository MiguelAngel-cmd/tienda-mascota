package com.tienda.sales.controller;

import com.tienda.sales.dto.SaleRequest;
import com.tienda.sales.dto.SaleResponse;
import com.tienda.sales.dto.SaleUpdateRequest;
import com.tienda.sales.service.SaleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/sales")
@Tag(name = "Ventas", description = "Registro de ventas y actualización automática del inventario")
public class SaleController {

    private final SaleService service;

    public SaleController(SaleService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Historial de ventas (la más reciente primero)")
    public List<SaleResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar una venta con su detalle")
    public SaleResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping
    @Operation(summary = "Registrar una venta (valida cliente, productos y stock; descuenta el inventario)")
    public ResponseEntity<SaleResponse> crear(@Valid @RequestBody SaleRequest req) {
        SaleResponse creada = service.crear(req);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creada.id()).toUri();
        return ResponseEntity.created(location).body(creada);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cancelar una venta (devuelve el stock si ya estaba facturada)")
    public SaleResponse actualizar(@PathVariable Long id, @Valid @RequestBody SaleUpdateRequest req) {
        return service.actualizar(id, req);
    }
}
