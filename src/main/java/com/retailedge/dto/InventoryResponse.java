package com.retailedge.dto;

import com.retailedge.entity.Inventory;
import java.time.OffsetDateTime;
import java.util.UUID;

public record InventoryResponse(
        UUID id,
        UUID productId,
        UUID ownerId,
        int stock,
        OffsetDateTime updatedAt) {

    public static InventoryResponse from(Inventory inventory) {
        return new InventoryResponse(
                inventory.getId(),
                inventory.getProductId(),
                inventory.getOwnerId(),
                inventory.getStock(),
                inventory.getUpdatedAt());
    }
}