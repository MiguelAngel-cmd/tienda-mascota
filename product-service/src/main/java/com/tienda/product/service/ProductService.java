package com.tienda.product.service;

import com.tienda.product.dto.ProductRequest;
import com.tienda.product.dto.ProductResponse;
import com.tienda.product.exception.DuplicateResourceException;
import com.tienda.product.exception.ResourceNotFoundException;
import com.tienda.product.model.EstadoProducto;
import com.tienda.product.model.Product;
import com.tienda.product.repository.ProductRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Capa de negocio: aquí viven las reglas del enunciado. */
@Service
public class ProductService {

    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> listar() {
        return repository.findAll().stream().map(ProductResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse obtener(Long id) {
        return ProductResponse.from(buscar(id));
    }

    @Transactional
    public ProductResponse crear(ProductRequest req) {
        String codigo = req.codigoBarras().trim();
        // Regla: no permitir códigos de barras duplicados
        if (repository.existsByCodigoBarras(codigo)) {
            throw new DuplicateResourceException("Ya existe un producto con el código de barras " + codigo);
        }
        Product p = new Product();
        copiar(req, p);
        return ProductResponse.from(repository.save(p));
    }

    @Transactional
    public ProductResponse actualizar(Long id, ProductRequest req) {
        Product p = buscar(id);
        String codigo = req.codigoBarras().trim();
        // El código puede repetirse solo si pertenece al mismo producto
        repository.findByCodigoBarras(codigo).ifPresent(otro -> {
            if (!otro.getId().equals(id)) {
                throw new DuplicateResourceException("Ya existe otro producto con el código de barras " + codigo);
            }
        });
        copiar(req, p);
        return ProductResponse.from(repository.save(p));
    }

    @Transactional
    public void eliminar(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Producto no encontrado: " + id);
        }
        repository.deleteById(id);
    }

    private Product buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + id));
    }

    private void copiar(ProductRequest req, Product p) {
        p.setNombre(req.nombre().trim());
        p.setCategoria(req.categoria());
        p.setMarca(req.marca());
        p.setDescripcion(req.descripcion());
        p.setPrecio(req.precio());
        p.setCodigoBarras(req.codigoBarras().trim());
        p.setEstado(req.estado() != null ? req.estado() : EstadoProducto.ACTIVO);
    }
}
