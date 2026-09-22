package com.retailedge.controller;

import com.retailedge.dto.CartResponse;
import com.retailedge.dto.CreateCartRequest;
import com.retailedge.dto.CursorPage;
import com.retailedge.dto.ScanResult;
import com.retailedge.security.JwtIdentity;
import com.retailedge.service.CartAbandonmentService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cart-abandonments")
public class CartAbandonmentController {

    private final CartAbandonmentService service;

    public CartAbandonmentController(CartAbandonmentService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<CartResponse> create(
            @Valid @RequestBody CreateCartRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID ownerId = JwtIdentity.requireOwnerId(jwt);
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request, ownerId));
    }

    @GetMapping("/abandoned")
    public CursorPage<CartResponse> findAbandoned(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "25") int limit,
            @AuthenticationPrincipal Jwt jwt) {
        return service.findAbandoned(JwtIdentity.requireOwnerId(jwt), cursor, limit);
    }

    @GetMapping("/{cartId}")
    public CartResponse findById(
            @PathVariable UUID cartId,
            @AuthenticationPrincipal Jwt jwt) {
        return service.findById(cartId, JwtIdentity.requireOwnerId(jwt));
    }

    @PostMapping("/{cartId}/recovery/start")
    public CartResponse startRecovery(
            @PathVariable UUID cartId,
            @AuthenticationPrincipal Jwt jwt) {
        return service.startRecovery(cartId, JwtIdentity.requireOwnerId(jwt));
    }

    @PostMapping("/{cartId}/recovery/complete")
    public CartResponse completeRecovery(
            @PathVariable UUID cartId,
            @AuthenticationPrincipal Jwt jwt) {
        return service.markRecovered(cartId, JwtIdentity.requireOwnerId(jwt));
    }

    @PostMapping("/scan")
    @PreAuthorize("hasRole('ADMIN')")
    public ScanResult scan(
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer batchSize) {
        return batchSize == null
                ? service.scanAbandoned(cursor)
                : service.scanAbandoned(cursor, batchSize);
    }
}
