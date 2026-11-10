package com.tienda.inventory.service;

import com.tienda.inventory.client.ProductClient;
import com.tienda.inventory.dto.*;
import com.tienda.inventory.exception.BusinessRuleException;
import com.tienda.inventory.exception.DuplicateResourceException;
import com.tienda.inventory.exception.ResourceNotFoundException;
import com.tienda.inventory.model.Inventory;
import com.tienda.inventory.model.InventoryMovement;
import com.tienda.inventory.model.TipoMovimiento;
import com.tienda.inventory.repository.InventoryMovementRepository;
import com.tienda.inventory.repository.InventoryRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final InventoryRepository inventoryRepository;
    private final InventoryMovementRepository movementRepository;
    private final ProductClient productClient;

    public InventoryService(InventoryRepository inventoryRepository,
                            InventoryMovementRepository movementRepository,
                            ProductClient productClient) {
        this.inventoryRepository = inventoryRepository;
        this.movementRepository = movementRepository;
        this.productClient = productClient;
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> listar() {
        return inventoryRepository.findAll().stream().map(InventoryResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public InventoryResponse obtenerPorProducto(Long productId) {
        return InventoryResponse.from(buscarPorProducto(productId));
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> alertas() {
        return inventoryRepository.findBajoMinimo().stream().map(InventoryResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<MovementResponse> movimientos(Long productId) {
        buscarPorProducto(productId); // 404 si no hay inventario
        return movementRepository.findByProductIdOrderByFechaMovimientoDesc(productId).stream()
                .map(MovementResponse::from).toList();
    }

    @Transactional
    public InventoryResponse crear(InventoryRequest req) {
        // Regla: no registrar inventario para productos inexistentes (consulta a Product-Service)
        if (!productClient.existe(req.productId())) {
            throw new BusinessRuleException("No se puede registrar inventario: el producto "
                    + req.productId() + " no existe");
        }
        if (inventoryRepository.existsByProductId(req.productId())) {
            throw new DuplicateResourceException("El producto " + req.productId() + " ya tiene inventario registrado");
        }
        Inventory inv = new Inventory();
        inv.setProductId(req.productId());
        inv.setStockActual(req.stockActual());
        inv.setStockMinimo(req.stockMinimo());
        inv.setUltimaActualizacion(LocalDateTime.now());
        inv = inventoryRepository.save(inv);

        if (req.stockActual() > 0) {
            registrar(inv.getProductId(), TipoMovimiento.ENTRADA, req.stockActual());
        }
        avisarSiStockBajo(inv);
        return InventoryResponse.from(inv);
    }

    /** PUT: corrige el stock directamente; queda registrado como AJUSTE con el stock resultante. */
    @Transactional
    public InventoryResponse actualizar(Long id, InventoryUpdateRequest req) {
        Inventory inv = inventoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventario no encontrado: " + id));
        inv.setStockActual(req.stockActual());
        inv.setStockMinimo(req.stockMinimo());
        inv.setUltimaActualizacion(LocalDateTime.now());
        inv = inventoryRepository.save(inv);

        registrar(inv.getProductId(), TipoMovimiento.AJUSTE, req.stockActual());
        avisarSiStockBajo(inv);
        return InventoryResponse.from(inv);
    }

    @Transactional
    public InventoryResponse registrarMovimiento(Long productId, MovementRequest req) {
        // Bloqueo de la fila para que dos ventas simultáneas no pisen el stock
        Inventory inv = inventoryRepository.findByProductIdForUpdate(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El producto " + productId + " no tiene inventario registrado"));
        int cantidad = req.cantidad();

        switch (req.tipoMovimiento()) {
            case ENTRADA -> {
                exigirPositiva(cantidad);
                inv.setStockActual(inv.getStockActual() + cantidad);
            }
            case SALIDA -> {
                exigirPositiva(cantidad);
                // Regla: no permitir inventario negativo
                if (inv.getStockActual() < cantidad) {
                    throw new BusinessRuleException("Stock insuficiente para el producto " + productId
                            + ": disponible " + inv.getStockActual() + ", solicitado " + cantidad);
                }
                inv.setStockActual(inv.getStockActual() - cantidad);
            }
            case AJUSTE -> inv.setStockActual(cantidad);
        }
        inv.setUltimaActualizacion(LocalDateTime.now());
        inv = inventoryRepository.save(inv);

        // Regla: registrar todos los movimientos
        registrar(productId, req.tipoMovimiento(), cantidad);
        avisarSiStockBajo(inv);
        return InventoryResponse.from(inv);
    }

    private Inventory buscarPorProducto(Long productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El producto " + productId + " no tiene inventario registrado"));
    }

    private void registrar(Long productId, TipoMovimiento tipo, int cantidad) {
        InventoryMovement m = new InventoryMovement();
        m.setProductId(productId);
        m.setTipoMovimiento(tipo);
        m.setCantidad(cantidad);
        m.setFechaMovimiento(LocalDateTime.now());
        movementRepository.save(m);
    }

    private void exigirPositiva(int cantidad) {
        if (cantidad <= 0) {
            throw new BusinessRuleException("La cantidad debe ser mayor que cero para entradas y salidas");
        }
    }

    /** Regla: generar alerta cuando el stock sea inferior al mínimo. */
    private void avisarSiStockBajo(Inventory inv) {
        if (inv.getStockActual() < inv.getStockMinimo()) {
            log.warn("ALERTA DE STOCK BAJO: producto {} tiene {} unidades (mínimo {})",
                    inv.getProductId(), inv.getStockActual(), inv.getStockMinimo());
        }
    }
}
