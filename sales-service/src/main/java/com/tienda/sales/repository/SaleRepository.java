package com.tienda.sales.repository;

import com.tienda.sales.model.Sale;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    /** Historial de ventas: la más reciente primero. */
    List<Sale> findAllByOrderByFechaVentaDesc();
}
