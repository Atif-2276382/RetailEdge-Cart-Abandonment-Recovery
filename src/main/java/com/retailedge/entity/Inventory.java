package com.retailedge.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.Check;

@Entity
@Check(constraints = "stock >= 0")
@Table(name = "inventory", indexes = {
        @Index(name = "idx_inventory_owner_product", columnList = "owner_id, product_id", unique = true)
})
public class Inventory {

    @Id
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(nullable = false)
    private int stock;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    private long version;

    protected Inventory() {
    }

    public Inventory(UUID id, UUID productId, UUID ownerId, int stock) {
        this.id = id;
        this.productId = productId;
        this.ownerId = ownerId;
        this.stock = stock;
        this.updatedAt = OffsetDateTime.now();
    }

    public void updateStock(int newStock) {
        if (newStock < 0) {
            throw new IllegalArgumentException("Stock cannot be negative");
        }
        this.stock = newStock;
        this.updatedAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getProductId() {
        return productId;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public int getStock() {
        return stock;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}