# Architecture Documentation

## System Architecture

### High-Level Architecture

```
┌─────────────────────────────────��───────────────────────────────────────┐
│                           Client Applications                            │
│                    (Web, Mobile, Third-party Services)                   │
└─────────────────────────────────────────────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         API Gateway / Load Balancer                      │
│                           (Rate Limiting, SSL)                           │
└───────────────────────────────��─────────────────────────────────────────┘
                                     │
                                     ▼
┌────────────────────────────────────────��────────────────────────────────┐
│                     Payment Processing Service                           │
│  ┌───────────────┐  ┌───────────────┐  ┌───────────────┐               │
│  │   REST API    │  │   Security    │  │  Correlation  │               │
│  │  Controllers  │  │   (JWT/Rate)  │  │    ID Filter  │               │
│  └───────┬───────┘  └───────────────┘  └───────────────┘               │
│          │                                                               │
│  ┌───────▼─────���──────────────────────────────────────────────────────┐│
│  │                        Service Layer                                ││
│  │  ┌────────────┐  ┌────────────────┐  ┌���───────────────────────┐   ││
│  │  │  Payment   │  │  Subscription  │  │     Idempotency        │   ││
│  │  │  Service   │  │    Service     │  │       Service          │   ││
│  │  └─────┬──────┘  └───────┬────────┘  └────────────────────────┘   ││
│  └────────┼─────────────────┼────────���───────────────────────────────┘│
│           │                 │                                          │
│  ┌────────▼─────────────────▼────────────────────────────────────────┐│
│  │                     Gateway Layer                                   ���│
│  │  ┌────────────────────────────────────────────────────────────┐   ││
│  │  │              Authorize.Net Gateway                          │   ││
│  │  │         (SDK Integration, Retry Logic)                      │   ││
│  │  └────────────────────────────────────────────────────────────┘   ││
│  └──────────────��────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────────────┘
          │                   │                      │
          ▼                   ▼                      ▼
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│   PostgreSQL    │  │    RabbitMQ     │  │  Authorize.Net  │
│    Database     │  │  Message Queue  │  │    Sandbox      │
└─────────────────┘  └─────────────────┘  └─────────────────┘
```

## Payment Flow Diagrams

### Purchase Flow (Auth + Capture)

```
Client           Payment Service        Gateway            Authorize.Net
  │                    │                   │                     │
  │ POST /purchase     │                   │                     │
  │───────────────────>│                   │                     │
  │                    │ Validate Request  │                     │
  │                    │ Check Idempotency │                     │
  │                    │ Create Transaction│                     │
  │                    │                   │                     │
  │                    │ Auth+Capture      │                     │
  │                    │──────────────────>│                     │
  │                    │                   │ createTransaction   │
  │                    │                   │────────────────────>│
  │                    │                   │                     │
  │                    │                   │    Response         │
  │                    │                   │<────────────────────│
  │                    │   GatewayResponse │                     │
  │                    │<──────────────────│                     │
  │                    │                   │                     │
  │                    │ Update Transaction│                     ���
  │                    │ Audit Log         │                     │
  │  TransactionResponse                   │                     │
  │<──��────────────────│                   │                     │
```

### Authorize & Capture Flow (Two-Step)

```
Client           Payment Service        Gateway            Authorize.Net
  │                    │                   │                     │
  │ POST /authorize    │                   │                     │
  │───────────────────>│                   │                     │
  │                    │ Authorize Only    │                     │
  │                    │─���────────────────>│                     │
  │                    │                   │ authOnlyTransaction │
  │                    │                   │──────────────���─────>│
  │                    │                   │<────────────────────│
  │                    │<──────────────────│                     │
  │<───────────────────│ (Status: AUTHORIZED)                    │
  │                    │                   │                     │
  │                    │                   │                     │
  │ POST /capture      │                   │                     │
  │───────────────────>│                   │                     │
  │                    │ Capture           │                     │
  │                    │──────────────────>│                     │
  │                    │                   │priorAuthCapture     │
  │                    │                   │────────────────────>│
  │                    │                   │<────────────────────│
  │                    │<──────────────────│                     │
  │<───────────────────│ (Status: CAPTURED)                      │
```

### Refund Flow

```
Client           Payment Service        Gateway            Authorize.Net
  │                    │                   │                     │
  │ POST /refund       │                   │                     │
  │────��──────────────>│                   │                     │
  │                    │ Validate State    │                     │
  │                    │ (canRefund?)      │                     │
  │                    │                   │                     │
  │                    │ Create Refund Tx  │                     │
  │                    │──────────────────>│                     │
  │                    │                   │ refundTransaction   │
  │                    │                   │────────────────────>│
  │                    │                   │<───────────��────────│
  │                    │<──────────────────│                     │
  │                    │                   │                     │
  │                    │ Update Original Tx│                     │
  │<───────────────────│ (Track Refunded Amount)                 │
```

### Webhook Processing Flow

```
Authorize.Net        Webhook Controller      RabbitMQ       Webhook Processor
     │                      │                   │                  │
     │ POST /webhooks       │                   │                  │
     │──��──────────────────>│                   │                  │
     │                      │ Validate Signature│                  │
     │                      │ Store Event       │                  │
     │                      │                   │                  │
     │                      │ Queue Event ID    │                  │
     │                      │──────────────────>│                  │
     │    200 OK            │                   │                  │
     │<─────────────────────│                   │                  │
     │                      │                   │                  │
     │                      │                   │ Consume Event    │
     │                      │                   │─────────────────>│
     │                      │                   │                  │
     │                      │                   │                  │ Process
     │                      │                   │                  │ Update DB
     │                      │                   │    ACK           │
     │                      ���                   │<─────────────────│
```

## Database Schema

### Entity Relationships

```
┌────────────────────┐       ┌────────────────────┐
│    Transaction     │       │    Subscription    │
├────────────────────┤       ├────────────────────┤
│ id (PK)            │       │ id (PK)            │
│ order_id           │       │ customer_id        │
│ customer_id        │       │ status             │
│ type               │       │ amount             │
│ status             │       │ billing_interval   │
│ amount             │       │ gateway_sub_id     │
│ gateway_trans_id   │       │ idempotency_key    │
│ parent_trans_id(FK)│◄──────│ ...                │
│ subscription_id(FK)│───────►│                    │
│ idempotency_key    │       └────────────────────┘
│ ...                │
└────────────────────┘
         │
         │ 1:N (parent-child)
         ▼
┌────────────────────┐       ┌────────────────────┐
│    Audit_Log       │       │  Idempotency_Key   │
├────────────────────┤       ├────────────────────┤
│ id (PK)            │       │ id (PK)            │
│ entity_type        │       │ key (UNIQUE)       │
│ entity_id          │       │ request_hash       │
│ action             │       │ response_body      │
│ correlation_id     │       │ expires_at         │
│ timestamp          │       │ ...                │
│ ...                │       └────────────────────┘
└────────────────────┘
                             ┌────────────────────┐
                             │   Webhook_Event    │
                             ├────────────────────┤
                             │ id (PK)            │
                             │ event_id (UNIQUE)  │
                             ��� event_type         │
                             │ payload            │
                             │ processed          │
                             │ ...                │
                             └─────────────────��──┘
```

## Design Decisions

### 1. Synchronous vs Asynchronous Processing

**Decision**: Hybrid approach
- **Synchronous**: Direct payment operations for immediate feedback
- **Asynchronous**: Webhook processing via RabbitMQ

**Rationale**: 
- Users need immediate payment confirmation
- Webhooks can be processed asynchronously for reliability
- Dead Letter Queues (DLQ) handle failed events

### 2. Retry Strategy

**Decision**: Exponential backoff with jitter

```
Attempt 1: 1 second
Attempt 2: 2 seconds
Attempt 3: 4 seconds
Max attempts: 3
```

**Rationale**: 
- Prevents thundering herd on gateway
- Allows transient failures to recover
- Limits impact of persistent failures

### 3. Idempotency Implementation

**Decision**: Client-provided idempotency keys with 24-hour TTL

**Rationale**:
- Prevents duplicate charges
- Allows safe retries
- Keys expire to limit storage

### 4. PCI DSS Compliance

**Strategy**:
- Never store full card numbers
- Only store last 4 digits and card brand
- Use Authorize.Net tokenization
- Encrypt sensitive data in transit (TLS)
- Comprehensive audit logging

### 5. Rate Limiting

**Decision**: Token bucket algorithm per client

```
100 requests/minute default
Configurable per endpoint
```

**Rationale**:
- Prevents abuse
- Protects gateway quota
- Allows burst traffic

## Security Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Security Layers                          │
├─────────────────────────────────────────────────────���───────┤
│  Layer 1: TLS Encryption (HTTPS)                            │
├─────────────────────────────────────────────────────────────┤
│  Layer 2: Rate Limiting (Token Bucket)                      │
├─────────────────────────────────────────────────────────────┤
│  Layer 3: JWT Authentication                                │
├─────────────────────────────────────────────────────────────┤
│  Layer 4: Correlation ID Tracking                           │
├─────────────────────────────────────────────────────────────┤
│  Layer 5: Input Validation                                  │
├─────────────────────────────────────────────────────────────┤
│  Layer 6: Audit Logging                                     │
└───────────────────────────────────���─────────────────────────┘
```

## Scalability Considerations

### Horizontal Scaling

- Stateless service design
- Database connection pooling
- Message queue for load distribution
- Idempotency support for retries

### Performance Optimizations

- Async audit logging
- Connection pooling (HikariCP)
- Efficient database indexes
- Caching where appropriate

## Error Handling Strategy

| Error Type | HTTP Status | Retry Strategy |
|------------|-------------|----------------|
| Validation Error | 400 | No retry |
| Authentication | 401 | No retry |
| Authorization | 403 | No retry |
| Not Found | 404 | No retry |
| Duplicate | 409 | Return cached |
| Rate Limited | 429 | Exponential backoff |
| Gateway Error | 502 | Retry with backoff |
| Server Error | 500 | Retry with backoff |

