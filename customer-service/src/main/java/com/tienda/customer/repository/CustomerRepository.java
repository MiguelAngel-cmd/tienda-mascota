package com.tienda.customer.repository;

import com.tienda.customer.model.Customer;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data JPA genera la implementación a partir de los nombres de los métodos. */
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    boolean existsByDocumento(String documento);

    boolean existsByCorreoIgnoreCase(String correo);

    Optional<Customer> findByDocumento(String documento);

    Optional<Customer> findByCorreoIgnoreCase(String correo);
}
