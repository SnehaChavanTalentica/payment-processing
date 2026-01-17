# Payment Processing System

A production-grade payment processing backend service that integrates with Authorize.Net Sandbox API. This system handles comprehensive payment workflows including transactions, subscriptions, and compliance requirements.

## Features

- **Payment Transactions**: Purchase, Authorize, Capture, Void, Refund
- **Recurring Billing**: Subscription management with ARB (Automated Recurring Billing)
- **Idempotency**: Safe request retries with idempotency keys
- **Webhook Processing**: Async event processing via RabbitMQ
- **Security**: JWT authentication, rate limiting, PCI DSS compliance
- **Observability**: Distributed tracing, metrics, comprehensive logging

## Tech Stack

- **Language**: Java 17
- **Framework**: Spring Boot 3.2
- **Database**: PostgreSQL
- **Message Broker**: RabbitMQ
- **Payment Gateway**: Authorize.Net SDK
- **Security**: Spring Security with JWT
- **Documentation**: OpenAPI/Swagger

## Quick Start

### Prerequisites

- Java 17+
- Maven 3.8+
- Docker & Docker Compose
- Authorize.Net Sandbox Account

### Setup

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd payment-processing-system
   ```

2. **Configure environment variables**
   ```bash
   export AUTHNET_API_LOGIN_ID=your_api_login_id
   export AUTHNET_TRANSACTION_KEY=your_transaction_key
   export AUTHNET_SIGNATURE_KEY=your_signature_key
   ```

3. **Start with Docker Compose**
   ```bash
   docker-compose up -d
   ```

4. **Or run locally**
   ```bash
   # Start dependencies
   docker-compose up -d postgres rabbitmq
   
   # Run application
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
   ```

### Access Points

- **API**: http://localhost:8080/api/v1
- **Swagger UI**: http://localhost:8080/api/v1/swagger-ui.html
- **API Docs**: http://localhost:8080/api/v1/api-docs
- **Health Check**: http://localhost:8080/api/v1/actuator/health
- **Metrics**: http://localhost:8080/api/v1/actuator/prometheus
- **RabbitMQ Management**: http://localhost:15672 (guest/guest)
- **Grafana**: http://localhost:3000 (admin/admin)
- **Zipkin**: http://localhost:9411

## API Authentication

### Get JWT Token
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin123"}'
```

### Use Token
```bash
curl -X GET http://localhost:8080/api/v1/payments/{id} \
  -H "Authorization: Bearer <token>"
```

## API Endpoints

### Payments
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/payments/purchase` | One-step payment |
| POST | `/payments/authorize` | Hold funds |
| POST | `/payments/capture` | Settle authorized payment |
| POST | `/payments/cancel` | Void authorization |
| POST | `/payments/refund` | Full or partial refund |
| GET | `/payments/{id}` | Get transaction details |

### Subscriptions
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/subscriptions/create` | Create subscription |
| GET | `/subscriptions/{id}` | Get subscription |
| PUT | `/subscriptions/{id}` | Update subscription |
| DELETE | `/subscriptions/{id}` | Cancel subscription |

### Webhooks
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/webhooks/authorize-net` | Receive Authorize.Net webhooks |

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_HOST` | Database host | localhost |
| `DB_PORT` | Database port | 5432 |
| `DB_NAME` | Database name | payment_db |
| `DB_USERNAME` | Database user | payment_user |
| `DB_PASSWORD` | Database password | payment_pass |
| `RABBITMQ_HOST` | RabbitMQ host | localhost |
| `RABBITMQ_PORT` | RabbitMQ port | 5672 |
| `AUTHNET_API_LOGIN_ID` | Authorize.Net API Login ID | - |
| `AUTHNET_TRANSACTION_KEY` | Authorize.Net Transaction Key | - |
| `AUTHNET_SIGNATURE_KEY` | Authorize.Net Signature Key | - |
| `AUTHNET_SANDBOX` | Use sandbox environment | true |
| `JWT_SECRET_KEY` | JWT signing key | - |

## Testing

```bash
# Run all tests
./mvnw test

# Run with coverage
./mvnw test jacoco:report

# View coverage report
open target/site/jacoco/index.html
```

## Project Structure

See [PROJECT_STRUCTURE.md](PROJECT_STRUCTURE.md) for detailed structure.

## Architecture

See [ARCHITECTURE.md](ARCHITECTURE.md) for system architecture and design decisions.

## Observability

See [OBSERVABILITY.md](OBSERVABILITY.md) for metrics, tracing, and logging details.

## License

Proprietary - All rights reserved.

