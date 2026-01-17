package com.payment.processing.repository;

import com.payment.processing.domain.entity.Subscription;
import com.payment.processing.domain.enums.SubscriptionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    Optional<Subscription> findByGatewaySubscriptionId(String gatewaySubscriptionId);
    boolean existsByIdempotencyKey(String idempotencyKey);
    Page<Subscription> findByCustomerId(String customerId, Pageable pageable);
    Page<Subscription> findByStatus(SubscriptionStatus status, Pageable pageable);
}

