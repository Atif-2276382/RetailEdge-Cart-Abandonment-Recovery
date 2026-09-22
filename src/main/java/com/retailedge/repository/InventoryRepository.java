package com.retailedge.repository;

import com.retailedge.entity.Inventory;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryRepository extends JpaRepository<Inventory, UUID> {

    Optional<Inventory> findByProductIdAndOwnerId(UUID productId, UUID ownerId);

    Optional<Inventory> findByIdAndOwnerId(UUID id, UUID ownerId);
}