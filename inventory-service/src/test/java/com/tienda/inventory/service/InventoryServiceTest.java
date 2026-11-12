package com.tienda.inventory.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.tienda.inventory.client.ProductClient;
import com.tienda.inventory.dto.*;
import com.tienda.inventory.exception.BusinessRuleException;
import com.tienda.inventory.exception.ResourceNotFoundException;
import com.tienda.inventory.model.Inventory;
import com.tienda.inventory.model.InventoryMovement;
import com.tienda.inventory.model.TipoMovimiento;
import com.tienda.inventory.repository.InventoryMovementRepository;
import com.tienda.inventory.repository.InventoryRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    InventoryRepository inventoryRepository;
    @Mock
    InventoryMovementRepository movementRepository;
    @Mock
    ProductClient productClient;

    @InjectMocks
    InventoryService service;

    private Inventory inventario(Long id, Long productId, int stock, int minimo) {
        Inventory i = new Inventory();
        i.setId(id);
        i.setProductId(productId);
        i.setStockActual(stock);
        i.setStockMinimo(minimo);
        i.setUltimaActualizacion(LocalDateTime.now());
        return i;
    }

    @Test
    void registrarInventario_deProductoInexistente_lanzaExcepcion() {
        when(productClient.existe(10L)).thenReturn(false);

        assertThrows(BusinessRuleException.class,
                () -> service.crear(new InventoryRequest(10L, 5, 2)));
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void registrarInventario_guardaYRegistraMovimientoDeEntrada() {
        when(productClient.existe(10L)).thenReturn(true);
        when(inventoryRepository.existsByProductId(10L)).thenReturn(false);
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(inv -> {
            Inventory i = inv.getArgument(0);
            i.setId(1L);
            return i;
        });

        InventoryResponse resp = service.crear(new InventoryRequest(10L, 20, 5));

        assertEquals(20, resp.stockActual());
        assertFalse(resp.alertaStockBajo());
        ArgumentCaptor<InventoryMovement> captor = ArgumentCaptor.forClass(InventoryMovement.class);
        verify(movementRepository).save(captor.capture());
        assertEquals(TipoMovimiento.ENTRADA, captor.getValue().getTipoMovimiento());
        assertEquals(20, captor.getValue().getCantidad());
    }

    @Test
    void movimientoDeEntrada_sumaAlStock() {
        when(inventoryRepository.findByProductIdForUpdate(10L)).thenReturn(Optional.of(inventario(1L, 10L, 10, 3)));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(inv -> inv.getArgument(0));

        InventoryResponse resp = service.registrarMovimiento(10L, new MovementRequest(TipoMovimiento.ENTRADA, 15));

        assertEquals(25, resp.stockActual());
        verify(movementRepository).save(any(InventoryMovement.class));
    }

    @Test
    void movimientoDeSalida_restaAlStock() {
        when(inventoryRepository.findByProductIdForUpdate(10L)).thenReturn(Optional.of(inventario(1L, 10L, 10, 3)));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(inv -> inv.getArgument(0));

        InventoryResponse resp = service.registrarMovimiento(10L, new MovementRequest(TipoMovimiento.SALIDA, 4));

        assertEquals(6, resp.stockActual());
    }

    @Test
    void salidaMayorAlStock_noPermiteInventarioNegativo() {
        when(inventoryRepository.findByProductIdForUpdate(10L)).thenReturn(Optional.of(inventario(1L, 10L, 3, 1)));

        assertThrows(BusinessRuleException.class,
                () -> service.registrarMovimiento(10L, new MovementRequest(TipoMovimiento.SALIDA, 5)));
        verify(movementRepository, never()).save(any());
    }

    @Test
    void stockPorDebajoDelMinimo_activaLaAlerta() {
        when(inventoryRepository.findByProductIdForUpdate(10L)).thenReturn(Optional.of(inventario(1L, 10L, 10, 8)));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(inv -> inv.getArgument(0));

        InventoryResponse resp = service.registrarMovimiento(10L, new MovementRequest(TipoMovimiento.SALIDA, 5));

        assertEquals(5, resp.stockActual());
        assertTrue(resp.alertaStockBajo());
    }

    @Test
    void ajuste_dejaElStockEnLaCantidadIndicada() {
        when(inventoryRepository.findByProductIdForUpdate(10L)).thenReturn(Optional.of(inventario(1L, 10L, 10, 2)));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(inv -> inv.getArgument(0));

        InventoryResponse resp = service.registrarMovimiento(10L, new MovementRequest(TipoMovimiento.AJUSTE, 7));

        assertEquals(7, resp.stockActual());
    }

    @Test
    void movimientoSobreProductoSinInventario_lanzaNotFound() {
        when(inventoryRepository.findByProductIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.registrarMovimiento(99L, new MovementRequest(TipoMovimiento.ENTRADA, 1)));
    }
}
