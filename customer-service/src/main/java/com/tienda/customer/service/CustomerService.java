package com.tienda.customer.service;

import com.tienda.customer.dto.CustomerRequest;
import com.tienda.customer.dto.CustomerResponse;
import com.tienda.customer.exception.DuplicateResourceException;
import com.tienda.customer.exception.ResourceNotFoundException;
import com.tienda.customer.model.Customer;
import com.tienda.customer.repository.CustomerRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Capa de negocio: aquí viven las reglas del enunciado. */
@Service
public class CustomerService {

    private final CustomerRepository repository;

    public CustomerService(CustomerRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> listar() {
        return repository.findAll().stream().map(CustomerResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public CustomerResponse obtener(Long id) {
        return CustomerResponse.from(buscar(id));
    }

    @Transactional
    public CustomerResponse crear(CustomerRequest req) {
        String documento = limpiar(req.documento());
        String correo = limpiar(req.correo());
        // Reglas: no permitir documentos ni correos repetidos
        if (documento != null && repository.existsByDocumento(documento)) {
            throw new DuplicateResourceException("Ya existe un cliente con el documento " + documento);
        }
        if (correo != null && repository.existsByCorreoIgnoreCase(correo)) {
            throw new DuplicateResourceException("Ya existe un cliente con el correo " + correo);
        }
        Customer c = new Customer();
        copiar(req, c);
        return CustomerResponse.from(repository.save(c));
    }

    @Transactional
    public CustomerResponse actualizar(Long id, CustomerRequest req) {
        Customer c = buscar(id);
        String documento = limpiar(req.documento());
        String correo = limpiar(req.correo());
        // El documento/correo puede repetirse solo si pertenece al mismo cliente
        if (documento != null) {
            repository.findByDocumento(documento).ifPresent(otro -> {
                if (!otro.getId().equals(id)) {
                    throw new DuplicateResourceException("Ya existe otro cliente con el documento " + documento);
                }
            });
        }
        if (correo != null) {
            repository.findByCorreoIgnoreCase(correo).ifPresent(otro -> {
                if (!otro.getId().equals(id)) {
                    throw new DuplicateResourceException("Ya existe otro cliente con el correo " + correo);
                }
            });
        }
        copiar(req, c);
        return CustomerResponse.from(repository.save(c));
    }

    @Transactional
    public void eliminar(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Cliente no encontrado: " + id);
        }
        repository.deleteById(id);
    }

    private Customer buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado: " + id));
    }

    private void copiar(CustomerRequest req, Customer c) {
        c.setNombre(req.nombre().trim());
        c.setApellido(limpiar(req.apellido()));
        c.setDocumento(limpiar(req.documento()));
        c.setTelefono(req.telefono().trim());
        c.setCorreo(limpiar(req.correo()));
        c.setDireccion(limpiar(req.direccion()));
    }

    /** Quita espacios; un texto vacío se guarda como null (así no choca con la restricción unique). */
    private String limpiar(String valor) {
        if (valor == null) {
            return null;
        }
        String t = valor.trim();
        return t.isEmpty() ? null : t;
    }
}
