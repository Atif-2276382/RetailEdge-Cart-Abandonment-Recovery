package com.retailedge.repository;

import com.retailedge.entity.CartAbandonment;
import com.retailedge.entity.CartStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartAbandonmentRepository extends JpaRepository<CartAbandonment, UUID> {

    Optional<CartAbandonment> findByIdAndOwnerId(UUID id, UUID ownerId);

    List<CartAbandonment> findByOwnerIdAndStatusAndIdGreaterThanOrderByIdAsc(
	    UUID ownerId, CartStatus status, UUID cursor, Pageable pageable);

    List<CartAbandonment> findByOwnerIdAndStatusOrderByIdAsc(
	    UUID ownerId, CartStatus status, Pageable pageable);

    List<CartAbandonment> findByStatusAndLastActivityAtLessThanEqualAndIdGreaterThanOrderByIdAsc(
	    CartStatus status, OffsetDateTime cutoff, UUID cursor, Pageable pageable);

    List<CartAbandonment> findByStatusAndLastActivityAtLessThanEqualOrderByIdAsc(
	    CartStatus status, OffsetDateTime cutoff, Pageable pageable);
}