package com.retailedge.controller;

import com.retailedge.dto.CreateInventoryRequest;
import com.retailedge.dto.InventoryResponse;
import com.retailedge.dto.UpdateStockRequest;
import com.retailedge.security.JwtIdentity;
import com.retailedge.service.InventoryService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final InventoryService service;

    public InventoryController(InventoryService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InventoryResponse> create(
            @Valid @RequestBody CreateInventoryRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.create(request, JwtIdentity.requireOwnerId(jwt)));
    }

    @PatchMapping("/{inventoryId}/stock")
    @PreAuthorize("hasRole('ADMIN')")
    public InventoryResponse updateStock(
            @PathVariable UUID inventoryId,
            @Valid @RequestBody UpdateStockRequest request) {
        return service.updateStock(inventoryId, request);
    }

    @GetMapping("/products/{productId}")
    public InventoryResponse getByProduct(
            @PathVariable UUID productId,
            @AuthenticationPrincipal Jwt jwt) {
        return service.getByProduct(productId, JwtIdentity.requireOwnerId(jwt));
    }

    @DeleteMapping("/{inventoryId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID inventoryId) {
        service.delete(inventoryId);
        return ResponseEntity.noContent().build();
    }
}