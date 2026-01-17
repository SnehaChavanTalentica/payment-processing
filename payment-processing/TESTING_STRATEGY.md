# Testing Strategy

## Overview

This document outlines the testing strategy for the Payment Processing System, ensuring comprehensive coverage and quality assurance.

## Testing Pyramid

```
          ┌──────────────────┐
          │    E2E Tests     │  (5%)
          │  (Integration)   │
         ┌┴──────────────────┴┐
         │  Integration Tests │ (15%)
         │   (API, Gateway)   │
        ┌┴────────────────────┴┐
        │     Unit Tests       │ (80%)
        │   (Service, Logic)   │
        └──────────────────────┘
```

## Test Coverage Target

- **Overall Coverage**: ≥ 80%
- **Service Layer**: ≥ 90%
- **Controller Layer**: ≥ 85%
- **Gateway Layer**: ≥ 80%

## Unit Testing

### Framework
- **JUnit 5**: Test framework
- **Mockito**: Mocking framework
- **AssertJ**: Fluent assertions

### Test Categories

#### Service Layer Tests

```java
@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {
    
    @Mock
    private TransactionRepository transactionRepository;
    
    @Mock
    private PaymentGateway paymentGateway;
    
    @InjectMocks
    private PaymentServiceImpl paymentService;
    
    @Test
    void purchase_ShouldCreateTransaction_WhenValid() {
        // Given
        PaymentRequest request = createValidPaymentRequest();
        when(paymentGateway.purchase(any())).thenReturn(successResponse());
        
        // When
        TransactionResponse response = paymentService.purchase(request, "key", "corr");
        
        // Then
        assertThat(response.getStatus()).isEqualTo(TransactionStatus.CAPTURED);
    }
}
```

#### Gateway Layer Tests

```java
@ExtendWith(MockitoExtension.class)
class AuthorizeNetGatewayTest {
    
    @Test
    void purchase_ShouldReturnSuccess_WhenApproved() {
        // Test gateway response parsing
    }
    
    @Test
    void purchase_ShouldReturnFailure_WhenDeclined() {
        // Test declined scenarios
    }
}
```

#### Controller Layer Tests

```java
@WebMvcTest(PaymentController.class)
class PaymentControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private PaymentService paymentService;
    
    @Test
    void purchase_ShouldReturn201_WhenSuccessful() {
        mockMvc.perform(post("/payments/purchase")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validPaymentJson()))
                .andExpect(status().isCreated());
    }
}
```

### Edge Cases Covered

| Category | Test Cases |
|----------|------------|
| **Validation** | Empty fields, invalid formats, boundary values |
| **Idempotency** | Duplicate requests, concurrent requests |
| **State Transitions** | Invalid captures, double refunds |
| **Error Handling** | Gateway timeouts, network errors |
| **Security** | Invalid tokens, expired tokens |

## Integration Testing

### Framework
- **Spring Boot Test**: Integration test support
- **Testcontainers**: Docker-based test dependencies
- **WireMock**: API mocking

### Test Scenarios

#### Database Integration

```java
@SpringBootTest
@Testcontainers
class TransactionRepositoryIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");
    
    @Test
    void shouldPersistTransaction() {
        // Test actual database operations
    }
}
```

#### Message Queue Integration

```java
@SpringBootTest
@Testcontainers
class WebhookProcessingIntegrationTest {
    
    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer();
    
    @Test
    void shouldProcessWebhookEvent() {
        // Test message queue processing
    }
}
```

#### API Integration

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class PaymentApiIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void fullPaymentFlowTest() {
        // Test complete payment flow
    }
}
```

### Gateway Mock Configuration

```java
@WireMockTest(httpPort = 8089)
class AuthorizeNetIntegrationTest {
    
    @Test
    void shouldHandleGatewayResponse(WireMockRuntimeInfo wmRuntimeInfo) {
        stubFor(post("/xml/v1/request.api")
            .willReturn(okXml(successResponse())));
        
        // Test gateway integration
    }
}
```

## Test Data Management

### Test Fixtures

```java
public class TestFixtures {
    
    public static PaymentRequest validPaymentRequest() {
        return PaymentRequest.builder()
            .orderId("TEST-ORDER-001")
            .customerId("CUST-001")
            .amount(new BigDecimal("100.00"))
            .cardNumber("4111111111111111")
            .expMonth("12")
            .expYear("2025")
            .cvv("123")
            .build();
    }
    
    public static Transaction authorizedTransaction() {
        return Transaction.builder()
            .status(TransactionStatus.AUTHORIZED)
            .amount(new BigDecimal("100.00"))
            .build();
    }
}
```

### Test Cards (Authorize.Net Sandbox)

| Card Number | Result |
|-------------|--------|
| 4111111111111111 | Approved |
| 4222222222222222 | Declined |
| 370000000000002 | AMEX Approved |
| 6011000000000012 | Discover Approved |

## Performance Testing

### Tools
- **JMeter**: Load testing
- **Gatling**: Performance scenarios

### Test Scenarios

| Scenario | Users | Duration | Target |
|----------|-------|----------|--------|
| Normal Load | 50 | 5 min | p95 < 500ms |
| Peak Load | 200 | 10 min | p95 < 1s |
| Stress Test | 500 | 15 min | No crashes |
| Endurance | 100 | 1 hour | Stable memory |

## Security Testing

### Test Areas

1. **Authentication**
   - Invalid credentials
   - Token expiration
   - Token manipulation

2. **Authorization**
   - Role-based access
   - Resource ownership

3. **Input Validation**
   - SQL injection
   - XSS prevention
   - Parameter tampering

4. **Rate Limiting**
   - Limit enforcement
   - Burst handling

## Continuous Integration

### Pipeline Stages

```yaml
stages:
  - build
  - test
  - coverage
  - integration
  - deploy

test:
  script:
    - ./mvnw test
  coverage: '/Total.*?([0-9]{1,3})%/'

integration:
  script:
    - ./mvnw verify -P integration-test
  services:
    - postgres:15
    - rabbitmq:3
```

### Coverage Enforcement

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <configuration>
        <rules>
            <rule>
                <element>BUNDLE</element>
                <limits>
                    <limit>
                        <counter>LINE</counter>
                        <value>COVEREDRATIO</value>
                        <minimum>0.80</minimum>
                    </limit>
                </limits>
            </rule>
        </rules>
    </configuration>
</plugin>
```

## Test Execution

### Running Tests

```bash
# All unit tests
./mvnw test

# Integration tests
./mvnw verify -P integration-test

# Coverage report
./mvnw test jacoco:report

# Specific test class
./mvnw test -Dtest=PaymentServiceImplTest

# Specific test method
./mvnw test -Dtest=PaymentServiceImplTest#purchase_ShouldCreateTransaction_WhenValid
```

### Viewing Reports

```bash
# Coverage report location
target/site/jacoco/index.html

# Surefire report
target/surefire-reports/
```

## Test Maintenance

### Best Practices

1. **Naming Convention**: `methodName_ShouldExpectedBehavior_WhenCondition`
2. **Single Assertion Principle**: One logical assertion per test
3. **Test Independence**: No shared state between tests
4. **Fast Execution**: Unit tests < 100ms each
5. **Deterministic**: No flaky tests

### Code Review Checklist

- [ ] Tests cover happy path
- [ ] Tests cover error scenarios
- [ ] Tests are independent
- [ ] Mocks are properly configured
- [ ] Assertions are meaningful
- [ ] No hardcoded test data

