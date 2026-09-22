package com.retailedge.dto;

import com.retailedge.entity.CartAbandonment;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CartAbandonmentResponse(
        UUID id,
        BigDecimal cartTotal,
        OffsetDateTime abandonedAt) {

    public static CartAbandonmentResponse from(CartAbandonment abandonment) {
        return new CartAbandonmentResponse(
                abandonment.getId(),
                abandonment.getCartTotal(),
                abandonment.getAbandonedAt());
    }
}