package com.payment.processing.repository;

import com.payment.processing.domain.entity.Transaction;
import com.payment.processing.domain.enums.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);
    Optional<Transaction> findByGatewayTransactionId(String gatewayTransactionId);
    List<Transaction> findByOrderId(String orderId);
    Page<Transaction> findByCustomerId(String customerId, Pageable pageable);
    Page<Transaction> findByStatus(TransactionStatus status, Pageable pageable);
    boolean existsByIdempotencyKey(String idempotencyKey);
}

