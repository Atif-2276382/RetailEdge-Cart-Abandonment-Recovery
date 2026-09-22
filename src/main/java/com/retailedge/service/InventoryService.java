package com.retailedge.service;

import com.retailedge.dto.CreateInventoryRequest;
import com.retailedge.dto.InventoryResponse;
import com.retailedge.dto.UpdateStockRequest;
import com.retailedge.entity.Inventory;
import com.retailedge.exception.ResourceNotFoundException;
import com.retailedge.repository.InventoryRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final InventoryRepository repository;

    public InventoryService(InventoryRepository repository) {
        this.repository = repository;
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public InventoryResponse create(CreateInventoryRequest request, UUID ownerId) {
        validateRequest(request, ownerId);
        validateStock(request.stock());
        Inventory inventory = new Inventory(
                UUID.randomUUID(),
                request.productId(),
                ownerId,
                request.stock());
        InventoryResponse response = InventoryResponse.from(repository.save(inventory));
        log.info("inventory_created inventory_id={} product_id={} owner_id={}",
                response.id(), response.productId(), response.ownerId());
        return response;
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public InventoryResponse updateStock(UUID inventoryId, UpdateStockRequest request) {
        validateRequest(request, inventoryId);
        validateStock(request.stock());
        Inventory inventory = findById(inventoryId);
        inventory.updateStock(request.stock());
        InventoryResponse response = InventoryResponse.from(repository.save(inventory));
        log.info("inventory_stock_updated inventory_id={} product_id={} stock={}",
                response.id(), response.productId(), response.stock());
        return response;
    }

    @Transactional(readOnly = true)
    public InventoryResponse getByProduct(UUID productId, UUID ownerId) {
        if (productId == null || ownerId == null) {
            throw new IllegalArgumentException("Product and owner are required");
        }
        return repository.findByProductIdAndOwnerId(productId, ownerId)
                .map(InventoryResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found"));
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(UUID inventoryId) {
        if (inventoryId == null) {
            throw new IllegalArgumentException("Inventory ID is required");
        }
        Inventory inventory = findById(inventoryId);
        repository.delete(inventory);
        log.info("inventory_deleted inventory_id={} product_id={}", inventory.getId(), inventory.getProductId());
    }

    private Inventory findById(UUID inventoryId) {
        return repository.findById(inventoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found"));
    }

    private void validateStock(int stock) {
        if (stock < 0) {
            throw new IllegalArgumentException("Stock cannot be negative");
        }
    }

    private void validateRequest(CreateInventoryRequest request, UUID ownerId) {
        if (request == null || request.productId() == null || ownerId == null) {
            throw new IllegalArgumentException("Product and owner are required");
        }
    }

    private void validateRequest(UpdateStockRequest request, UUID inventoryId) {
        if (request == null || inventoryId == null) {
            throw new IllegalArgumentException("Inventory ID and stock are required");
        }
    }
}