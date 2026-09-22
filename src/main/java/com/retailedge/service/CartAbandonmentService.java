package com.retailedge.service;

import com.retailedge.dto.CartResponse;
import com.retailedge.dto.CreateCartRequest;
import com.retailedge.dto.CursorPage;
import com.retailedge.dto.ScanResult;
import com.retailedge.entity.CartAbandonment;
import com.retailedge.entity.CartStatus;
import com.retailedge.exception.ResourceNotFoundException;
import com.retailedge.repository.CartAbandonmentRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartAbandonmentService {

    private static final Logger log = LoggerFactory.getLogger(CartAbandonmentService.class);
    private static final int MAX_PAGE_SIZE = 100;

    private final CartAbandonmentRepository repository;
    private final Clock clock;
    private final int configuredBatchSize;

    public CartAbandonmentService(
            CartAbandonmentRepository repository,
            Clock clock,
            @Value("${recovery.scan.batch-size:100}") int configuredBatchSize) {
        this.repository = repository;
        this.clock = clock;
        this.configuredBatchSize = validateBatchSize(configuredBatchSize);
    }

    @Transactional
    public CartResponse create(CreateCartRequest request, UUID ownerId) {
        validateOwner(ownerId);
        if (request == null || request.customerEmail() == null || request.customerEmail().isBlank()
                || request.cartTotal() == null || request.cartTotal().signum() < 0) {
            throw new IllegalArgumentException("Cart email and nonnegative total are required");
        }
        CartAbandonment cart = new CartAbandonment(
                UUID.randomUUID(), ownerId, request.customerEmail(), request.cartTotal(), now());
        CartResponse response = CartResponse.from(repository.save(cart));
        log.info("cart_created cart_id={} owner_id={}", response.id(), ownerId);
        return response;
    }

    @Transactional(readOnly = true)
    public CartResponse findById(UUID cartId, UUID ownerId) {
        return CartResponse.from(findOwned(cartId, ownerId));
    }

    @Transactional(readOnly = true)
    public CursorPage<CartResponse> findAbandoned(UUID ownerId, String cursor, int limit) {
        validateOwner(ownerId);
        int pageSize = validatePageSize(limit);
        UUID cursorId = decodeCursor(cursor);
        List<CartAbandonment> results = cursorId == null
                ? repository.findByOwnerIdAndStatusOrderByIdAsc(
                        ownerId, CartStatus.ABANDONED, PageRequest.of(0, pageSize + 1))
                : repository.findByOwnerIdAndStatusAndIdGreaterThanOrderByIdAsc(
                        ownerId, CartStatus.ABANDONED, cursorId, PageRequest.of(0, pageSize + 1));
        boolean hasNext = results.size() > pageSize;
        List<CartAbandonment> page = results.subList(0, Math.min(results.size(), pageSize));
        String nextCursor = hasNext ? page.get(page.size() - 1).getId().toString() : null;
        return new CursorPage<>(page.stream().map(CartResponse::from).toList(), nextCursor, hasNext);
    }

    @Transactional
    public CartResponse startRecovery(UUID cartId, UUID ownerId) {
        CartAbandonment cart = findOwned(cartId, ownerId);
        cart.startRecovery(now());
        log.info("cart_recovery_started cart_id={} owner_id={}", cartId, ownerId);
        return CartResponse.from(repository.save(cart));
    }

    @Transactional
    public CartResponse markRecovered(UUID cartId, UUID ownerId) {
        CartAbandonment cart = findOwned(cartId, ownerId);
        cart.markRecovered(now());
        log.info("cart_recovered cart_id={} owner_id={}", cartId, ownerId);
        return CartResponse.from(repository.save(cart));
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ScanResult scanAbandoned(String cursor) {
        return scanAbandoned(cursor, configuredBatchSize);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ScanResult scanAbandoned(String cursor, int batchSize) {
        int validatedBatchSize = validateBatchSize(batchSize);
        UUID cursorId = decodeCursor(cursor);
        OffsetDateTime cutoff = now().minus(CartAbandonment.ABANDONMENT_THRESHOLD);
        List<CartAbandonment> results = cursorId == null
                ? repository.findByStatusAndLastActivityAtLessThanEqualOrderByIdAsc(
                        CartStatus.ACTIVE, cutoff, PageRequest.of(0, validatedBatchSize + 1))
                : repository.findByStatusAndLastActivityAtLessThanEqualAndIdGreaterThanOrderByIdAsc(
                        CartStatus.ACTIVE, cutoff, cursorId, PageRequest.of(0, validatedBatchSize + 1));
        boolean hasNext = results.size() > validatedBatchSize;
        List<CartAbandonment> batch = results.subList(0, Math.min(results.size(), validatedBatchSize));
        int abandoned = 0;
        for (CartAbandonment cart : batch) {
            if (cart.markAbandonedIfEligible(now())) {
                repository.save(cart);
                abandoned++;
            }
        }
        String nextCursor = batch.isEmpty() ? null : batch.get(batch.size() - 1).getId().toString();
        log.info("cart_abandonment_scan_completed processed={} abandoned={} next_cursor={} has_next={}",
                batch.size(), abandoned, nextCursor, hasNext);
        return new ScanResult(batch.size(), abandoned, nextCursor, hasNext);
    }

    private CartAbandonment findOwned(UUID cartId, UUID ownerId) {
        validateOwner(ownerId);
        if (cartId == null) {
            throw new IllegalArgumentException("Cart ID is required");
        }
        return repository.findByIdAndOwnerId(cartId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock);
    }

    private void validateOwner(UUID ownerId) {
        if (ownerId == null) {
            throw new IllegalArgumentException("Authenticated owner is required");
        }
    }

    private int validatePageSize(int limit) {
        if (limit < 1 || limit > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Page size must be between 1 and " + MAX_PAGE_SIZE);
        }
        return limit;
    }

    private int validateBatchSize(int batchSize) {
        if (batchSize < 1 || batchSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Batch size must be between 1 and " + MAX_PAGE_SIZE);
        }
        return batchSize;
    }

    private UUID decodeCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(cursor);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Cursor must be a UUID", exception);
        }
    }
}
