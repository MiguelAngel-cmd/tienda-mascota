package com.tienda.inventory.repository;

import com.tienda.inventory.model.InventoryMovement;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {

    List<InventoryMovement> findByProductIdOrderByFechaMovimientoDesc(Long productId);
}
