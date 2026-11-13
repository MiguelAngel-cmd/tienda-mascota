package com.tienda.sales.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

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
import com.tienda.sales.exception.ServiceUnavailableException;
import com.tienda.sales.model.EstadoVenta;
import com.tienda.sales.model.Sale;
import com.tienda.sales.model.SaleDetail;
import com.tienda.sales.repository.SaleRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SaleServiceTest {

    @Mock
    SaleRepository repository;
    @Mock
    CustomerClient customerClient;
    @Mock
    ProductClient productClient;
    @Mock
    InventoryClient inventoryClient;

    @InjectMocks
    SaleService service;

    private ProductInfo producto(Long id, String precio, String estado) {
        return new ProductInfo(id, "Producto " + id, new BigDecimal(precio), estado);
    }

    private InventoryInfo existencias(Long productId, int stock) {
        return new InventoryInfo(productId, stock);
    }

    /** El repositorio simulado asigna id a la venta la primera vez que se guarda. */
    private void simularGuardado() {
        when(repository.save(any(Sale.class))).thenAnswer(inv -> {
            Sale s = inv.getArgument(0);
            if (s.getId() == null) {
                s.setId(1L);
            }
            return s;
        });
    }

    private Sale venta(Long id, EstadoVenta estado) {
        Sale s = new Sale();
        s.setId(id);
        s.setCustomerId(1L);
        s.setFechaVenta(LocalDateTime.now());
        s.setTotal(new BigDecimal("39001.00"));
        s.setEstado(estado);
        s.agregarDetalle(detalle(10L, 2, "12000.50"));
        s.agregarDetalle(detalle(20L, 3, "5000.00"));
        return s;
    }

    private SaleDetail detalle(Long productId, int cantidad, String precio) {
        SaleDetail d = new SaleDetail();
        d.setProductId(productId);
        d.setCantidad(cantidad);
        d.setPrecioUnitario(new BigDecimal(precio));
        d.setSubtotal(new BigDecimal(precio).multiply(BigDecimal.valueOf(cantidad)));
        return d;
    }

    private SaleRequest ventaDeDosProductos() {
        return new SaleRequest(1L, List.of(new SaleItemRequest(10L, 2), new SaleItemRequest(20L, 3)));
    }

    private void productosConStock() {
        when(customerClient.existe(1L)).thenReturn(true);
        when(productClient.obtener(10L)).thenReturn(Optional.of(producto(10L, "12000.50", "ACTIVO")));
        when(productClient.obtener(20L)).thenReturn(Optional.of(producto(20L, "5000", "ACTIVO")));
        when(inventoryClient.obtener(10L)).thenReturn(Optional.of(existencias(10L, 50)));
        when(inventoryClient.obtener(20L)).thenReturn(Optional.of(existencias(20L, 50)));
    }

    @Test
    void registrarVenta_calculaSubtotalesYTotal_yDescuentaElInventario() {
        productosConStock();
        simularGuardado();

        SaleResponse resp = service.crear(ventaDeDosProductos());

        assertEquals(EstadoVenta.FACTURADA, resp.estado());
        assertEquals(2, resp.detalles().size());
        assertEquals(0, new BigDecimal("24001.00").compareTo(resp.detalles().get(0).subtotal()));
        assertEquals(0, new BigDecimal("15000.00").compareTo(resp.detalles().get(1).subtotal()));
        assertEquals(0, new BigDecimal("39001.00").compareTo(resp.total()));
        verify(inventoryClient).descontar(10L, 2);
        verify(inventoryClient).descontar(20L, 3);
    }

    @Test
    void registrarVenta_conClienteInexistente_lanzaExcepcion() {
        when(customerClient.existe(99L)).thenReturn(false);

        assertThrows(BusinessRuleException.class,
                () -> service.crear(new SaleRequest(99L, List.of(new SaleItemRequest(10L, 1)))));
        verifyNoInteractions(productClient, inventoryClient, repository);
    }

    @Test
    void registrarVenta_conProductoInexistente_lanzaExcepcion() {
        when(customerClient.existe(1L)).thenReturn(true);
        when(productClient.obtener(10L)).thenReturn(Optional.empty());

        assertThrows(BusinessRuleException.class,
                () -> service.crear(new SaleRequest(1L, List.of(new SaleItemRequest(10L, 1)))));
        verify(repository, never()).save(any());
    }

    @Test
    void registrarVenta_conProductoInactivo_lanzaExcepcion() {
        when(customerClient.existe(1L)).thenReturn(true);
        when(productClient.obtener(10L)).thenReturn(Optional.of(producto(10L, "1000", "INACTIVO")));

        assertThrows(BusinessRuleException.class,
                () -> service.crear(new SaleRequest(1L, List.of(new SaleItemRequest(10L, 1)))));
        verifyNoInteractions(inventoryClient);
        verify(repository, never()).save(any());
    }

    @Test
    void registrarVenta_sinStockSuficiente_lanzaExcepcionYNoDescuenta() {
        when(customerClient.existe(1L)).thenReturn(true);
        when(productClient.obtener(10L)).thenReturn(Optional.of(producto(10L, "1000", "ACTIVO")));
        when(inventoryClient.obtener(10L)).thenReturn(Optional.of(existencias(10L, 1)));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> service.crear(new SaleRequest(1L, List.of(new SaleItemRequest(10L, 5)))));

        assertTrue(ex.getMessage().contains("Stock insuficiente"));
        verify(repository, never()).save(any());
        verify(inventoryClient, never()).descontar(anyLong(), anyInt());
    }

    @Test
    void registrarVenta_deProductoSinInventario_lanzaExcepcion() {
        when(customerClient.existe(1L)).thenReturn(true);
        when(productClient.obtener(10L)).thenReturn(Optional.of(producto(10L, "1000", "ACTIVO")));
        when(inventoryClient.obtener(10L)).thenReturn(Optional.empty());

        assertThrows(BusinessRuleException.class,
                () -> service.crear(new SaleRequest(1L, List.of(new SaleItemRequest(10L, 1)))));
        verify(repository, never()).save(any());
    }

    @Test
    void registrarVenta_conProductoRepetido_sumaLasCantidades() {
        when(customerClient.existe(1L)).thenReturn(true);
        when(productClient.obtener(10L)).thenReturn(Optional.of(producto(10L, "2000", "ACTIVO")));
        when(inventoryClient.obtener(10L)).thenReturn(Optional.of(existencias(10L, 5)));
        simularGuardado();

        SaleResponse resp = service.crear(
                new SaleRequest(1L, List.of(new SaleItemRequest(10L, 2), new SaleItemRequest(10L, 3))));

        assertEquals(1, resp.detalles().size());
        assertEquals(5, resp.detalles().get(0).cantidad());
        assertEquals(0, new BigDecimal("10000.00").compareTo(resp.total()));
        verify(inventoryClient).descontar(10L, 5);
    }

    @Test
    void registrarVenta_siFallaElInventario_devuelveLoDescontadoYCancelaLaVenta() {
        productosConStock();
        simularGuardado();
        doNothing().when(inventoryClient).descontar(10L, 2);
        doThrow(new BusinessRuleException("sin stock")).when(inventoryClient).descontar(20L, 3);

        assertThrows(BusinessRuleException.class, () -> service.crear(ventaDeDosProductos()));

        verify(inventoryClient).reponer(10L, 2);
        verify(inventoryClient, never()).reponer(eq(20L), anyInt());
        ArgumentCaptor<Sale> captor = ArgumentCaptor.forClass(Sale.class);
        verify(repository, times(2)).save(captor.capture());
        assertEquals(EstadoVenta.CANCELADA, captor.getValue().getEstado());
    }

    @Test
    void consultarHistorial_devuelveLasVentas() {
        when(repository.findAllByOrderByFechaVentaDesc())
                .thenReturn(List.of(venta(2L, EstadoVenta.FACTURADA), venta(1L, EstadoVenta.CANCELADA)));

        List<SaleResponse> ventas = service.listar();

        assertEquals(2, ventas.size());
        assertEquals(2L, ventas.get(0).id());
    }

    @Test
    void consultarVenta_conDetalle() {
        when(repository.findById(5L)).thenReturn(Optional.of(venta(5L, EstadoVenta.FACTURADA)));

        SaleResponse resp = service.obtener(5L);

        assertEquals(5L, resp.id());
        assertEquals(2, resp.detalles().size());
    }

    @Test
    void consultarVentaInexistente_lanzaNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.obtener(99L));
    }

    @Test
    void cancelarVentaFacturada_devuelveElStockAlInventario() {
        when(repository.findById(5L)).thenReturn(Optional.of(venta(5L, EstadoVenta.FACTURADA)));
        simularGuardado();

        SaleResponse resp = service.actualizar(5L, new SaleUpdateRequest(EstadoVenta.CANCELADA));

        assertEquals(EstadoVenta.CANCELADA, resp.estado());
        verify(inventoryClient).reponer(10L, 2);
        verify(inventoryClient).reponer(20L, 3);
    }

    @Test
    void cancelarVentaPendiente_noTocaElInventario() {
        when(repository.findById(5L)).thenReturn(Optional.of(venta(5L, EstadoVenta.PENDIENTE)));
        simularGuardado();

        SaleResponse resp = service.actualizar(5L, new SaleUpdateRequest(EstadoVenta.CANCELADA));

        assertEquals(EstadoVenta.CANCELADA, resp.estado());
        verifyNoInteractions(inventoryClient);
    }

    @Test
    void cancelarVentaYaCancelada_lanzaExcepcion() {
        when(repository.findById(5L)).thenReturn(Optional.of(venta(5L, EstadoVenta.CANCELADA)));

        assertThrows(BusinessRuleException.class,
                () -> service.actualizar(5L, new SaleUpdateRequest(EstadoVenta.CANCELADA)));
        verify(repository, never()).save(any());
    }

    @Test
    void cambiarEstadoADistintoDeCancelada_noEstaPermitido() {
        when(repository.findById(5L)).thenReturn(Optional.of(venta(5L, EstadoVenta.PENDIENTE)));

        assertThrows(BusinessRuleException.class,
                () -> service.actualizar(5L, new SaleUpdateRequest(EstadoVenta.FACTURADA)));
        verify(repository, never()).save(any());
    }

    @Test
    void cancelarVenta_siFallaLaDevolucionDeStock_deshaceLoRepuestoYSigueFacturada() {
        Sale facturada = venta(5L, EstadoVenta.FACTURADA);
        when(repository.findById(5L)).thenReturn(Optional.of(facturada));
        doNothing().when(inventoryClient).reponer(10L, 2);
        doThrow(new ServiceUnavailableException("caído")).when(inventoryClient).reponer(20L, 3);

        assertThrows(ServiceUnavailableException.class,
                () -> service.actualizar(5L, new SaleUpdateRequest(EstadoVenta.CANCELADA)));

        verify(inventoryClient).descontar(10L, 2);
        assertEquals(EstadoVenta.FACTURADA, facturada.getEstado());
        verify(repository, never()).save(any());
    }
}
