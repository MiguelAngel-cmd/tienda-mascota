package com.tienda.inventory.repository;

import com.tienda.inventory.model.Inventory;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByProductId(Long productId);

    boolean existsByProductId(Long productId);

    /** Bloquea la fila mientras se modifica el stock: evita que dos ventas simultáneas descuenten a la vez. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Inventory i where i.productId = :productId")
    Optional<Inventory> findByProductIdForUpdate(@Param("productId") Long productId);

    /** Productos cuyo stock está por debajo del mínimo (alertas). */
    @Query("select i from Inventory i where i.stockActual < i.stockMinimo")
    List<Inventory> findBajoMinimo();
}
