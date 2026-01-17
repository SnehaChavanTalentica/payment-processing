# Observability Documentation

## Overview

This document describes the observability strategy for the Payment Processing System, covering metrics, distributed tracing, and logging.

## Metrics

### Available Metrics

The application exposes metrics via Spring Boot Actuator and Micrometer, available at:
- **Endpoint**: `/actuator/prometheus`

### Payment Metrics

| Metric Name | Type | Description | Labels |
|-------------|------|-------------|--------|
| `payment.transactions` | Counter | Total payment transactions | `type`, `status` |
| `subscription.operations` | Counter | Subscription operations | `type` |
| `gateway.transactions` | Counter | Gateway API calls | `gateway` |
| `gateway.response_time` | Timer | Gateway response times | `gateway` |

### JVM Metrics

| Metric Name | Description |
|-------------|-------------|
| `jvm.memory.used` | JVM memory usage |
| `jvm.memory.max` | JVM max memory |
| `jvm.gc.pause` | Garbage collection pauses |
| `jvm.threads.live` | Live thread count |

### HTTP Metrics

| Metric Name | Description |
|-------------|-------------|
| `http.server.requests` | HTTP request count and latency |
| `http.server.requests.active` | Active requests |

### Database Metrics

| Metric Name | Description |
|-------------|-------------|
| `hikaricp.connections.active` | Active DB connections |
| `hikaricp.connections.idle` | Idle DB connections |
| `hikaricp.connections.pending` | Pending connection requests |

### RabbitMQ Metrics

| Metric Name | Description |
|-------------|-------------|
| `rabbitmq.consumed` | Messages consumed |
| `rabbitmq.published` | Messages published |
| `rabbitmq.acknowledged` | Messages acknowledged |

## Distributed Tracing

### Correlation ID

Every request is assigned a correlation ID for end-to-end tracing.

**Header**: `X-Correlation-ID`
- If not provided, a UUID is generated
- Included in all log entries
- Returned in response headers
- Propagated to downstream services

### Trace Context

The application uses Micrometer Tracing with Brave for distributed tracing.

**Configuration**:
```yaml
management:
  tracing:
    sampling:
      probability: 1.0  # Sample 100% in dev, reduce in prod
```

### Zipkin Integration

Traces are exported to Zipkin for visualization.

**Access**: http://localhost:9411

**Features**:
- Request flow visualization
- Latency analysis
- Dependency graph
- Error tracking

## Logging

### Log Format

```
%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] [%X{correlationId:-}] [%X{traceId:-}] %-5level %logger{36} - %msg%n
```

### Log Levels

| Level | Usage |
|-------|-------|
| ERROR | Errors requiring attention |
| WARN | Potential issues |
| INFO | Business events |
| DEBUG | Detailed flow information |
| TRACE | Very detailed debugging |

### Log Categories

| Category | Default Level | Description |
|----------|---------------|-------------|
| `com.payment` | DEBUG | Application logs |
| `org.springframework.web` | INFO | Web framework |
| `org.springframework.security` | INFO | Security events |
| `org.hibernate.SQL` | DEBUG | SQL queries |

### Structured Logging

All logs include:
- Timestamp
- Thread name
- Correlation ID
- Trace ID (when available)
- Log level
- Logger name
- Message

### Audit Logging

Security and business events are stored in the `audit_logs` table:

| Field | Description |
|-------|-------------|
| `entity_type` | Type of entity (TRANSACTION, SUBSCRIPTION, SECURITY) |
| `entity_id` | ID of the affected entity |
| `action` | Action performed |
| `user_id` | User who performed the action |
| `user_ip` | Client IP address |
| `correlation_id` | Request correlation ID |
| `timestamp` | Event timestamp |

## Monitoring Recommendations

### Alerting Rules

#### Critical Alerts

| Condition | Threshold | Action |
|-----------|-----------|--------|
| Error rate > 5% | 5 min window | Page on-call |
| Gateway timeout | 10 failures/min | Investigate gateway |
| Database connections exhausted | > 90% used | Scale database |
| Queue depth growing | > 1000 messages | Scale consumers |

#### Warning Alerts

| Condition | Threshold | Action |
|-----------|-----------|--------|
| Latency p95 > 2s | 5 min window | Investigate |
| Memory usage > 80% | Sustained | Plan scaling |
| DLQ messages | Any | Review failures |

### Grafana Dashboards

#### Recommended Panels

1. **Overview Dashboard**
   - Request rate
   - Error rate
   - Latency percentiles
   - Active transactions

2. **Payment Dashboard**
   - Transaction success rate
   - Transaction types breakdown
   - Refund rate
   - Gateway response times

3. **Infrastructure Dashboard**
   - JVM memory
   - CPU usage
   - Database connections
   - Queue depth

### Health Checks

**Endpoint**: `/actuator/health`

Components checked:
- Database connectivity
- RabbitMQ connectivity
- Disk space
- Application state

**Sample Response**:
```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "rabbit": { "status": "UP" },
    "diskSpace": { "status": "UP" }
  }
}
```

## Prometheus Configuration

```yaml
# prometheus.yml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'payment-service'
    metrics_path: '/api/v1/actuator/prometheus'
    static_configs:
      - targets: ['payment-service:8080']
```

## Log Aggregation

For production deployments, consider:
- **ELK Stack** (Elasticsearch, Logstash, Kibana)
- **Loki + Grafana**
- **CloudWatch Logs** (AWS)
- **Stackdriver** (GCP)

### Log Shipping Configuration

```yaml
# Example for Filebeat
filebeat.inputs:
  - type: container
    paths:
      - /var/lib/docker/containers/*/*.log

output.elasticsearch:
  hosts: ["elasticsearch:9200"]
```

## Performance Baselines

### Expected Metrics

| Metric | Target | Acceptable |
|--------|--------|------------|
| Request latency (p50) | < 200ms | < 500ms |
| Request latency (p95) | < 500ms | < 1s |
| Request latency (p99) | < 1s | < 2s |
| Error rate | < 0.1% | < 1% |
| Availability | 99.9% | 99.5% |

### Gateway Performance

| Operation | Expected Latency |
|-----------|------------------|
| Purchase | 500-1000ms |
| Authorize | 500-1000ms |
| Capture | 300-500ms |
| Refund | 500-1000ms |
| Void | 300-500ms |

