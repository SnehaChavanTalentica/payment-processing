package com.payment.processing.gateway;

import com.payment.processing.config.AuthorizeNetProperties;
import com.payment.processing.dto.request.PaymentRequest;
import com.payment.processing.dto.request.SubscriptionRequest;
import com.payment.processing.dto.request.SubscriptionUpdateRequest;
import com.payment.processing.exception.GatewayException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import net.authorize.Environment;
import net.authorize.api.contract.v1.*;
import net.authorize.api.controller.*;
import net.authorize.api.controller.base.ApiOperationBase;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
@Slf4j
public class AuthorizeNetGateway implements PaymentGateway {

    private final AuthorizeNetProperties properties;
    private final Counter transactionCounter;
    private final Timer gatewayTimer;

    public AuthorizeNetGateway(AuthorizeNetProperties properties, MeterRegistry meterRegistry) {
        this.properties = properties;
        this.transactionCounter = Counter.builder("gateway.transactions")
                .tag("gateway", "authorize_net")
                .register(meterRegistry);
        this.gatewayTimer = Timer.builder("gateway.response_time")
                .tag("gateway", "authorize_net")
                .register(meterRegistry);

        Environment env = properties.isSandbox() ? Environment.SANDBOX : Environment.PRODUCTION;
        ApiOperationBase.setEnvironment(env);
    }

    private MerchantAuthenticationType getMerchantAuth() {
        MerchantAuthenticationType merchantAuth = new MerchantAuthenticationType();
        merchantAuth.setName(properties.getApiLoginId());
        merchantAuth.setTransactionKey(properties.getTransactionKey());
        return merchantAuth;
    }

    @Retryable(value = GatewayException.class, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    @Override
    public GatewayResponse purchase(PaymentRequest request) {
        log.info("Processing purchase for order: {}", request.getOrderId());
        return gatewayTimer.record(() -> {
            transactionCounter.increment();
            return executeTransaction(request, TransactionTypeEnum.AUTH_CAPTURE_TRANSACTION);
        });
    }

    @Override
    @Retryable(value = GatewayException.class, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public GatewayResponse authorize(PaymentRequest request) {
        log.info("Processing authorization for order: {}", request.getOrderId());
        return gatewayTimer.record(() -> {
            transactionCounter.increment();
            return executeTransaction(request, TransactionTypeEnum.AUTH_ONLY_TRANSACTION);
        });
    }

    private GatewayResponse executeTransaction(PaymentRequest request, TransactionTypeEnum transactionType) {
        CreditCardType creditCard = new CreditCardType();
        creditCard.setCardNumber(request.getCardNumber());
        creditCard.setExpirationDate(request.getExpYear() + "-" + request.getExpMonth());
        creditCard.setCardCode(request.getCvv());

        PaymentType payment = new PaymentType();
        payment.setCreditCard(creditCard);

        OrderType order = new OrderType();
        order.setInvoiceNumber(request.getOrderId());
        order.setDescription(request.getDescription());

        CustomerAddressType billingAddress = new CustomerAddressType();
        billingAddress.setFirstName(request.getBillingFirstName());
        billingAddress.setLastName(request.getBillingLastName());
        billingAddress.setAddress(request.getBillingAddress());
        billingAddress.setCity(request.getBillingCity());
        billingAddress.setState(request.getBillingState());
        billingAddress.setZip(request.getBillingZip());
        billingAddress.setCountry(request.getBillingCountry());

        CustomerDataType customerData = new CustomerDataType();
        customerData.setId(request.getCustomerId());
        customerData.setEmail(request.getCustomerEmail());

        TransactionRequestType transactionRequest = new TransactionRequestType();
        transactionRequest.setTransactionType(transactionType.value());
        transactionRequest.setAmount(request.getAmount().setScale(2, RoundingMode.HALF_UP));
        transactionRequest.setPayment(payment);
        transactionRequest.setOrder(order);
        transactionRequest.setBillTo(billingAddress);
        transactionRequest.setCustomer(customerData);

        CreateTransactionRequest apiRequest = new CreateTransactionRequest();
        apiRequest.setMerchantAuthentication(getMerchantAuth());
        apiRequest.setTransactionRequest(transactionRequest);

        CreateTransactionController controller = new CreateTransactionController(apiRequest);
        controller.execute();

        return parseTransactionResponse(controller.getApiResponse());
    }

    @Override
    @Retryable(value = GatewayException.class, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public GatewayResponse capture(String transactionId, BigDecimal amount) {
        log.info("Processing capture for transaction: {}", transactionId);
        return gatewayTimer.record(() -> {
            transactionCounter.increment();

            TransactionRequestType transactionRequest = new TransactionRequestType();
            transactionRequest.setTransactionType(TransactionTypeEnum.PRIOR_AUTH_CAPTURE_TRANSACTION.value());
            transactionRequest.setRefTransId(transactionId);
            if (amount != null) {
                transactionRequest.setAmount(amount.setScale(2, RoundingMode.HALF_UP));
            }

            CreateTransactionRequest apiRequest = new CreateTransactionRequest();
            apiRequest.setMerchantAuthentication(getMerchantAuth());
            apiRequest.setTransactionRequest(transactionRequest);

            CreateTransactionController controller = new CreateTransactionController(apiRequest);
            controller.execute();

            return parseTransactionResponse(controller.getApiResponse());
        });
    }

    @Override
    @Retryable(value = GatewayException.class, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public GatewayResponse voidTransaction(String transactionId) {
        log.info("Processing void for transaction: {}", transactionId);
        return gatewayTimer.record(() -> {
            transactionCounter.increment();

            TransactionRequestType transactionRequest = new TransactionRequestType();
            transactionRequest.setTransactionType(TransactionTypeEnum.VOID_TRANSACTION.value());
            transactionRequest.setRefTransId(transactionId);

            CreateTransactionRequest apiRequest = new CreateTransactionRequest();
            apiRequest.setMerchantAuthentication(getMerchantAuth());
            apiRequest.setTransactionRequest(transactionRequest);

            CreateTransactionController controller = new CreateTransactionController(apiRequest);
            controller.execute();

            return parseTransactionResponse(controller.getApiResponse());
        });
    }

    @Override
    @Retryable(value = GatewayException.class, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public GatewayResponse refund(String transactionId, BigDecimal amount, String cardLastFour) {
        log.info("Processing refund for transaction: {}", transactionId);
        return gatewayTimer.record(() -> {
            transactionCounter.increment();

            CreditCardType creditCard = new CreditCardType();
            creditCard.setCardNumber(cardLastFour);
            creditCard.setExpirationDate("XXXX");

            PaymentType payment = new PaymentType();
            payment.setCreditCard(creditCard);

            TransactionRequestType transactionRequest = new TransactionRequestType();
            transactionRequest.setTransactionType(TransactionTypeEnum.REFUND_TRANSACTION.value());
            transactionRequest.setRefTransId(transactionId);
            transactionRequest.setAmount(amount.setScale(2, RoundingMode.HALF_UP));
            transactionRequest.setPayment(payment);

            CreateTransactionRequest apiRequest = new CreateTransactionRequest();
            apiRequest.setMerchantAuthentication(getMerchantAuth());
            apiRequest.setTransactionRequest(transactionRequest);

            CreateTransactionController controller = new CreateTransactionController(apiRequest);
            controller.execute();

            return parseTransactionResponse(controller.getApiResponse());
        });
    }

    @Override
    public GatewayResponse createSubscription(SubscriptionRequest request) {
        log.info("Creating subscription for customer: {}", request.getCustomerId());
        return gatewayTimer.record(() -> {
            transactionCounter.increment();

            PaymentScheduleType schedule = new PaymentScheduleType();
            PaymentScheduleType.Interval interval = new PaymentScheduleType.Interval();

            int count = request.getIntervalCount() != null ? request.getIntervalCount() : 1;
            switch (request.getBillingInterval()) {
                case DAILY -> { interval.setLength((short) count); interval.setUnit(ARBSubscriptionUnitEnum.DAYS); }
                case WEEKLY -> { interval.setLength((short) (count * 7)); interval.setUnit(ARBSubscriptionUnitEnum.DAYS); }
                case MONTHLY -> { interval.setLength((short) count); interval.setUnit(ARBSubscriptionUnitEnum.MONTHS); }
                case YEARLY -> { interval.setLength((short) (count * 12)); interval.setUnit(ARBSubscriptionUnitEnum.MONTHS); }
            }
            schedule.setInterval(interval);

            try {
                schedule.setStartDate(javax.xml.datatype.DatatypeFactory.newInstance()
                        .newXMLGregorianCalendar(request.getStartDate() != null
                                ? request.getStartDate().toString()
                                : java.time.LocalDate.now().plusDays(1).toString()));
            } catch (Exception e) {
                throw new GatewayException("DATE_ERROR", "Invalid start date", e);
            }

            schedule.setTotalOccurrences((short) (request.getTotalCycles() != null ? request.getTotalCycles() : 9999));

            CreditCardType creditCard = new CreditCardType();
            creditCard.setCardNumber(request.getCardNumber());
            creditCard.setExpirationDate(request.getExpYear() + "-" + request.getExpMonth());
            creditCard.setCardCode(request.getCvv());

            PaymentType payment = new PaymentType();
            payment.setCreditCard(creditCard);

            NameAndAddressType billTo = new NameAndAddressType();
            billTo.setFirstName(request.getBillingFirstName());
            billTo.setLastName(request.getBillingLastName());
            billTo.setAddress(request.getBillingAddress());
            billTo.setCity(request.getBillingCity());
            billTo.setState(request.getBillingState());
            billTo.setZip(request.getBillingZip());
            billTo.setCountry(request.getBillingCountry());

            ARBSubscriptionType subscription = new ARBSubscriptionType();
            subscription.setName(request.getName());
            subscription.setPaymentSchedule(schedule);
            subscription.setAmount(request.getAmount().setScale(2, RoundingMode.HALF_UP));
            subscription.setPayment(payment);
            subscription.setBillTo(billTo);

            if (request.getTrialDays() != null && request.getTrialDays() > 0) {
                schedule.setTrialOccurrences(request.getTrialDays().shortValue());
                subscription.setTrialAmount(request.getTrialAmount() != null
                        ? request.getTrialAmount().setScale(2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO);
            }

            ARBCreateSubscriptionRequest apiRequest = new ARBCreateSubscriptionRequest();
            apiRequest.setMerchantAuthentication(getMerchantAuth());
            apiRequest.setSubscription(subscription);

            ARBCreateSubscriptionController controller = new ARBCreateSubscriptionController(apiRequest);
            controller.execute();

            return parseSubscriptionResponse(controller.getApiResponse());
        });
    }

    @Override
    public GatewayResponse updateSubscription(String subscriptionId, SubscriptionUpdateRequest request) {
        log.info("Updating subscription: {}", subscriptionId);
        return gatewayTimer.record(() -> {
            ARBSubscriptionType subscription = new ARBSubscriptionType();

            if (request.getName() != null) subscription.setName(request.getName());
            if (request.getAmount() != null) subscription.setAmount(request.getAmount().setScale(2, RoundingMode.HALF_UP));

            if (request.getCardNumber() != null) {
                CreditCardType creditCard = new CreditCardType();
                creditCard.setCardNumber(request.getCardNumber());
                creditCard.setExpirationDate(request.getExpYear() + "-" + request.getExpMonth());
                if (request.getCvv() != null) creditCard.setCardCode(request.getCvv());
                PaymentType payment = new PaymentType();
                payment.setCreditCard(creditCard);
                subscription.setPayment(payment);
            }

            ARBUpdateSubscriptionRequest apiRequest = new ARBUpdateSubscriptionRequest();
            apiRequest.setMerchantAuthentication(getMerchantAuth());
            apiRequest.setSubscriptionId(subscriptionId);
            apiRequest.setSubscription(subscription);

            ARBUpdateSubscriptionController controller = new ARBUpdateSubscriptionController(apiRequest);
            controller.execute();

            ARBUpdateSubscriptionResponse response = controller.getApiResponse();
            if (response != null && response.getMessages().getResultCode() == MessageTypeEnum.OK) {
                return GatewayResponse.builder().success(true).subscriptionId(subscriptionId).build();
            } else {
                String errorCode = response != null && response.getMessages().getMessage() != null
                        ? response.getMessages().getMessage().get(0).getCode() : "UNKNOWN";
                String errorMessage = response != null && response.getMessages().getMessage() != null
                        ? response.getMessages().getMessage().get(0).getText() : "Unknown error";
                return GatewayResponse.failure(errorCode, errorMessage);
            }
        });
    }

    @Override
    public GatewayResponse cancelSubscription(String subscriptionId) {
        log.info("Canceling subscription: {}", subscriptionId);
        return gatewayTimer.record(() -> {
            ARBCancelSubscriptionRequest apiRequest = new ARBCancelSubscriptionRequest();
            apiRequest.setMerchantAuthentication(getMerchantAuth());
            apiRequest.setSubscriptionId(subscriptionId);

            ARBCancelSubscriptionController controller = new ARBCancelSubscriptionController(apiRequest);
            controller.execute();

            ARBCancelSubscriptionResponse response = controller.getApiResponse();
            if (response != null && response.getMessages().getResultCode() == MessageTypeEnum.OK) {
                return GatewayResponse.builder().success(true).subscriptionId(subscriptionId).build();
            } else {
                String errorCode = response != null && response.getMessages().getMessage() != null
                        ? response.getMessages().getMessage().get(0).getCode() : "UNKNOWN";
                String errorMessage = response != null && response.getMessages().getMessage() != null
                        ? response.getMessages().getMessage().get(0).getText() : "Unknown error";
                return GatewayResponse.failure(errorCode, errorMessage);
            }
        });
    }

    @Override
    public GatewayResponse getSubscriptionStatus(String subscriptionId) {
        ARBGetSubscriptionStatusRequest apiRequest = new ARBGetSubscriptionStatusRequest();
        apiRequest.setMerchantAuthentication(getMerchantAuth());
        apiRequest.setSubscriptionId(subscriptionId);

        ARBGetSubscriptionStatusController controller = new ARBGetSubscriptionStatusController(apiRequest);
        controller.execute();

        ARBGetSubscriptionStatusResponse response = controller.getApiResponse();
        if (response != null && response.getMessages().getResultCode() == MessageTypeEnum.OK) {
            return GatewayResponse.builder()
                    .success(true)
                    .subscriptionId(subscriptionId)
                    .responseMessage(response.getStatus() != null ? response.getStatus().value() : "UNKNOWN")
                    .build();
        }
        return GatewayResponse.failure("UNKNOWN", "Failed to get subscription status");
    }

    @Override
    public boolean validateWebhookSignature(String payload, String signature) {
        if (properties.getSignatureKey() == null || properties.getSignatureKey().isEmpty()) {
            log.warn("Webhook signature key not configured, skipping validation");
            return true;
        }
        try {
            Mac hmac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(
                    properties.getSignatureKey().getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac.init(secretKey);
            byte[] hash = hmac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String computedSignature = Base64.getEncoder().encodeToString(hash);
            return computedSignature.equals(signature);
        } catch (Exception e) {
            log.error("Error validating webhook signature", e);
            return false;
        }
    }

    private GatewayResponse parseTransactionResponse(CreateTransactionResponse response) {
        if (response == null) {
            return GatewayResponse.failure("NULL_RESPONSE", "No response from gateway");
        }

        TransactionResponse transResult = response.getTransactionResponse();

        if (response.getMessages().getResultCode() == MessageTypeEnum.OK && transResult != null) {
            if ("1".equals(transResult.getResponseCode())) {
                return GatewayResponse.builder()
                        .success(true)
                        .transactionId(transResult.getTransId())
                        .authCode(transResult.getAuthCode())
                        .avsResult(transResult.getAvsResultCode())
                        .cvvResult(transResult.getCvvResultCode())
                        .responseCode(transResult.getResponseCode())
                        .responseMessage("Transaction approved")
                        .build();
            } else {
                String errorCode = transResult.getErrors() != null && !transResult.getErrors().getError().isEmpty()
                        ? transResult.getErrors().getError().get(0).getErrorCode()
                        : transResult.getResponseCode();
                String errorMessage = transResult.getErrors() != null && !transResult.getErrors().getError().isEmpty()
                        ? transResult.getErrors().getError().get(0).getErrorText()
                        : "Transaction declined";
                return GatewayResponse.failure(errorCode, errorMessage);
            }
        } else {
            String errorCode = response.getMessages().getMessage() != null
                    ? response.getMessages().getMessage().get(0).getCode() : "UNKNOWN";
            String errorMessage = response.getMessages().getMessage() != null
                    ? response.getMessages().getMessage().get(0).getText() : "Unknown error";
            return GatewayResponse.failure(errorCode, errorMessage);
        }
    }

    private GatewayResponse parseSubscriptionResponse(ARBCreateSubscriptionResponse response) {
        if (response == null) {
            return GatewayResponse.failure("NULL_RESPONSE", "No response from gateway");
        }

        if (response.getMessages().getResultCode() == MessageTypeEnum.OK) {
            return GatewayResponse.builder()
                    .success(true)
                    .subscriptionId(response.getSubscriptionId())
                    .customerProfileId(response.getProfile() != null ? response.getProfile().getCustomerProfileId() : null)
                    .paymentProfileId(response.getProfile() != null ? response.getProfile().getCustomerPaymentProfileId() : null)
                    .build();
        } else {
            String errorCode = response.getMessages().getMessage() != null
                    ? response.getMessages().getMessage().get(0).getCode() : "UNKNOWN";
            String errorMessage = response.getMessages().getMessage() != null
                    ? response.getMessages().getMessage().get(0).getText() : "Unknown error";
            return GatewayResponse.failure(errorCode, errorMessage);
        }
    }
}

