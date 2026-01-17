# Project Structure

## Overview

This project follows a clean, layered architecture pattern with clear separation of concerns.

```
payment-processing-system/
├── src/
│   ├── main/
│   │   ├── java/com/qnst/payment/
│   │   │   ├── PaymentApplication.java      # Main Spring Boot application
│   │   │   ├── audit/                       # Audit logging components
│   │   │   ├── config/                      # Configuration classes
│   │   │   ├── controller/                  # REST API controllers
│   │   │   ├── domain/                      # Domain model
│   │   │   │   ├── entity/                  # JPA entities
│   │   │   │   └── enums/                   # Enumeration types
���   │   │   ├── dto/                         # Data Transfer Objects
│   │   │   │   ├── request/                 # Request DTOs
│   │   │   │   └── response/                # Response DTOs
│   │   │   ├── exception/                   # Custom exceptions & handlers
│   │   │   ├── gateway/                     # Payment gateway integration
│   │   │   ├── queue/                       # Message queue consumers
│   │   │   ├── repository/                  # Data access layer
│   │   │   ├── security/                    # Security configuration
��   │   │   ├── service/                     # Business logic
│   │   │   │   └── impl/                    # Service implementations
│   │   │   └── webhook/                     # Webhook handling
│   │   └── resources/
│   │       ├── application.yml              # Main configuration
│   │       ├── application-local.yml        # Local profile config
│   │       └── application-prod.yml         # Production profile config
│   └── test/
│       └── java/com/qnst/payment/           # Test classes
├── docs/                                     # Documentation files
├── screenshots/                              # Screenshot evidence
├── docker-compose.yml                        # Docker orchestration
├── Dockerfile                                # Container definition
├── pom.xml                                   # Maven build file
└── README.md                                 # Project readme
```

## Module Descriptions

### audit/
Audit logging services for compliance and security monitoring.
- `AuditService.java` - Interface for audit operations
- `AuditServiceImpl.java` - Async audit logging implementation

### config/
Spring configuration classes.
- `AppConfig.java` - General app config (ObjectMapper, async executors)
- `AuthorizeNetProperties.java` - Authorize.Net configuration properties
- `OpenApiConfig.java` - Swagger/OpenAPI configuration
- `RabbitMQConfig.java` - Message queue configuration

### controller/
REST API endpoints following RESTful conventions.
- `PaymentController.java` - Payment transaction endpoints
- `SubscriptionController.java` - Subscription management endpoints
- `AuthController.java` - Authentication endpoints

### domain/entity/
JPA entities representing database tables.
- `Transaction.java` - Payment transaction entity
- `Subscription.java` - Recurring billing subscription
- `IdempotencyKey.java` - Request deduplication
- `WebhookEvent.java` - Webhook event storage
- `AuditLog.java` - Audit trail records
- `User.java` - User account entity
- `BaseEntity.java` - Common entity fields

### domain/enums/
Enumeration types for type-safe values.
- `TransactionStatus.java` - Transaction states
- `TransactionType.java` - Transaction operation types
- `SubscriptionStatus.java` - Subscription states
- `BillingInterval.java` - Recurring billing intervals
- `PaymentMethodType.java` - Payment method types
- `WebhookEventType.java` - Webhook event types

### dto/
Data Transfer Objects for API communication.
- **request/** - Input DTOs with validation
- **response/** - Output DTOs with formatting

### exception/
Custom exceptions and global error handling.
- `GlobalExceptionHandler.java` - Centralized exception handling
- Various exception classes for specific error scenarios

### gateway/
Payment gateway integration layer.
- `PaymentGateway.java` - Gateway interface
- `GatewayResponse.java` - Gateway response wrapper
- `AuthorizeNetGateway.java` - Authorize.Net implementation

### queue/
Message queue consumers for async processing.
- `WebhookEventConsumer.java` - RabbitMQ consumer for webhooks

### repository/
Spring Data JPA repositories.
- `TransactionRepository.java` - Transaction data access
- `SubscriptionRepository.java` - Subscription data access
- `WebhookEventRepository.java` - Webhook event data access
- `IdempotencyKeyRepository.java` - Idempotency key data access
- `AuditLogRepository.java` - Audit log data access

### security/
Security configuration and components.
- `SecurityConfig.java` - Spring Security configuration
- `JwtTokenProvider.java` - JWT token generation/validation
- `JwtAuthenticationFilter.java` - JWT authentication filter
- `JwtProperties.java` - JWT configuration properties
- `RateLimitFilter.java` - Rate limiting implementation
- `CorrelationIdFilter.java` - Correlation ID for tracing

### service/
Business logic layer.
- `PaymentService.java` - Payment operations interface
- `SubscriptionService.java` - Subscription operations interface
- `IdempotencyService.java` - Idempotency operations interface
- **impl/** - Service implementations

### webhook/
Webhook handling components.
- `WebhookController.java` - Webhook endpoint
- `WebhookProcessor.java` - Webhook event processing

## Key Design Patterns

1. **Repository Pattern** - Data access abstraction
2. **Service Layer Pattern** - Business logic encapsulation
3. **DTO Pattern** - API/internal separation
4. **Strategy Pattern** - Payment gateway abstraction
5. **Filter Chain** - Security and cross-cutting concerns

