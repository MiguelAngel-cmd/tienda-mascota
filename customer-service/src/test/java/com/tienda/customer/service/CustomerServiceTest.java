package com.tienda.customer.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.tienda.customer.dto.CustomerRequest;
import com.tienda.customer.dto.CustomerResponse;
import com.tienda.customer.exception.DuplicateResourceException;
import com.tienda.customer.exception.ResourceNotFoundException;
import com.tienda.customer.model.Customer;
import com.tienda.customer.repository.CustomerRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    CustomerRepository repository;

    @InjectMocks
    CustomerService service;

    private CustomerRequest request(String documento, String correo) {
        return new CustomerRequest("Ana", "Pérez", documento, "3001234567", correo, "Calle 1 # 2-3");
    }

    private Customer existente(Long id, String documento, String correo) {
        Customer c = new Customer();
        c.setId(id);
        c.setNombre("Luis");
        c.setTelefono("3000000000");
        c.setDocumento(documento);
        c.setCorreo(correo);
        return c;
    }

    @Test
    void registrarCliente_guardaYDevuelveRespuesta() {
        when(repository.existsByDocumento("123")).thenReturn(false);
        when(repository.existsByCorreoIgnoreCase("ana@mail.com")).thenReturn(false);
        when(repository.save(any(Customer.class))).thenAnswer(inv -> {
            Customer c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });

        CustomerResponse resp = service.crear(request("123", "ana@mail.com"));

        assertEquals(1L, resp.id());
        assertEquals("Ana", resp.nombre());
        assertEquals("ana@mail.com", resp.correo());
    }

    @Test
    void registrarCliente_conDocumentoDuplicado_lanzaExcepcion() {
        when(repository.existsByDocumento("123")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> service.crear(request("123", "ana@mail.com")));
        verify(repository, never()).save(any());
    }

    @Test
    void registrarCliente_conCorreoDuplicado_lanzaExcepcion() {
        when(repository.existsByDocumento("123")).thenReturn(false);
        when(repository.existsByCorreoIgnoreCase("ana@mail.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> service.crear(request("123", "ana@mail.com")));
        verify(repository, never()).save(any());
    }

    @Test
    void registrarCliente_sinDocumentoNiCorreo_noConsultaDuplicados() {
        when(repository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        CustomerResponse resp = service.crear(request("  ", ""));

        assertNull(resp.documento());
        assertNull(resp.correo());
        verify(repository, never()).existsByDocumento(any());
        verify(repository, never()).existsByCorreoIgnoreCase(any());
    }

    @Test
    void consultarClientes_devuelveLaLista() {
        when(repository.findAll()).thenReturn(List.of(existente(1L, "1", "a@a.com"), existente(2L, "2", "b@b.com")));

        assertEquals(2, service.listar().size());
    }

    @Test
    void consultarCliente_inexistente_lanzaNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.obtener(99L));
    }

    @Test
    void actualizarCliente_cambiaLosDatos() {
        Customer c = existente(5L, "111", "viejo@mail.com");
        when(repository.findById(5L)).thenReturn(Optional.of(c));
        when(repository.findByDocumento("222")).thenReturn(Optional.empty());
        when(repository.findByCorreoIgnoreCase("nuevo@mail.com")).thenReturn(Optional.empty());
        when(repository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        CustomerResponse resp = service.actualizar(5L, request("222", "nuevo@mail.com"));

        assertEquals("222", resp.documento());
        assertEquals("nuevo@mail.com", resp.correo());
        assertEquals("Ana", resp.nombre());
    }

    @Test
    void actualizarCliente_conCorreoDeOtroCliente_lanzaExcepcion() {
        when(repository.findById(5L)).thenReturn(Optional.of(existente(5L, "111", "a@a.com")));
        when(repository.findByDocumento("111")).thenReturn(Optional.of(existente(5L, "111", "a@a.com")));
        when(repository.findByCorreoIgnoreCase("otro@mail.com"))
                .thenReturn(Optional.of(existente(7L, "999", "otro@mail.com")));

        assertThrows(DuplicateResourceException.class, () -> service.actualizar(5L, request("111", "otro@mail.com")));
        verify(repository, never()).save(any());
    }

    @Test
    void actualizarCliente_conSusPropiosDatos_noEsDuplicado() {
        Customer c = existente(5L, "111", "a@a.com");
        when(repository.findById(5L)).thenReturn(Optional.of(c));
        when(repository.findByDocumento("111")).thenReturn(Optional.of(c));
        when(repository.findByCorreoIgnoreCase("a@a.com")).thenReturn(Optional.of(c));
        when(repository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> service.actualizar(5L, request("111", "a@a.com")));
    }

    @Test
    void eliminarCliente_existente_loBorra() {
        when(repository.existsById(5L)).thenReturn(true);

        service.eliminar(5L);

        verify(repository).deleteById(5L);
    }

    @Test
    void eliminarCliente_inexistente_lanzaNotFound() {
        when(repository.existsById(5L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.eliminar(5L));
        verify(repository, never()).deleteById(any());
    }
}
