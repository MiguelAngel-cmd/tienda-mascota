package com.tienda.product.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.tienda.product.dto.ProductRequest;
import com.tienda.product.dto.ProductResponse;
import com.tienda.product.exception.DuplicateResourceException;
import com.tienda.product.exception.ResourceNotFoundException;
import com.tienda.product.model.Categoria;
import com.tienda.product.model.EstadoProducto;
import com.tienda.product.model.Product;
import com.tienda.product.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    ProductRepository repository;

    @InjectMocks
    ProductService service;

    private ProductRequest request(String codigo) {
        return new ProductRequest("Concentrado Adulto", Categoria.ALIMENTOS, "DogChow",
                "Bulto 10 kg", new BigDecimal("85000"), codigo, null);
    }

    private Product existente(Long id, String codigo) {
        Product p = new Product();
        p.setId(id);
        p.setNombre("Pelota");
        p.setCategoria(Categoria.JUGUETES);
        p.setPrecio(new BigDecimal("12000"));
        p.setCodigoBarras(codigo);
        p.setEstado(EstadoProducto.ACTIVO);
        return p;
    }

    @Test
    void crearProducto_guardaYDevuelveRespuesta() {
        when(repository.existsByCodigoBarras("123")).thenReturn(false);
        when(repository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        ProductResponse resp = service.crear(request("123"));

        assertEquals(1L, resp.id());
        assertEquals("Concentrado Adulto", resp.nombre());
        assertEquals(EstadoProducto.ACTIVO, resp.estado()); // estado por defecto
    }

    @Test
    void crearProducto_conCodigoDuplicado_lanzaExcepcion() {
        when(repository.existsByCodigoBarras("123")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> service.crear(request("123")));
        verify(repository, never()).save(any());
    }

    @Test
    void consultarProducto_inexistente_lanzaNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.obtener(99L));
    }

    @Test
    void actualizarProducto_cambiaLosDatos() {
        Product p = existente(5L, "111");
        when(repository.findById(5L)).thenReturn(Optional.of(p));
        when(repository.findByCodigoBarras("222")).thenReturn(Optional.empty());
        when(repository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductResponse resp = service.actualizar(5L, request("222"));

        assertEquals("222", resp.codigoBarras());
        assertEquals("Concentrado Adulto", resp.nombre());
    }

    @Test
    void actualizarProducto_conCodigoDeOtroProducto_lanzaExcepcion() {
        when(repository.findById(5L)).thenReturn(Optional.of(existente(5L, "111")));
        when(repository.findByCodigoBarras("999")).thenReturn(Optional.of(existente(7L, "999")));

        assertThrows(DuplicateResourceException.class, () -> service.actualizar(5L, request("999")));
    }

    @Test
    void eliminarProducto_existente_loBorra() {
        when(repository.existsById(5L)).thenReturn(true);

        service.eliminar(5L);

        verify(repository).deleteById(5L);
    }

    @Test
    void eliminarProducto_inexistente_lanzaNotFound() {
        when(repository.existsById(5L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.eliminar(5L));
    }
}
