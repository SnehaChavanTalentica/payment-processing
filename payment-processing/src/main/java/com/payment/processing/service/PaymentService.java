package com.payment.processing.service;

import com.payment.processing.dto.request.*;
import com.payment.processing.dto.response.TransactionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface PaymentService {
    TransactionResponse purchase(PaymentRequest request, String idempotencyKey, String correlationId);
    TransactionResponse authorize(PaymentRequest request, String idempotencyKey, String correlationId);
    TransactionResponse capture(CaptureRequest request, String idempotencyKey, String correlationId);
    TransactionResponse cancel(CancelRequest request, String idempotencyKey, String correlationId);
    TransactionResponse refund(RefundRequest request, String idempotencyKey, String correlationId);
    TransactionResponse getTransaction(UUID transactionId);
    Page<TransactionResponse> getTransactionsByCustomer(String customerId, Pageable pageable);
    Page<TransactionResponse> getTransactionsByOrder(String orderId, Pageable pageable);
}

