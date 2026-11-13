package com.tienda.sales.service;

import com.tienda.sales.client.CustomerClient;
import com.tienda.sales.client.InventoryClient;
import com.tienda.sales.client.InventoryInfo;
import com.tienda.sales.client.ProductClient;
import com.tienda.sales.client.ProductInfo;
import com.tienda.sales.dto.SaleItemRequest;
import com.tienda.sales.dto.SaleRequest;
import com.tienda.sales.dto.SaleResponse;
import com.tienda.sales.dto.SaleUpdateRequest;
import com.tienda.sales.exception.BusinessRuleException;
import com.tienda.sales.exception.ResourceNotFoundException;
import com.tienda.sales.model.EstadoVenta;
import com.tienda.sales.model.Sale;
import com.tienda.sales.model.SaleDetail;
import com.tienda.sales.repository.SaleRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Capa de negocio: orquesta el flujo de venta entre Customer, Product e Inventory. */
@Service
public class SaleService {

    private static final Logger log = LoggerFactory.getLogger(SaleService.class);

    private final SaleRepository repository;
    private final CustomerClient customerClient;
    private final ProductClient productClient;
    private final InventoryClient inventoryClient;

    public SaleService(SaleRepository repository, CustomerClient customerClient,
                       ProductClient productClient, InventoryClient inventoryClient) {
        this.repository = repository;
        this.customerClient = customerClient;
        this.productClient = productClient;
        this.inventoryClient = inventoryClient;
    }

    @Transactional(readOnly = true)
    public List<SaleResponse> listar() {
        return repository.findAllByOrderByFechaVentaDesc().stream().map(SaleResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public SaleResponse obtener(Long id) {
        return SaleResponse.from(buscar(id));
    }

    /**
     * Flujo: validar cliente -> validar productos -> verificar stock -> registrar venta -> actualizar inventario.
     * No lleva @Transactional a propósito: la venta se guarda como PENDIENTE antes de llamar al inventario
     * y cada paso se confirma por separado, así si el inventario falla la venta queda CANCELADA en el historial.
     */
    public SaleResponse crear(SaleRequest req) {
        // Regla: no permitir ventas sin cliente válido
        if (!customerClient.existe(req.customerId())) {
            throw new BusinessRuleException("No se puede registrar la venta: el cliente "
                    + req.customerId() + " no existe");
        }

        // Si un producto viene repetido se suman sus cantidades, para verificar el stock una sola vez
        Map<Long, Integer> cantidades = new LinkedHashMap<>();
        for (SaleItemRequest item : req.items()) {
            cantidades.merge(item.productId(), item.cantidad(), Integer::sum);
        }

        Sale venta = new Sale();
        venta.setCustomerId(req.customerId());
        venta.setFechaVenta(LocalDateTime.now());
        venta.setEstado(EstadoVenta.PENDIENTE);
        BigDecimal total = BigDecimal.ZERO;

        for (Map.Entry<Long, Integer> linea : cantidades.entrySet()) {
            Long productId = linea.getKey();
            int cantidad = linea.getValue();

            ProductInfo producto = productClient.obtener(productId)
                    .orElseThrow(() -> new BusinessRuleException("El producto " + productId + " no existe"));
            if (!"ACTIVO".equals(producto.estado())) {
                throw new BusinessRuleException("El producto " + productId + " está inactivo y no se puede vender");
            }

            // Regla: no permitir ventas sin stock disponible
            InventoryInfo inventario = inventoryClient.obtener(productId)
                    .orElseThrow(() -> new BusinessRuleException(
                            "El producto " + productId + " no tiene inventario registrado"));
            if (inventario.stockActual() < cantidad) {
                throw new BusinessRuleException("Stock insuficiente para el producto " + productId
                        + ": disponible " + inventario.stockActual() + ", solicitado " + cantidad);
            }

            // Regla: calcular automáticamente subtotales y total
            BigDecimal subtotal = producto.precio().multiply(BigDecimal.valueOf(cantidad))
                    .setScale(2, RoundingMode.HALF_UP);
            SaleDetail detalle = new SaleDetail();
            detalle.setProductId(productId);
            detalle.setCantidad(cantidad);
            detalle.setPrecioUnitario(producto.precio());
            detalle.setSubtotal(subtotal);
            venta.agregarDetalle(detalle);
            total = total.add(subtotal);
        }
        venta.setTotal(total);
        venta = repository.save(venta);

        // Regla: actualizar el inventario después de registrar la venta
        List<SaleDetail> descontados = new ArrayList<>();
        try {
            for (SaleDetail detalle : venta.getDetalles()) {
                inventoryClient.descontar(detalle.getProductId(), detalle.getCantidad());
                descontados.add(detalle);
            }
        } catch (RuntimeException e) {
            // Si algo falla a la mitad se devuelve lo que ya se había descontado y la venta se cancela
            devolverStock(descontados);
            venta.setEstado(EstadoVenta.CANCELADA);
            repository.save(venta);
            throw e;
        }
        venta.setEstado(EstadoVenta.FACTURADA);
        return SaleResponse.from(repository.save(venta));
    }

    /** PUT: por ahora solo permite cancelar una venta; si estaba FACTURADA, el stock vuelve al inventario. */
    public SaleResponse actualizar(Long id, SaleUpdateRequest req) {
        Sale venta = buscar(id);
        if (req.estado() != EstadoVenta.CANCELADA) {
            throw new BusinessRuleException("Solo se permite cambiar el estado a CANCELADA; "
                    + "la venta se factura automáticamente al registrarla");
        }
        if (venta.getEstado() == EstadoVenta.CANCELADA) {
            throw new BusinessRuleException("La venta " + id + " ya está cancelada");
        }
        if (venta.getEstado() == EstadoVenta.FACTURADA) {
            List<SaleDetail> repuestos = new ArrayList<>();
            try {
                for (SaleDetail detalle : venta.getDetalles()) {
                    inventoryClient.reponer(detalle.getProductId(), detalle.getCantidad());
                    repuestos.add(detalle);
                }
            } catch (RuntimeException e) {
                // No se pudo devolver todo el stock: se deshace lo repuesto y la venta sigue FACTURADA
                volverADescontar(repuestos);
                throw e;
            }
        }
        venta.setEstado(EstadoVenta.CANCELADA);
        return SaleResponse.from(repository.save(venta));
    }

    private Sale buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada: " + id));
    }

    /** Compensación: devuelve el stock de los detalles indicados (si falla uno, se registra y se sigue). */
    private void devolverStock(List<SaleDetail> detalles) {
        for (SaleDetail d : detalles) {
            try {
                inventoryClient.reponer(d.getProductId(), d.getCantidad());
            } catch (RuntimeException e) {
                log.error("No se pudo devolver el stock del producto {} ({} unidades): {}",
                        d.getProductId(), d.getCantidad(), e.getMessage());
            }
        }
    }

    private void volverADescontar(List<SaleDetail> detalles) {
        for (SaleDetail d : detalles) {
            try {
                inventoryClient.descontar(d.getProductId(), d.getCantidad());
            } catch (RuntimeException e) {
                log.error("No se pudo restablecer la salida del producto {} ({} unidades): {}",
                        d.getProductId(), d.getCantidad(), e.getMessage());
            }
        }
    }
}
