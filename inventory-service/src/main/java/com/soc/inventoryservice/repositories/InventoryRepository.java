package com.soc.inventoryservice.repositories;

import com.soc.inventoryservice.models.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    Optional<Inventory> findByCode(String code);
    List<Inventory> findByCodeIn(List<String> code);
}
