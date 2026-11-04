package com.tienda.product.repository;

import com.tienda.product.model.Product;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data JPA genera la implementación a partir de los nombres de los métodos. */
public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsByCodigoBarras(String codigoBarras);

    Optional<Product> findByCodigoBarras(String codigoBarras);
}
