package com.retailedge.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.Check;

@Entity
@Check(constraints = "cart_total >= 0")
@Table(name = "cart_abandonments", indexes = {
        @Index(name = "idx_cart_owner_status_activity", columnList = "owner_id, status, last_activity_at"),
        @Index(name = "idx_cart_owner_abandoned_cursor", columnList = "owner_id, abandoned_at, id")
})
public class CartAbandonment {

    public static final Duration ABANDONMENT_THRESHOLD = Duration.ofMinutes(30);
    public static final Duration RECOVERY_WINDOW = Duration.ofHours(24);

    @Id
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "customer_email", nullable = false)
    private String customerEmail;

    @Column(name = "cart_total", nullable = false, precision = 19, scale = 4)
    private BigDecimal cartTotal;

    @Column(name = "last_activity_at", nullable = false)
    private OffsetDateTime lastActivityAt;

    @Column(name = "abandoned_at")
    private OffsetDateTime abandonedAt;

    @Column(name = "recovered_at")
    private OffsetDateTime recoveredAt;

    @Column(name = "recovery_deadline")
    private OffsetDateTime recoveryDeadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CartStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "recovery_status", nullable = false, length = 20)
    private RecoveryStatus recoveryStatus;

    @Version
    private long version;

    protected CartAbandonment() {
    }

    public CartAbandonment(UUID id, UUID ownerId, String customerEmail, BigDecimal cartTotal,
            OffsetDateTime lastActivityAt) {
        this.id = id;
        this.ownerId = ownerId;
        this.customerEmail = customerEmail;
        this.cartTotal = cartTotal;
        this.lastActivityAt = lastActivityAt;
        this.status = CartStatus.ACTIVE;
        this.recoveryStatus = RecoveryStatus.NOT_STARTED;
    }

    public boolean markAbandonedIfEligible(OffsetDateTime now) {
        if (status != CartStatus.ACTIVE || lastActivityAt.isAfter(now.minus(ABANDONMENT_THRESHOLD))) {
            return false;
        }
        status = CartStatus.ABANDONED;
        abandonedAt = now;
        recoveryDeadline = now.plus(RECOVERY_WINDOW);
        return true;
    }

    public void startRecovery(OffsetDateTime now) {
        if (status != CartStatus.ABANDONED || recoveryDeadline == null || now.isAfter(recoveryDeadline)) {
            throw new IllegalStateException("Cart is outside the recovery window");
        }
        recoveryStatus = RecoveryStatus.IN_PROGRESS;
    }

    public void markRecovered(OffsetDateTime now) {
        if (status != CartStatus.ABANDONED || recoveryDeadline == null || now.isAfter(recoveryDeadline)) {
            throw new IllegalStateException("Cart is outside the recovery window");
        }
        status = CartStatus.RECOVERED;
        recoveryStatus = RecoveryStatus.RECOVERED;
        recoveredAt = now;
    }

    public void expireRecovery(OffsetDateTime now) {
        if (status == CartStatus.ABANDONED && recoveryDeadline != null && now.isAfter(recoveryDeadline)) {
            status = CartStatus.EXPIRED;
            recoveryStatus = RecoveryStatus.EXPIRED;
        }
    }

    public UUID getId() { return id; }
    public UUID getOwnerId() { return ownerId; }
    public String getCustomerEmail() { return customerEmail; }
    public BigDecimal getCartTotal() { return cartTotal; }
    public OffsetDateTime getLastActivityAt() { return lastActivityAt; }
    public OffsetDateTime getAbandonedAt() { return abandonedAt; }
    public OffsetDateTime getRecoveredAt() { return recoveredAt; }
    public OffsetDateTime getRecoveryDeadline() { return recoveryDeadline; }
    public CartStatus getStatus() { return status; }
    public RecoveryStatus getRecoveryStatus() { return recoveryStatus; }
}
