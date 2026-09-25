package com.retailedge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.retailedge.dto.CreateCartRequest;
import com.retailedge.dto.CursorPage;
import com.retailedge.dto.ScanResult;
import com.retailedge.entity.CartAbandonment;
import com.retailedge.entity.CartStatus;
import com.retailedge.entity.RecoveryStatus;
import com.retailedge.exception.ResourceNotFoundException;
import com.retailedge.repository.CartAbandonmentRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class CartAbandonmentServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-21T12:00:00Z");
    private static final UUID OWNER_ID = UUID.randomUUID();

    @Mock
    private CartAbandonmentRepository repository;

    private CartAbandonmentService service;

    @BeforeEach
    void setUp() {
        service = new CartAbandonmentService(
                repository, Clock.fixed(NOW, ZoneOffset.UTC), 2);
        lenient().when(repository.save(any(CartAbandonment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createUsesAuthenticatedOwnerAndCurrencySafeTotal() {
        CreateCartRequest request = new CreateCartRequest("customer@example.com", new BigDecimal("19.99"));

        var response = service.create(request, OWNER_ID);

        assertThat(response.cartTotal()).isEqualByComparingTo("19.99");
        assertThat(response.status()).isEqualTo(CartStatus.ACTIVE);
        verify(repository).save(any(CartAbandonment.class));
    }

    @Test
    void createRejectsNegativeTotalBeforePersistence() {
        CreateCartRequest request = new CreateCartRequest("customer@example.com", new BigDecimal("-0.01"));

        assertThatThrownBy(() -> service.create(request, OWNER_ID))
                .isInstanceOf(IllegalArgumentException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void scanMarksCartAbandonedAfterThirtyMinutes() {
        CartAbandonment cart = activeCart(NOW.minusSeconds(30 * 60));
        when(repository.findByStatusAndLastActivityAtLessThanEqualOrderByIdAsc(
                any(), any(), any(PageRequest.class))).thenReturn(List.of(cart));

        ScanResult result = service.scanAbandoned(null, 2);

        assertThat(result.abandoned()).isEqualTo(1);
        assertThat(cart.getStatus()).isEqualTo(CartStatus.ABANDONED);
        assertThat(cart.getRecoveryDeadline()).isEqualTo(now().plusHours(24));
    }

    @Test
    void scanLeavesRecentlyActiveCartAlone() {
        CartAbandonment cart = activeCart(NOW.minusSeconds(29 * 60));
        when(repository.findByStatusAndLastActivityAtLessThanEqualOrderByIdAsc(
                any(), any(), any(PageRequest.class))).thenReturn(List.of(cart));

        ScanResult result = service.scanAbandoned(null, 2);

        assertThat(result.abandoned()).isZero();
        assertThat(cart.getStatus()).isEqualTo(CartStatus.ACTIVE);
        verify(repository, never()).save(any());
    }

    @Test
    void recoveryStartsForOwnedAbandonedCart() {
        CartAbandonment cart = abandonedCart();
        when(repository.findByIdAndOwnerId(cart.getId(), OWNER_ID)).thenReturn(java.util.Optional.of(cart));

        var response = service.startRecovery(cart.getId(), OWNER_ID);

        assertThat(response.recoveryStatus()).isEqualTo(RecoveryStatus.IN_PROGRESS);
    }

    @Test
    void recoveryCompletesWithinTwentyFourHourWindow() {
        CartAbandonment cart = abandonedCart();
        when(repository.findByIdAndOwnerId(cart.getId(), OWNER_ID)).thenReturn(java.util.Optional.of(cart));

        var response = service.markRecovered(cart.getId(), OWNER_ID);

        assertThat(response.status()).isEqualTo(CartStatus.RECOVERED);
        assertThat(response.recoveryStatus()).isEqualTo(RecoveryStatus.RECOVERED);
    }

    @Test
    void recoveryRejectsCartOutsideWindow() {
        CartAbandonment cart = new CartAbandonment(
                UUID.randomUUID(), OWNER_ID, "customer@example.com", BigDecimal.ONE,
                now().minusHours(25).minusMinutes(31));
            cart.markAbandonedIfEligible(now().minusHours(25));
        when(repository.findByIdAndOwnerId(cart.getId(), OWNER_ID)).thenReturn(java.util.Optional.of(cart));

        assertThatThrownBy(() -> service.markRecovered(cart.getId(), OWNER_ID))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void ownerMismatchReturnsNotFoundAndDoesNotExposeCart() {
        UUID cartId = UUID.randomUUID();
        when(repository.findByIdAndOwnerId(cartId, OWNER_ID)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> service.findById(cartId, OWNER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void abandonedResultsUseCursorAndBoundedPageSize() {
        UUID cursor = UUID.randomUUID();
        CartAbandonment first = abandonedCart();
        CartAbandonment second = abandonedCart();
        when(repository.findByOwnerIdAndStatusAndIdGreaterThanOrderByIdAsc(
                OWNER_ID, CartStatus.ABANDONED, cursor, PageRequest.of(0, 3)))
                .thenReturn(List.of(first, second));

        CursorPage<?> page = service.findAbandoned(OWNER_ID, cursor.toString(), 2);

        assertThat(page.content()).hasSize(2);
        assertThat(page.hasNext()).isFalse();
        assertThat(page.nextCursor()).isNull();
    }

    @Test
    void scanReturnsNextCursorForRestartableBatch() {
        CartAbandonment first = activeCart(NOW.minus(Duration.ofHours(1)));
        CartAbandonment second = activeCart(NOW.minus(Duration.ofHours(1)));
        CartAbandonment extra = activeCart(NOW.minus(Duration.ofHours(1)));
        when(repository.findByStatusAndLastActivityAtLessThanEqualOrderByIdAsc(
                CartStatus.ACTIVE, now().minusMinutes(30), PageRequest.of(0, 3)))
                .thenReturn(List.of(first, second, extra));

        ScanResult result = service.scanAbandoned(null, 2);

        assertThat(result.processed()).isEqualTo(2);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.nextCursor()).isEqualTo(second.getId().toString());
    }

    @Test
    void invalidCursorIsRejected() {
        assertThatThrownBy(() -> service.findAbandoned(OWNER_ID, "invalid", 10))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private CartAbandonment abandonedCart() {
        CartAbandonment cart = activeCart(NOW.minus(Duration.ofMinutes(31)));
        cart.markAbandonedIfEligible(now());
        return cart;
    }

    private CartAbandonment activeCart(Instant lastActivity) {
        return new CartAbandonment(UUID.randomUUID(), OWNER_ID, "customer@example.com",
                new BigDecimal("10.00"), lastActivity.atOffset(ZoneOffset.UTC));
    }

    private OffsetDateTime now() {
        return NOW.atOffset(ZoneOffset.UTC);
    }
}
