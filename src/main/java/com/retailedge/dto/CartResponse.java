package com.retailedge.dto;

import com.retailedge.entity.CartAbandonment;
import com.retailedge.entity.CartStatus;
import com.retailedge.entity.RecoveryStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CartResponse(
        UUID id,
        BigDecimal cartTotal,
        CartStatus status,
        RecoveryStatus recoveryStatus,
        OffsetDateTime abandonedAt,
        OffsetDateTime recoveryDeadline,
        OffsetDateTime recoveredAt) {

    public static CartResponse from(CartAbandonment cart) {
        return new CartResponse(cart.getId(), cart.getCartTotal(), cart.getStatus(),
                cart.getRecoveryStatus(), cart.getAbandonedAt(), cart.getRecoveryDeadline(),
                cart.getRecoveredAt());
    }
}