# E-Commerce Microservices Platform - Complete Interview Q&A Guide

## Table of Contents
1. Project Overview Questions
2. Architecture & Design Questions
3. Technology Stack Questions
4. Microservices Deep Dive
5. Kafka & Event-Driven Questions
6. Database & Data Management
7. Security & Authentication
8. Saga Pattern & Distributed Transactions
9. Failure Handling & Resilience
10. Docker & Deployment
11. Monitoring & Observability
12. Performance & Scalability
13. Code-Level Questions
14. Troubleshooting & Debugging
15. Best Practices & Improvements

---

## 1. PROJECT OVERVIEW QUESTIONS

### Q1: Tell me about your project in 2 minutes.
**Answer**: 
"I built a production-ready e-commerce microservices platform that handles the complete order lifecycle from product browsing to delivery. The system consists of 10 microservices communicating asynchronously through Apache Kafka, implementing the Saga pattern for distributed transaction management.

Key highlights:
- Event-driven architecture with Kafka as the message broker
- OAuth2 authentication using Keycloak
- Fully containerized with Docker - 18 containers orchestrated via Docker Compose
- Complete observability with Zipkin for tracing, Prometheus for metrics, and Grafana for dashboards
- Implements reserve-deduct inventory pattern for consistency
- Handles failures gracefully with compensating transactions

The order flow takes approximately 100 seconds from creation to delivery, with each microservice handling its specific responsibility - payment processing, warehouse operations, shipping, and final delivery. All services are independently deployable and scalable."

---

### Q2: What problem does your project solve?
**Answer**:
"Traditional monolithic e-commerce systems face several challenges:
1. **Scalability**: Can't scale individual components independently
2. **Reliability**: Single point of failure
3. **Deployment**: Entire system needs redeployment for small changes
4. **Technology Lock-in**: Stuck with one tech stack

My project solves these by:
- **Independent Scaling**: Can scale payment service separately from inventory
- **Fault Isolation**: Payment service failure doesn't crash the entire system
- **Independent Deployment**: Can deploy order service without touching product service
- **Technology Freedom**: Product service uses MongoDB, Order service uses MySQL
- **Distributed Transactions**: Saga pattern ensures data consistency across services
- **Async Processing**: Kafka enables non-blocking operations"

---

### Q3: Why did you choose microservices over monolithic architecture?
**Answer**:
"I chose microservices for several reasons:

**Business Requirements**:
- Different teams can work on different services independently
- Services have different scaling needs (payment vs notification)
- Need for technology diversity (MongoDB for products, MySQL for orders)

**Technical Benefits**:
- **Scalability**: Scale only the services that need it (e.g., scale product service during high traffic)
- **Resilience**: Circuit breakers prevent cascading failures
- **Deployment**: Deploy services independently without downtime
- **Maintainability**: Smaller codebases are easier to understand and maintain

**Real-world Scenario**:
During Black Friday sales, product browsing increases 10x but orders only 3x. With microservices, I can scale product-service to 10 instances and order-service to 3 instances. In a monolith, I'd have to scale everything equally, wasting resources."

---

### Q4: What are the main challenges you faced and how did you solve them?
**Answer**:
"**Challenge 1: Distributed Transactions**
- Problem: How to ensure order, payment, and inventory are consistent across services?
- Solution: Implemented Saga pattern with compensating transactions. If payment fails, automatically release inventory reservation.

**Challenge 2: Data Consistency**
- Problem: Inventory deduction timing - when to actually reduce stock?
- Solution: Reserve-deduct pattern. Reserve on order creation, deduct only after delivery. This prevents overselling while allowing cancellations.

**Challenge 3: Service Communication**
- Problem: Synchronous calls create tight coupling and cascading failures
- Solution: Event-driven architecture with Kafka. Services publish events and don't wait for responses.

**Challenge 4: Debugging Distributed Systems**
- Problem: Request flows through 5+ services, hard to debug
- Solution: Implemented Zipkin distributed tracing. Every request gets a trace ID that flows through all services.

**Challenge 5: Duplicate Orders**
- Problem: Network retries or user double-clicks could create duplicate orders
- Solution: Idempotency using UUID keys. Same request twice returns same order."

---

### Q5: How many services are in your project and what does each do?
**Answer**:
"Total: 10 microservices + 8 infrastructure services = 18 containers

**Microservices (10)**:
1. **Eureka Server (8761)**: Service discovery - all services register here
2. **API Gateway (8081)**: Single entry point, validates OAuth2 tokens, routes requests
3. **Product Service (8082)**: Manages product catalog in MongoDB
4. **Order Service (8083)**: Saga coordinator, orchestrates entire order flow
5. **Inventory Service (8084)**: Stock management with reserve-deduct pattern
6. **Notification Service (8085)**: Logs all events, can send emails/SMS
7. **Payment Service (8086)**: Processes payments, has 5% random failure for testing
8. **Warehouse Service (8087)**: Simulates item picking and preparation
9. **Shipping Service (8088)**: Handles courier dispatch
10. **Delivery Service (8089)**: Marks order as delivered

**Infrastructure (8)**:
1. **Keycloak (8080)**: OAuth2 authentication server
2. **Kafka (9092)**: Message broker for async communication
3. **Zookeeper (2181)**: Kafka cluster coordination
4. **MySQL (3307)**: Stores orders and inventory
5. **MongoDB (27017)**: Stores products
6. **Zipkin (9411)**: Distributed tracing
7. **Prometheus (9090)**: Metrics collection
8. **Grafana (3000)**: Metrics visualization"

---

## 2. ARCHITECTURE & DESIGN QUESTIONS

### Q6: Explain your system architecture.
**Answer**:
"My architecture has 6 layers:

**Layer 1: Security Layer**
- Keycloak handles all authentication
- Issues JWT Bearer tokens
- Validates tokens at API Gateway

**Layer 2: API Layer**
- API Gateway: Single entry point, validates tokens, routes requests
- Eureka Server: Service discovery, all services register here

**Layer 3: Microservices Layer**
- 10 independent services
- Each has its own database (database per service pattern)
- Communicate via Kafka events

**Layer 4: Messaging Layer**
- Kafka: 8 topics for different event types
- Zookeeper: Manages Kafka cluster

**Layer 5: Data Layer**
- MySQL: Orders and Inventory (ACID transactions needed)
- MongoDB: Products (flexible schema, no joins needed)

**Layer 6: Observability Layer**
- Zipkin: Traces requests across services
- Prometheus: Collects metrics from all services
- Grafana: Visualizes metrics in dashboards

**Communication**:
- Synchronous: Client → Gateway → Service (REST)
- Asynchronous: Service → Kafka → Service (Events)"

---

### Q7: Why did you use event-driven architecture?
**Answer**:
"I chose event-driven architecture for several critical reasons:

**1. Loose Coupling**
- Services don't need to know about each other
- Order service publishes 'order-created' event
- Inventory service subscribes to it
- They never directly call each other

**2. Scalability**
- Can add new consumers without changing producers
- Example: Added notification service later, just subscribed to events

**3. Resilience**
- If inventory service is down, events queue in Kafka
- When it comes back up, processes all missed events
- No data loss

**4. Async Processing**
- Order service doesn't wait for payment to complete
- Returns response immediately
- Payment happens in background

**5. Audit Trail**
- All events stored in Kafka
- Complete history of what happened
- Can replay events if needed

**Real Example**:
When order is created:
- Order service publishes event and returns immediately (100ms)
- Inventory, Payment, Notification all process in parallel
- User gets instant response, processing happens async"

---

### Q8: What is the Saga pattern and why did you use it?
**Answer**:
"**What is Saga Pattern?**
A design pattern for managing distributed transactions across microservices. Instead of a single ACID transaction, it uses a sequence of local transactions with compensating actions.

**Why I Used It?**
In microservices, you can't use traditional database transactions across services. My order flow involves:
- Order Service (MySQL)
- Inventory Service (MySQL)
- Payment Service (no DB)

Can't wrap all three in one transaction because they're separate databases and services.

**My Implementation - Orchestration-based Saga**:
Order Service acts as the saga coordinator:

1. **Create Order** (local transaction)
2. **Reserve Inventory** (publish event)
3. **Process Payment** (publish event)
4. **If payment succeeds**: Continue to warehouse
5. **If payment fails**: Execute compensating transaction (release inventory)

**Compensating Transaction Example**:
```
Normal Flow:
Order Created → Inventory Reserved → Payment Success → Continue

Failure Flow:
Order Created → Inventory Reserved → Payment Failed → 
Release Inventory (compensating) → Cancel Order
```

**Code Flow**:
```java
// Order Service - Payment Failure Handler
if (paymentEvent.getStatus() == FAILED) {
    // Compensating transaction
    order.setStatus(CANCELLED);
    releaseInventoryReservation(order);
    notifyCustomer(order);
}
```

**Benefits**:
- Data consistency across services
- Automatic rollback on failures
- No distributed locks needed
- Each service maintains its own data integrity"

---

### Q9: What design patterns did you use in your project?
**Answer**:
"I used 8 key design patterns:

**1. Saga Pattern**
- For distributed transactions
- Compensating transactions on failure
- Used in: Order flow coordination

**2. Event Sourcing**
- All state changes published as events
- Complete audit trail
- Used in: Order status changes

**3. Database Per Service**
- Each service has its own database
- Data isolation and independence
- Order Service: MySQL, Product Service: MongoDB

**4. API Gateway Pattern**
- Single entry point for clients
- Centralized authentication
- Request routing

**5. Service Discovery Pattern**
- Eureka for dynamic service registration
- Services find each other at runtime
- No hardcoded URLs

**6. Circuit Breaker Pattern**
- Resilience4j implementation
- Prevents cascading failures
- Used in: Order Service external calls

**7. Retry Pattern**
- Automatic retry on transient failures
- Exponential backoff
- Used in: Kafka message processing

**8. Idempotency Pattern**
- UUID-based idempotency keys
- Prevents duplicate orders
- Database unique constraint enforcement"

---

### Q10: How do your services communicate with each other?
**Answer**:
"I use two communication patterns:

**1. Synchronous Communication (REST)**
- **When**: Client needs immediate response
- **How**: HTTP REST APIs
- **Example**: 
  - Client → API Gateway → Product Service (Get Products)
  - Response flows back immediately

**Flow**:
```
Client → Gateway (validates token) → Eureka (discovers service) 
→ Product Service → MongoDB → Response back
```

**2. Asynchronous Communication (Kafka)**
- **When**: Long-running operations, no immediate response needed
- **How**: Kafka events
- **Example**: Order processing flow

**Flow**:
```
Order Service → Kafka Topic → Payment Service
Payment Service → Kafka Topic → Order Service
```

**Why Both?**
- **Synchronous**: For queries (GET products, GET order status)
- **Asynchronous**: For commands (Create order, Process payment)

**Kafka Topics (8)**:
1. order-events - Order created
2. order-payment-events - Trigger payment
3. payment-events - Payment result
4. order-status-events - Status changes
5. warehouse-events - Warehouse result
6. shipping-events - Shipping result
7. delivery-events - Delivery result
8. inventory-reservation-events - Stock operations

**Benefits**:
- Synchronous: Simple, immediate feedback
- Asynchronous: Scalable, resilient, decoupled"

---

### Q11: Why did you choose Kafka over RabbitMQ or other message brokers?
**Answer**:
"I chose Kafka over RabbitMQ for specific reasons:

**Kafka Advantages for My Use Case**:

**1. Event Streaming**
- Kafka stores events permanently (configurable retention)
- Can replay events if needed
- RabbitMQ deletes messages after consumption

**2. High Throughput**
- Kafka handles millions of messages/second
- Better for high-volume e-commerce scenarios
- RabbitMQ better for low-latency, low-volume

**3. Multiple Consumers**
- Same event consumed by multiple services
- Order-status-events consumed by: Warehouse, Shipping, Delivery, Notification
- RabbitMQ requires complex routing for this

**4. Scalability**
- Kafka partitions enable horizontal scaling
- Can add more consumers to same consumer group
- Load balances automatically

**5. Audit Trail**
- All events stored in Kafka
- Complete history of order lifecycle
- Can debug issues by replaying events

**When I'd Choose RabbitMQ**:
- Need complex routing (topic exchanges, headers)
- Need message priority
- Need low latency (< 10ms)
- Lower message volume

**My Scenario**:
- High volume of orders
- Multiple services need same events
- Need event history for debugging
- Throughput > Latency

**Result**: Kafka was the right choice for event-driven e-commerce platform."

---

### Q12: Explain the database per service pattern. Why not a shared database?
**Answer**:
"**Database Per Service Pattern**: Each microservice has its own database that only it can access.

**My Implementation**:
- Order Service → MySQL (orderdb)
- Inventory Service → MySQL (inventorydb)
- Product Service → MongoDB (productdb)

**Why Not Shared Database?**

**Problems with Shared Database**:
1. **Tight Coupling**: Services coupled through database schema
2. **No Independent Deployment**: Schema change affects all services
3. **Scaling Issues**: Can't scale databases independently
4. **Technology Lock-in**: All services must use same database
5. **Transaction Boundaries**: Hard to maintain service boundaries

**Benefits of Database Per Service**:

**1. Independence**
- Order Service can change its schema without affecting Product Service
- Can deploy independently

**2. Technology Freedom**
- Product Service uses MongoDB (flexible schema for products)
- Order Service uses MySQL (ACID transactions for orders)
- Choose best database for each use case

**3. Scalability**
- Can scale Product database separately (read-heavy)
- Can scale Order database separately (write-heavy)

**4. Fault Isolation**
- If Product database goes down, Order Service still works
- Failures don't cascade

**Challenges & Solutions**:

**Challenge 1: No Joins Across Services**
- Solution: Denormalize data, store productId in orders
- Accept eventual consistency

**Challenge 2: Distributed Transactions**
- Solution: Saga pattern with compensating transactions

**Challenge 3: Data Duplication**
- Solution: Accept it as trade-off for independence
- Use events to keep data synchronized

**Real Example**:
```
Order Service stores:
- orderId, productId, quantity, price

Product Service stores:
- productId, name, description, price

No foreign key between them!
If need product details, call Product Service API or subscribe to product-events
```

**Result**: Loose coupling, independent scaling, technology freedom outweigh the complexity."

---

## 3. TECHNOLOGY STACK QUESTIONS

### Q13: Why did you choose Spring Boot?
**Answer**:
"I chose Spring Boot for several reasons:

**1. Microservices Support**
- Spring Cloud for service discovery (Eureka)
- Spring Cloud Gateway for API Gateway
- Built-in support for distributed systems

**2. Production-Ready Features**
- Spring Boot Actuator for health checks
- Metrics integration with Prometheus
- Distributed tracing with Zipkin

**3. Ecosystem**
- Spring Data JPA for MySQL
- Spring Data MongoDB for MongoDB
- Spring Kafka for event streaming
- Spring Security OAuth2 for authentication

**4. Convention Over Configuration**
- Auto-configuration reduces boilerplate
- Embedded servers (Tomcat)
- No XML configuration needed

**5. Community & Support**
- Large community
- Extensive documentation
- Industry standard for Java microservices

**6. Developer Productivity**
- Fast development with Spring Boot starters
- Hot reload with DevTools
- Easy testing with Spring Test

**Alternatives Considered**:
- **Node.js**: Good for I/O heavy, but Java better for complex business logic
- **Go**: Fast but less ecosystem support
- **.NET Core**: Good but team expertise in Java

**Result**: Spring Boot provided the best balance of productivity, ecosystem, and enterprise features."

---

### Q14: Why MySQL for orders and MongoDB for products?
**Answer**:
"I chose different databases based on data characteristics:

**MySQL for Orders & Inventory - Why?**

**1. ACID Transactions Required**
- Orders involve money - need strong consistency
- Inventory updates must be atomic
- Can't afford data loss or inconsistency

**2. Relational Data**
- Orders have line items (1:N relationship)
- Need foreign keys and referential integrity
- Complex queries with joins

**3. Structured Schema**
- Order structure is fixed and well-defined
- Schema doesn't change frequently

**Example**:
```sql
-- Atomic inventory update
UPDATE inventory 
SET reserved_quantity = reserved_quantity + 3
WHERE product_id = 'prod-123' 
AND (quantity - reserved_quantity) >= 3;
-- Either succeeds completely or fails completely
```

**MongoDB for Products - Why?**

**1. Flexible Schema**
- Products have varying attributes
- Electronics: RAM, Storage, Screen Size
- Clothing: Size, Color, Material
- Books: Author, ISBN, Pages
- Can't fit all in fixed schema

**2. No Complex Joins**
- Products are standalone documents
- No relationships to other entities
- Simple CRUD operations

**3. Read-Heavy Workload**
- Product browsing is 10x more than ordering
- MongoDB excellent for read performance
- Can scale horizontally easily

**4. JSON-like Structure**
- Products naturally fit JSON format
- Easy to add new fields without migration

**Example**:
```json
{
  "_id": "prod-123",
  "name": "iPhone 15",
  "specs": {
    "storage": "256GB",
    "color": "Blue",
    "camera": "48MP"
  },
  "reviews": [...]
}
```

**Could I Use One Database?**
- Yes, but suboptimal
- MySQL for products: Rigid schema, hard to add new product types
- MongoDB for orders: No ACID, risk of data inconsistency

**Result**: Right tool for the right job - MySQL for transactional data, MongoDB for flexible documents."

---

### Q15: Explain your choice of Keycloak for authentication.
**Answer**:
"I chose Keycloak over building custom authentication for several reasons:

**Why Keycloak?**

**1. Industry Standard OAuth2/OpenID Connect**
- Implements OAuth2 and OpenID Connect protocols
- No need to implement complex security protocols myself
- Battle-tested and secure

**2. Production-Ready Features**
- User management UI
- Role-based access control
- Multi-factor authentication support
- Social login (Google, Facebook) ready
- Password policies and validation

**3. Token Management**
- Automatic token generation
- Token refresh mechanism
- Token revocation
- Configurable expiry times

**4. Centralized Authentication**
- Single source of truth for users
- All microservices validate tokens with Keycloak
- No user data duplication across services

**5. Scalability**
- Can cluster Keycloak for high availability
- Handles thousands of authentications/second

**My Implementation**:
```
Realm: ecommerce
Client: backend-service
Client Secret: ecommerce-secret-2024
Test User: testuser/test123

Token Flow:
1. Client → Keycloak: username/password
2. Keycloak → Client: access_token (5 min), refresh_token (30 min)
3. Client → API Gateway: Bearer token
4. Gateway → Keycloak: Validate token
5. Gateway → Service: Forward request
```

**Alternatives Considered**:
- **Custom JWT**: Too much work, security risks
- **Spring Security**: Good but Keycloak more feature-rich
- **Auth0**: Paid service, Keycloak is free and self-hosted

**Benefits in My Project**:
- Saved 2-3 weeks of development time
- Enterprise-grade security out of the box
- Easy to add new users/roles
- Can integrate social login later

**Result**: Keycloak provided enterprise authentication without building it from scratch."

---

### Q16: Why Java 17? Why not Java 8 or Java 21?
**Answer**:
"I chose Java 17 for specific reasons:

**Why Not Java 8?**
- Released in 2014, outdated
- Missing modern features
- No longer receiving free updates
- Performance improvements in newer versions

**Why Java 17?**

**1. LTS (Long Term Support)**
- Supported until 2029
- Production-ready and stable
- Most enterprises use LTS versions

**2. Modern Language Features**
- Records (immutable data classes)
- Pattern Matching
- Text Blocks
- Sealed Classes

**3. Performance**
- Better garbage collection (G1GC improvements)
- Faster startup time
- Lower memory footprint

**4. Spring Boot 3.x Requirement**
- Spring Boot 3.2 requires Java 17 minimum
- Latest Spring features need Java 17

**5. Industry Adoption**
- Most companies migrating to Java 17
- Good balance of modern and stable

**Example - Records in My Code**:
```java
// Java 8 way
public class OrderEvent {
    private final Long orderId;
    private final String productId;
    // constructor, getters, equals, hashCode, toString
}

// Java 17 way
public record OrderEvent(Long orderId, String productId) {}
// Auto-generates everything!
```

**Why Not Java 21?**
- Too new (released Sept 2023)
- Not all libraries fully support it yet
- Java 17 more stable for production
- Would use Java 21 for new projects in 2025

**Result**: Java 17 provides modern features with LTS stability - perfect for production microservices."

---

### Q17: Explain your use of Resilience4j.
**Answer**:
"Resilience4j provides fault tolerance patterns in my microservices.

**What I Implemented**:

**1. Circuit Breaker**
```java
@CircuitBreaker(name = "orderService", fallbackMethod = "fallbackCreateOrder")
public Order createOrder(Order order) {
    // Normal order creation
}

public Order fallbackCreateOrder(Order order, Exception e) {
    // Return cached data or error response
    Order failedOrder = new Order();
    failedOrder.setStatus(CANCELLED);
    return failedOrder;
}
```

**Configuration**:
```properties
resilience4j.circuitbreaker.instances.orderService.sliding-window-size=10
resilience4j.circuitbreaker.instances.orderService.failure-rate-threshold=50
```

**How It Works**:
- Monitors last 10 requests
- If 50% fail, circuit opens
- Stops calling failing service
- Calls fallback method instead
- Prevents cascading failures

**2. Retry**
```java
@Retry(name = "orderService")
public Order createOrder(Order order) {
    // Retries automatically on failure
}
```

**Configuration**:
```properties
resilience4j.retry.instances.orderService.max-attempts=3
resilience4j.retry.instances.orderService.wait-duration=2s
```

**How It Works**:
- Retries 3 times on failure
- Waits 2s, 4s, 8s (exponential backoff)
- Good for transient failures (network glitches)

**3. Bulkhead**
- Limits concurrent calls to a service
- Prevents resource exhaustion
- Max 10 concurrent calls to inventory service

**4. Rate Limiter**
- Limits requests per second
- Prevents overload
- Max 10 requests/second per service

**Real Scenario**:
```
Payment Service is down:
1. Circuit Breaker detects 50% failures
2. Opens circuit, stops calling Payment Service
3. Calls fallback method
4. Returns error to user: "Payment service unavailable"
5. After 60s, tries again (half-open state)
6. If succeeds, closes circuit
7. If fails, stays open

Without Circuit Breaker:
- Every request waits for timeout (30s)
- Threads blocked
- System becomes unresponsive
- Cascading failure to other services
```

**Benefits**:
- Prevents cascading failures
- Graceful degradation
- System stays responsive
- Automatic recovery

**Result**: Resilience4j makes my microservices fault-tolerant and production-ready."

---

This is Part 1. Continue to next part for more questions...

## 4. MICROSERVICES DEEP DIVE

### Q18: Explain the Order Service in detail - it's the most complex service.
**Answer**:
"Order Service is the saga coordinator - the brain of the system.

**Responsibilities**:
1. Create orders
2. Orchestrate entire order flow
3. Listen to events from other services
4. Update order status
5. Handle failures and cancellations
6. Implement idempotency

**Key Components**:

**1. OrderController** (REST endpoints):
```java
POST /api/orders - Create order
GET /api/orders/{id} - Get order
GET /api/orders - Get all orders
GET /api/orders/paged - Paginated orders
PATCH /api/orders/{id}/status - Update status
DELETE /api/orders/{id}/cancel - Cancel order
```

**2. OrderService** (Business logic):
- Creates order with PENDING status
- Generates idempotency key (UUID)
- Calculates total amount
- Publishes events to Kafka
- Implements circuit breaker and retry

**3. Event Listeners**:
- PaymentEventListener - Listens to payment-events
- WarehouseEventListener - Listens to warehouse-events
- ShippingEventListener - Listens to shipping-events
- DeliveryEventListener - Listens to delivery-events

**Order Creation Flow**:
```java
public Order createOrder(Order order) {
    // 1. Idempotency check
    if (order.getIdempotencyKey() != null) {
        Order existing = repository.findByIdempotencyKey(
            order.getIdempotencyKey()
        );
        if (existing != null) return existing;
    } else {
        order.setIdempotencyKey(UUID.randomUUID().toString());
    }
    
    // 2. Calculate total
    BigDecimal total = order.getLineItems().stream()
        .map(item -> item.getPrice().multiply(
            BigDecimal.valueOf(item.getQuantity())
        ))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    order.setTotalAmount(total);
    
    // 3. Set initial status
    order.setStatus(OrderStatus.PENDING);
    order.setPaymentStatus(PaymentStatus.PENDING);
    
    // 4. Save to database
    Order saved = repository.save(order);
    
    // 5. Publish events
    // For inventory reservation
    saved.getLineItems().forEach(item -> 
        kafkaTemplate.send("order-events", 
            new OrderPlacedEvent(saved.getId(), 
                item.getProductId(), item.getQuantity())
        )
    );
    
    // For payment processing
    paymentKafkaTemplate.send("order-payment-events", 
        new PaymentEvent(saved.getId(), saved.getTotalAmount(), 
            saved.getIdempotencyKey())
    );
    
    return saved;
}
```

**Event Handling Example - Payment Result**:
```java
@KafkaListener(topics = "payment-events")
public void handlePaymentEvent(PaymentEvent event) {
    Order order = repository.findById(event.getOrderId())
        .orElseThrow();
    
    if (event.getStatus() == PaymentStatus.COMPLETED) {
        // Payment successful
        order.setStatus(OrderStatus.CONFIRMED);
        order.setPaymentStatus(PaymentStatus.COMPLETED);
        order.setConfirmedAt(LocalDateTime.now());
        order.setPaymentTransactionId(event.getTransactionId());
        repository.save(order);
        
        // Notify warehouse to start processing
        kafkaTemplate.send("order-status-events", 
            new OrderStatusEvent(order.getId(), "CONFIRMED")
        );
    } else {
        // Payment failed - compensating transaction
        order.setStatus(OrderStatus.CANCELLED);
        order.setPaymentStatus(PaymentStatus.FAILED);
        repository.save(order);
        
        // Release inventory
        order.getLineItems().forEach(item -> {
            kafkaTemplate.send("inventory-reservation-events",
                new InventoryReleaseEvent(order.getId(), 
                    item.getProductId(), item.getQuantity())
            );
        });
    }
}
```

**Database Schema**:
```sql
orders (
    id, idempotency_key UNIQUE, status, payment_status,
    total_amount, payment_transaction_id,
    created_at, confirmed_at, processing_at, 
    shipped_at, delivered_at, cancelled_at
)

order_line_items (
    id, order_id FK, product_id, quantity, price
)
```

**Resilience Features**:
- Circuit Breaker: Stops calling failing services
- Retry: 3 attempts with exponential backoff
- Fallback: Returns error order if all fails

**Result**: Order Service orchestrates the entire saga, handling both success and failure scenarios."

---

### Q19: Explain the Inventory Service and the reserve-deduct pattern.
**Answer**:
"Inventory Service manages stock with a two-phase approach: reserve first, deduct later.

**Why Reserve-Deduct Pattern?**

**Problem Without It**:
```
User orders 3 items
Immediately deduct from stock: 50 → 47
User cancels order
Need to add back: 47 → 50
What if multiple cancellations? Race conditions!
```

**Solution - Reserve-Deduct**:
```
Order Created: Reserve 3 items
Stock: quantity=50, reserved=3, available=47

Order Delivered: Deduct 3 items
Stock: quantity=47, reserved=0, available=47

Order Cancelled: Release 3 items
Stock: quantity=50, reserved=0, available=50
```

**Database Schema**:
```sql
CREATE TABLE inventory (
    id BIGINT PRIMARY KEY,
    product_id VARCHAR(255) UNIQUE,
    quantity INTEGER NOT NULL,
    reserved_quantity INTEGER NOT NULL DEFAULT 0,
    updated_at TIMESTAMP,
    CHECK (reserved_quantity <= quantity),
    CHECK (quantity >= 0)
);
```

**Formula**: `available = quantity - reserved_quantity`

**Three Operations**:

**1. Reserve Stock** (Order Created):
```java
@Transactional
public void reserveStock(String productId, int qty) {
    Inventory inv = repository.findByProductId(productId)
        .orElseThrow();
    
    int available = inv.getQuantity() - inv.getReservedQuantity();
    if (available < qty) {
        throw new InsufficientStockException();
    }
    
    inv.setReservedQuantity(inv.getReservedQuantity() + qty);
    repository.save(inv);
}
```

**SQL**:
```sql
UPDATE inventory 
SET reserved_quantity = reserved_quantity + 3
WHERE product_id = 'prod-123'
AND (quantity - reserved_quantity) >= 3;
```

**2. Deduct Stock** (Order Delivered):
```java
@Transactional
public void deductStock(String productId, int qty) {
    Inventory inv = repository.findByProductId(productId)
        .orElseThrow();
    
    inv.setQuantity(inv.getQuantity() - qty);
    inv.setReservedQuantity(inv.getReservedQuantity() - qty);
    repository.save(inv);
}
```

**SQL**:
```sql
UPDATE inventory 
SET quantity = quantity - 3,
    reserved_quantity = reserved_quantity - 3
WHERE product_id = 'prod-123';
```

**3. Release Stock** (Order Cancelled):
```java
@Transactional
public void releaseStock(String productId, int qty) {
    Inventory inv = repository.findByProductId(productId)
        .orElseThrow();
    
    inv.setReservedQuantity(inv.getReservedQuantity() - qty);
    repository.save(inv);
}
```

**SQL**:
```sql
UPDATE inventory 
SET reserved_quantity = reserved_quantity - 3
WHERE product_id = 'prod-123';
```

**Event Listeners**:
```java
// Reserve on order creation
@KafkaListener(topics = "order-events")
public void handleOrderEvent(OrderPlacedEvent event) {
    reserveStock(event.getProductId(), event.getQuantity());
}

// Deduct or release based on action
@KafkaListener(topics = "inventory-reservation-events")
public void handleReservationEvent(InventoryReservationEvent event) {
    if (event.getAction().equals("DEDUCT")) {
        deductStock(event.getProductId(), event.getQuantity());
    } else if (event.getAction().equals("RELEASE")) {
        releaseStock(event.getProductId(), event.getQuantity());
    }
}
```

**Complete Flow Example**:
```
Initial: quantity=50, reserved=0, available=50

Order 1 Created (3 items):
quantity=50, reserved=3, available=47

Order 2 Created (5 items):
quantity=50, reserved=8, available=42

Order 1 Delivered:
quantity=47, reserved=5, available=42

Order 2 Cancelled:
quantity=47, reserved=0, available=47
```

**Benefits**:
1. **No Overselling**: Reserved stock not available to others
2. **Easy Cancellation**: Just release reservation
3. **Audit Trail**: Can see reserved vs actual stock
4. **Race Condition Safe**: Database constraints prevent issues

**Result**: Reserve-deduct pattern ensures inventory consistency across distributed order flow."

---

### Q20: How does the Payment Service work? Why the 5% failure rate?
**Answer**:
"Payment Service simulates real-world payment processing with intentional failures for testing.

**Purpose**:
- Process payments asynchronously
- Simulate real payment gateway behavior
- Test failure scenarios and compensating transactions

**Implementation**:
```java
@Service
public class PaymentService {
    
    @KafkaListener(topics = "order-payment-events")
    public void processPayment(PaymentEvent event) {
        log.info("Processing payment for order: {}", event.getOrderId());
        
        // Simulate payment processing delay
        try {
            Thread.sleep(10000); // 10 seconds
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 5% random failure for testing
        boolean paymentSuccess = Math.random() > 0.05;
        
        PaymentResultEvent result = new PaymentResultEvent();
        result.setOrderId(event.getOrderId());
        result.setAmount(event.getAmount());
        
        if (paymentSuccess) {
            result.setStatus(PaymentStatus.COMPLETED);
            result.setTransactionId("TXN-" + UUID.randomUUID());
            log.info("Payment successful for order: {}", event.getOrderId());
        } else {
            result.setStatus(PaymentStatus.FAILED);
            result.setFailureReason("Insufficient funds / Card declined");
            log.error("Payment failed for order: {}", event.getOrderId());
        }
        
        // Publish result
        kafkaTemplate.send("payment-events", result);
    }
}
```

**Why 5% Failure Rate?**

**1. Real-World Simulation**:
- Real payment gateways have ~2-5% failure rate
- Card declined, insufficient funds, network issues
- Need to test how system handles failures

**2. Saga Pattern Testing**:
- Tests compensating transactions
- Verifies inventory release on payment failure
- Ensures order status updated correctly

**3. Resilience Testing**:
- Tests circuit breaker behavior
- Tests retry logic
- Tests fallback mechanisms

**4. Monitoring Testing**:
- Generates failure metrics
- Tests alerting systems
- Verifies error logging

**Payment Flow**:
```
T+0s: Order Service publishes to order-payment-events
T+0s: Payment Service receives event
T+0s-10s: Simulates payment processing (10 second delay)
T+10s: Random check (95% success, 5% fail)
T+10s: Publishes result to payment-events

Success Path:
- Order Service receives COMPLETED event
- Updates order to CONFIRMED
- Continues to warehouse

Failure Path:
- Order Service receives FAILED event
- Updates order to CANCELLED
- Releases inventory reservation
- Notifies customer
```

**Real Payment Gateway Integration**:
In production, would replace with:
```java
@Service
public class PaymentService {
    
    @Autowired
    private StripePaymentGateway stripeGateway;
    
    public void processPayment(PaymentEvent event) {
        try {
            PaymentIntent intent = stripeGateway.createPaymentIntent(
                event.getAmount(),
                event.getCurrency(),
                event.getPaymentMethod()
            );
            
            if (intent.getStatus().equals("succeeded")) {
                publishSuccess(event.getOrderId(), intent.getId());
            } else {
                publishFailure(event.getOrderId(), intent.getFailureMessage());
            }
        } catch (StripeException e) {
            publishFailure(event.getOrderId(), e.getMessage());
        }
    }
}
```

**Benefits of Current Implementation**:
- No external dependencies for testing
- Predictable failure rate
- Fast development and testing
- Easy to demonstrate saga pattern

**Result**: Payment Service simulates real-world behavior, enabling comprehensive testing of failure scenarios."

---

### Q21: What does the API Gateway do in your project?
**Answer**:
"API Gateway is the single entry point for all client requests.

**Responsibilities**:

**1. Authentication & Authorization**
```java
@Component
public class AuthenticationFilter implements GlobalFilter {
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, 
                             GatewayFilterChain chain) {
        String token = extractToken(exchange.getRequest());
        
        if (token == null) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
        
        // Validate token with Keycloak
        if (!keycloakService.validateToken(token)) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
        
        return chain.filter(exchange);
    }
}
```

**2. Request Routing**
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: product-service
          uri: lb://PRODUCT-SERVICE
          predicates:
            - Path=/api/products/**
          filters:
            - StripPrefix=0
            
        - id: order-service
          uri: lb://ORDER-SERVICE
          predicates:
            - Path=/api/orders/**
            
        - id: inventory-service
          uri: lb://INVENTORY-SERVICE
          predicates:
            - Path=/api/inventory/**
```

**How Routing Works**:
```
Client Request: GET /api/products/123
↓
Gateway matches: Path=/api/products/**
↓
Discovers service: lb://PRODUCT-SERVICE (via Eureka)
↓
Forwards to: http://product-service:8082/api/products/123
↓
Response flows back to client
```

**3. Load Balancing**
- If multiple instances of product-service running
- Gateway distributes requests across instances
- Uses Eureka for service discovery

**4. Cross-Cutting Concerns**
```java
// Logging Filter
public class LoggingFilter implements GlobalFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, 
                             GatewayFilterChain chain) {
        log.info("Request: {} {}", 
            exchange.getRequest().getMethod(),
            exchange.getRequest().getPath());
        
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            log.info("Response: {}", 
                exchange.getResponse().getStatusCode());
        }));
    }
}
```

**5. Rate Limiting** (if configured):
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: product-service
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 10
                redis-rate-limiter.burstCapacity: 20
```

**Complete Request Flow**:
```
1. Client → Gateway: POST /api/orders (with Bearer token)
2. Gateway: Extract token from Authorization header
3. Gateway → Keycloak: Validate token
4. Keycloak → Gateway: Token valid
5. Gateway → Eureka: Where is ORDER-SERVICE?
6. Eureka → Gateway: order-service:8083
7. Gateway → Order Service: Forward request
8. Order Service → Gateway: Response
9. Gateway → Client: Response
```

**Benefits**:

**1. Single Entry Point**
- Clients don't need to know about individual services
- Can change service locations without affecting clients

**2. Centralized Security**
- All authentication in one place
- Services don't need to validate tokens

**3. Simplified Client**
- Client calls one URL: http://gateway:8081
- Gateway handles routing to correct service

**4. Cross-Cutting Concerns**
- Logging, monitoring, rate limiting in one place
- Don't duplicate in every service

**5. Service Discovery Integration**
- Automatically discovers services via Eureka
- No hardcoded URLs

**Configuration**:
```properties
spring.application.name=api-gateway
server.port=8081

eureka.client.service-url.defaultZone=http://eureka-server:8761/eureka

spring.cloud.gateway.discovery.locator.enabled=true
spring.cloud.gateway.discovery.locator.lower-case-service-id=true
```

**Result**: API Gateway provides centralized routing, security, and cross-cutting concerns for all microservices."

---

## 5. KAFKA & EVENT-DRIVEN QUESTIONS

### Q22: Explain all 8 Kafka topics in your project.
**Answer**:
"I have 8 Kafka topics, each serving a specific purpose in the order flow:

**1. order-events**
- **Producer**: Order Service
- **Consumers**: Inventory Service, Notification Service
- **Purpose**: Notify when order is created
- **Message**: `{orderId, productId, quantity}`
- **Action**: Inventory reserves stock

**2. order-payment-events**
- **Producer**: Order Service
- **Consumer**: Payment Service
- **Purpose**: Trigger payment processing
- **Message**: `{orderId, amount, idempotencyKey}`
- **Action**: Payment Service processes payment

**3. payment-events**
- **Producer**: Payment Service
- **Consumer**: Order Service
- **Purpose**: Send payment result back
- **Message**: `{orderId, status, transactionId, failureReason}`
- **Action**: Order Service updates status (CONFIRMED or CANCELLED)

**4. order-status-events**
- **Producer**: Order Service
- **Consumers**: Warehouse, Shipping, Delivery, Notification Services
- **Purpose**: Broadcast order status changes
- **Message**: `{orderId, newStatus, oldStatus, timestamp}`
- **Action**: Next service in chain starts processing

**5. warehouse-events**
- **Producer**: Warehouse Service
- **Consumer**: Order Service
- **Purpose**: Send warehouse processing result
- **Message**: `{orderId, status, pickingCompletedAt}`
- **Action**: Order Service updates to PROCESSING

**6. shipping-events**
- **Producer**: Shipping Service
- **Consumer**: Order Service
- **Purpose**: Send shipping dispatch result
- **Message**: `{orderId, status, courierName, trackingNumber}`
- **Action**: Order Service updates to SHIPPED

**7. delivery-events**
- **Producer**: Delivery Service
- **Consumer**: Order Service
- **Purpose**: Send delivery completion result
- **Message**: `{orderId, status, deliveredAt, signature}`
- **Action**: Order Service updates to DELIVERED

**8. inventory-reservation-events**
- **Producer**: Order Service
- **Consumer**: Inventory Service
- **Purpose**: Command inventory to deduct or release stock
- **Message**: `{orderId, productId, quantity, action: DEDUCT/RELEASE}`
- **Action**: Inventory permanently deducts or releases reservation

**Event Flow Visualization**:
```
Order Created:
Order Service → order-events → Inventory (reserve)
Order Service → order-payment-events → Payment

Payment Success:
Payment → payment-events → Order Service
Order Service → order-status-events → Warehouse

Warehouse Complete:
Warehouse → warehouse-events → Order Service
Order Service → order-status-events → Shipping

Shipping Complete:
Shipping → shipping-events → Order Service
Order Service → order-status-events → Delivery

Delivery Complete:
Delivery → delivery-events → Order Service
Order Service → inventory-reservation-events → Inventory (deduct)
```

**Topic Configuration**:
```properties
# Auto-create topics
spring.kafka.admin.auto-create=true

# Retention: 7 days
log.retention.hours=168

# Replication factor: 1 (dev), 3 (prod)
default.replication.factor=1

# Partitions: 3 per topic (for scalability)
num.partitions=3
```

**Why Multiple Topics?**
- **Separation of Concerns**: Each topic has specific purpose
- **Scalability**: Can scale consumers independently
- **Flexibility**: Easy to add new consumers
- **Debugging**: Can see exactly what events flowing where

**Result**: 8 topics enable complete event-driven order orchestration with clear separation of concerns."

---

### Q23: How do you ensure message ordering in Kafka?
**Answer**:
"Message ordering is critical for order processing. Here's how I ensure it:

**Problem**:
```
Order 1: Created → Confirmed → Delivered
If messages arrive out of order:
Delivered → Created → Confirmed (WRONG!)
```

**Solution - Kafka Partitioning**:

**1. Partition by Order ID**
```java
@Service
public class OrderEventPublisher {
    
    public void publishOrderEvent(OrderEvent event) {
        // Use orderId as partition key
        kafkaTemplate.send(
            "order-events",
            event.getOrderId().toString(), // Partition key
            event
        );
    }
}
```

**How It Works**:
- Kafka uses hash of key to determine partition
- Same orderId always goes to same partition
- Messages in same partition are ordered
- Order 123 always goes to partition 2
- All events for Order 123 processed in order

**2. Consumer Configuration**:
```properties
# Single consumer per partition ensures ordering
spring.kafka.consumer.group-id=order-service-group
spring.kafka.consumer.max-poll-records=1
spring.kafka.consumer.enable-auto-commit=false
```

**3. Manual Commit After Processing**:
```java
@KafkaListener(topics = "payment-events")
public void handlePaymentEvent(
    ConsumerRecord<String, PaymentEvent> record,
    Acknowledgment ack
) {
    PaymentEvent event = record.value();
    
    // Process event
    processPayment(event);
    
    // Commit only after successful processing
    ack.acknowledge();
}
```

**Partition Strategy**:
```
Topic: order-events (3 partitions)

Order 123 → hash(123) % 3 = 0 → Partition 0
Order 456 → hash(456) % 3 = 1 → Partition 1
Order 789 → hash(789) % 3 = 2 → Partition 2
Order 124 → hash(124) % 3 = 1 → Partition 1

All events for Order 123 go to Partition 0 in order
All events for Order 456 go to Partition 1 in order
```

**Consumer Group**:
```
Consumer Group: order-service-group
- Consumer 1 reads Partition 0
- Consumer 2 reads Partition 1
- Consumer 3 reads Partition 2

Each consumer processes messages in order for its partition
```

**What If No Partition Key?**
```java
// Without key - round robin distribution
kafkaTemplate.send("order-events", event);
// Order 123 events could go to different partitions
// No ordering guarantee!
```

**Trade-offs**:

**Pros**:
- Guaranteed ordering per order
- Can scale consumers (one per partition)
- No complex coordination needed

**Cons**:
- Limited parallelism (max consumers = partitions)
- Hot partitions if some orders very active
- Can't reorder across partitions

**Real Example**:
```
Order 123 Events:
1. Created (t=0s) → Partition 0
2. Payment Success (t=10s) → Partition 0
3. Warehouse Complete (t=28s) → Partition 0
4. Shipped (t=48s) → Partition 0
5. Delivered (t=100s) → Partition 0

Consumer reads from Partition 0 in exact order
Processes: Created → Payment → Warehouse → Shipped → Delivered
```

**Result**: Partition key ensures all events for same order processed in correct order."

---

### Q24: What happens if a Kafka consumer fails while processing a message?
**Answer**:
"I handle consumer failures with proper error handling and retry mechanisms.

**Scenario**: Payment Service crashes while processing payment event

**Without Proper Handling**:
```
1. Consumer reads payment event
2. Starts processing
3. Crashes mid-processing
4. Message lost forever
5. Order stuck in PENDING state
```

**My Implementation**:

**1. Manual Commit (At-Least-Once Delivery)**:
```java
@KafkaListener(
    topics = "order-payment-events",
    containerFactory = "kafkaListenerContainerFactory"
)
public void processPayment(
    ConsumerRecord<String, PaymentEvent> record,
    Acknowledgment ack
) {
    try {
        PaymentEvent event = record.value();
        
        // Process payment
        PaymentResult result = paymentGateway.process(event);
        
        // Save to database
        paymentRepository.save(result);
        
        // Publish result
        kafkaTemplate.send("payment-events", result);
        
        // Commit only after everything succeeds
        ack.acknowledge();
        
    } catch (Exception e) {
        log.error("Payment processing failed", e);
        // Don't acknowledge - message will be redelivered
        throw e;
    }
}
```

**Configuration**:
```properties
# Manual commit mode
spring.kafka.consumer.enable-auto-commit=false
spring.kafka.listener.ack-mode=manual

# Retry configuration
spring.kafka.consumer.max-poll-records=1
spring.kafka.consumer.session-timeout-ms=30000
```

**What Happens on Failure**:
```
1. Consumer reads message (offset 100)
2. Starts processing
3. Crashes before ack.acknowledge()
4. Kafka doesn't receive commit
5. Consumer restarts
6. Kafka redelivers message from offset 100
7. Consumer processes again
8. This time succeeds
9. Calls ack.acknowledge()
10. Kafka commits offset 101
```

**2. Idempotency for Redelivery**:
```java
@Transactional
public void processPayment(PaymentEvent event) {
    // Check if already processed
    Optional<Payment> existing = paymentRepository
        .findByIdempotencyKey(event.getIdempotencyKey());
    
    if (existing.isPresent()) {
        log.info("Payment already processed, skipping");
        return; // Idempotent - safe to process multiple times
    }
    
    // Process payment
    Payment payment = new Payment();
    payment.setIdempotencyKey(event.getIdempotencyKey());
    payment.setOrderId(event.getOrderId());
    payment.setAmount(event.getAmount());
    payment.setStatus(processPaymentGateway(event));
    
    paymentRepository.save(payment);
}
```

**3. Dead Letter Queue (DLQ)**:
```java
@Bean
public ConcurrentKafkaListenerContainerFactory<String, PaymentEvent> 
    kafkaListenerContainerFactory() {
    
    factory.setCommonErrorHandler(
        new DefaultErrorHandler(
            new DeadLetterPublishingRecoverer(kafkaTemplate),
            new FixedBackOff(1000L, 3L) // 3 retries, 1s apart
        )
    );
    
    return factory;
}
```

**Flow with DLQ**:
```
1. Message processing fails
2. Retry 1 after 1 second - fails
3. Retry 2 after 1 second - fails
4. Retry 3 after 1 second - fails
5. Send to DLQ topic: order-payment-events.DLT
6. Alert operations team
7. Manual investigation and reprocessing
```

**4. Circuit Breaker Integration**:
```java
@CircuitBreaker(name = "paymentService", 
                fallbackMethod = "paymentFallback")
public void processPayment(PaymentEvent event) {
    // If payment gateway down, circuit opens
    // Stops trying, calls fallback
}

public void paymentFallback(PaymentEvent event, Exception e) {
    log.error("Payment service unavailable, sending to DLQ");
    kafkaTemplate.send("payment-events.DLT", event);
}
```

**Complete Error Handling Strategy**:
```
Level 1: Try processing
Level 2: If fails, retry 3 times (1s apart)
Level 3: If still fails, check circuit breaker
Level 4: If circuit open, send to DLQ
Level 5: Alert operations team
Level 6: Manual intervention
```

**Monitoring**:
```java
@Scheduled(fixedRate = 60000) // Every minute
public void checkDLQ() {
    long dlqCount = kafkaAdmin.getMessageCount("payment-events.DLT");
    if (dlqCount > 0) {
        alertService.sendAlert(
            "DLQ has " + dlqCount + " messages"
        );
    }
}
```

**Result**: Robust error handling ensures no message loss, with automatic retries and manual fallback for persistent failures."

---

This is Part 2. Continue to next part for more questions...

## 6. DATABASE & DATA MANAGEMENT QUESTIONS

### Q25: Why did you use two different databases (MySQL and MongoDB)?
**Answer**:
"I used polyglot persistence - different databases for different data characteristics.

**MySQL for Orders & Inventory**:

**Why?**
1. **ACID Transactions**: Money involved, need strong consistency
2. **Relational Data**: Orders have line items (1:N relationship)
3. **Complex Queries**: Need joins, aggregations, reporting
4. **Data Integrity**: Foreign keys, constraints

**Example**:
```sql
-- Atomic inventory update
BEGIN TRANSACTION;
UPDATE inventory 
SET reserved_quantity = reserved_quantity + 3
WHERE product_id = 'prod-123' 
AND (quantity - reserved_quantity) >= 3;

INSERT INTO order_line_items (order_id, product_id, quantity)
VALUES (1, 'prod-123', 3);
COMMIT;
-- Either both succeed or both fail
```

**MongoDB for Products**:

**Why?**
1. **Flexible Schema**: Products have varying attributes
2. **No Joins Needed**: Products are standalone documents
3. **Read-Heavy**: Product browsing 10x more than ordering
4. **Horizontal Scaling**: Easy to shard

**Example**:
```json
// Electronics
{
  "name": "iPhone",
  "specs": {"storage": "256GB", "ram": "8GB"}
}

// Clothing
{
  "name": "T-Shirt",
  "specs": {"size": "L", "color": "Blue", "material": "Cotton"}
}

// Books
{
  "name": "Clean Code",
  "specs": {"author": "Robert Martin", "pages": 464, "isbn": "123"}
}
```

**Could I use one database?**

**Option 1: Only MySQL**
- Products: Rigid schema, hard to add new product types
- Need EAV (Entity-Attribute-Value) pattern - complex
- Poor performance for product catalog

**Option 2: Only MongoDB**
- Orders: No ACID, risk of data inconsistency
- Inventory: Race conditions in stock updates
- Financial data needs ACID guarantees

**Trade-offs**:
- **Complexity**: Managing two databases
- **Consistency**: Eventual consistency across databases
- **Operations**: Two databases to monitor, backup

**Benefits**:
- **Right tool for right job**: Optimal performance
- **Scalability**: Scale databases independently
- **Flexibility**: Add new product types easily

**Result**: Polyglot persistence provides optimal solution for different data needs."

---

### Q26: How do you handle database transactions across microservices?
**Answer**:
"I use the Saga pattern since traditional ACID transactions don't work across microservices.

**Problem - Can't Use Distributed Transactions**:
```
Order Service (MySQL) + Inventory Service (MySQL) + Payment Service
Can't wrap all three in one database transaction
They're separate databases, separate services
```

**Solution - Saga Pattern with Compensating Transactions**:

**Normal Flow**:
```
1. Order Service: Create order (local transaction)
   - Save order with status PENDING
   - Commit transaction
   
2. Inventory Service: Reserve stock (local transaction)
   - Update reserved_quantity
   - Commit transaction
   
3. Payment Service: Process payment (local transaction)
   - Create payment record
   - Commit transaction
   
4. Order Service: Update to CONFIRMED (local transaction)
   - Update order status
   - Commit transaction
```

**Failure Flow with Compensation**:
```
1. Order Service: Create order ✓
   - Order saved with id=123
   
2. Inventory Service: Reserve stock ✓
   - Reserved 3 items
   
3. Payment Service: Process payment ✗
   - Payment failed
   
4. Compensating Transactions:
   a. Order Service: Cancel order
      - UPDATE orders SET status='CANCELLED' WHERE id=123
   
   b. Inventory Service: Release reservation
      - UPDATE inventory SET reserved_quantity = reserved_quantity - 3
```

**Implementation**:
```java
// Order Service - Saga Coordinator
@Transactional
public Order createOrder(Order order) {
    // Step 1: Local transaction
    order.setStatus(PENDING);
    Order saved = repository.save(order);
    
    // Step 2: Publish events (outside transaction)
    publishInventoryReservation(saved);
    publishPaymentRequest(saved);
    
    return saved;
}

// Payment Event Listener
@KafkaListener(topics = "payment-events")
@Transactional
public void handlePaymentResult(PaymentEvent event) {
    Order order = repository.findById(event.getOrderId()).orElseThrow();
    
    if (event.getStatus() == COMPLETED) {
        // Continue saga
        order.setStatus(CONFIRMED);
        repository.save(order);
        publishWarehouseRequest(order);
    } else {
        // Compensate - rollback saga
        order.setStatus(CANCELLED);
        repository.save(order);
        publishInventoryRelease(order); // Compensating action
    }
}
```

**Compensating Transaction Example**:
```java
// Inventory Service
@Transactional
public void releaseReservation(String productId, int quantity) {
    Inventory inv = repository.findByProductId(productId).orElseThrow();
    
    // Compensating action - undo reservation
    inv.setReservedQuantity(inv.getReservedQuantity() - quantity);
    repository.save(inv);
    
    log.info("Released {} units of {} (compensation)", quantity, productId);
}
```

**Saga State Machine**:
```
States: PENDING → CONFIRMED → PROCESSING → SHIPPED → DELIVERED
        ↓
     CANCELLED (compensation triggered)

Transitions:
- PENDING → CONFIRMED: Payment success
- PENDING → CANCELLED: Payment failure (compensate inventory)
- CONFIRMED → CANCELLED: Warehouse failure (compensate payment + inventory)
```

**Idempotency for Saga Steps**:
```java
@Transactional
public void reserveStock(ReservationEvent event) {
    // Check if already processed
    if (reservationRepository.existsByOrderId(event.getOrderId())) {
        log.info("Reservation already processed");
        return; // Idempotent
    }
    
    // Process reservation
    Inventory inv = repository.findByProductId(event.getProductId()).orElseThrow();
    inv.setReservedQuantity(inv.getReservedQuantity() + event.getQuantity());
    repository.save(inv);
    
    // Record that we processed this
    reservationRepository.save(new Reservation(event.getOrderId(), event.getProductId()));
}
```

**Benefits**:
- Each service maintains its own data integrity
- Automatic rollback on failures
- No distributed locks
- Services remain independent

**Challenges**:
- Eventual consistency (not immediate)
- Complex error handling
- Need to handle partial failures

**Result**: Saga pattern provides distributed transaction management without coupling services."

---

### Q27: How do you ensure data consistency in a distributed system?
**Answer**:
"I use multiple strategies for consistency:

**1. Eventual Consistency (Primary Approach)**

**Accept**: Data may be temporarily inconsistent but will eventually become consistent

**Example**:
```
T+0s: Order created (PENDING)
T+0s: Inventory reserved (50 → 47 available)
T+10s: Payment processed (CONFIRMED)
T+100s: Order delivered (DELIVERED)
T+100s: Inventory deducted (47 → 47 actual)

Between 0s and 100s: Order and Inventory in different states
After 100s: Consistent
```

**2. Idempotency**

**Ensure**: Same operation multiple times = same result

**Implementation**:
```java
// Order Service
public Order createOrder(Order order) {
    // Idempotency key check
    if (order.getIdempotencyKey() != null) {
        Order existing = repository.findByIdempotencyKey(
            order.getIdempotencyKey()
        );
        if (existing != null) {
            return existing; // Return existing, don't create duplicate
        }
    }
    
    order.setIdempotencyKey(UUID.randomUUID().toString());
    return repository.save(order);
}
```

**Database Constraint**:
```sql
CREATE TABLE orders (
    id BIGINT PRIMARY KEY,
    idempotency_key VARCHAR(255) UNIQUE NOT NULL
);
```

**3. Optimistic Locking**

**Prevent**: Lost updates in concurrent scenarios

**Implementation**:
```java
@Entity
public class Inventory {
    @Id
    private Long id;
    
    private String productId;
    private Integer quantity;
    private Integer reservedQuantity;
    
    @Version
    private Long version; // Optimistic lock
}

// Service
@Transactional
public void reserveStock(String productId, int qty) {
    Inventory inv = repository.findByProductId(productId).orElseThrow();
    
    int available = inv.getQuantity() - inv.getReservedQuantity();
    if (available < qty) {
        throw new InsufficientStockException();
    }
    
    inv.setReservedQuantity(inv.getReservedQuantity() + qty);
    repository.save(inv); // Version checked automatically
    // If version changed, throws OptimisticLockException
}
```

**How It Works**:
```
Thread 1: Read inventory (version=1, quantity=50, reserved=0)
Thread 2: Read inventory (version=1, quantity=50, reserved=0)

Thread 1: Reserve 3 items
          UPDATE inventory SET reserved=3, version=2 WHERE id=1 AND version=1
          Success! (version=2)

Thread 2: Reserve 5 items
          UPDATE inventory SET reserved=5, version=2 WHERE id=1 AND version=1
          Fails! (version is now 2, not 1)
          Throws OptimisticLockException
          Retry with fresh data
```

**4. Event Sourcing**

**Store**: All state changes as events

**Implementation**:
```java
// All order state changes published as events
public void updateOrderStatus(Long orderId, OrderStatus newStatus) {
    Order order = repository.findById(orderId).orElseThrow();
    OrderStatus oldStatus = order.getStatus();
    
    order.setStatus(newStatus);
    repository.save(order);
    
    // Publish event
    OrderStatusChangedEvent event = new OrderStatusChangedEvent(
        orderId, oldStatus, newStatus, LocalDateTime.now()
    );
    kafkaTemplate.send("order-status-events", event);
}
```

**Benefits**:
- Complete audit trail
- Can rebuild state from events
- Can debug issues by replaying events

**5. Compensating Transactions**

**Handle**: Failures by undoing previous operations

**Example**:
```java
// Payment fails - compensate
@KafkaListener(topics = "payment-events")
public void handlePaymentFailure(PaymentEvent event) {
    if (event.getStatus() == FAILED) {
        // Compensate: Release inventory
        InventoryReleaseEvent releaseEvent = new InventoryReleaseEvent(
            event.getOrderId(),
            event.getProductId(),
            event.getQuantity()
        );
        kafkaTemplate.send("inventory-reservation-events", releaseEvent);
        
        // Compensate: Cancel order
        Order order = repository.findById(event.getOrderId()).orElseThrow();
        order.setStatus(CANCELLED);
        repository.save(order);
    }
}
```

**6. Database Constraints**

**Enforce**: Data integrity at database level

**Examples**:
```sql
-- Prevent negative inventory
ALTER TABLE inventory ADD CONSTRAINT check_quantity 
CHECK (quantity >= 0);

ALTER TABLE inventory ADD CONSTRAINT check_reserved 
CHECK (reserved_quantity >= 0 AND reserved_quantity <= quantity);

-- Prevent duplicate orders
ALTER TABLE orders ADD CONSTRAINT unique_idempotency 
UNIQUE (idempotency_key);

-- Referential integrity
ALTER TABLE order_line_items ADD CONSTRAINT fk_order 
FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE;
```

**7. Monitoring & Alerts**

**Detect**: Inconsistencies and alert

**Implementation**:
```java
@Scheduled(fixedRate = 300000) // Every 5 minutes
public void checkConsistency() {
    // Check for orders stuck in PENDING > 2 hours
    List<Order> stuckOrders = repository.findByStatusAndCreatedAtBefore(
        PENDING, LocalDateTime.now().minusHours(2)
    );
    
    if (!stuckOrders.isEmpty()) {
        alertService.sendAlert("Found " + stuckOrders.size() + " stuck orders");
    }
    
    // Check for inventory inconsistencies
    List<Inventory> inconsistent = inventoryRepository.findAll().stream()
        .filter(inv -> inv.getReservedQuantity() > inv.getQuantity())
        .collect(Collectors.toList());
    
    if (!inconsistent.isEmpty()) {
        alertService.sendAlert("Inventory inconsistency detected");
    }
}
```

**Trade-offs**:

**Strong Consistency** (Traditional ACID):
- Pros: Always consistent
- Cons: Tight coupling, poor scalability, single point of failure

**Eventual Consistency** (My Approach):
- Pros: Loose coupling, high scalability, fault tolerance
- Cons: Temporary inconsistency, complex error handling

**Result**: Multiple strategies ensure data consistency while maintaining microservices independence."

---

## 7. SECURITY & AUTHENTICATION QUESTIONS

### Q28: Explain the complete authentication flow in your project.
**Answer**:
"My authentication uses OAuth2 with Keycloak. Here's the complete flow:

**Step 1: User Login**
```
Client → Keycloak: POST /realms/ecommerce/protocol/openid-connect/token

Request Body (x-www-form-urlencoded):
grant_type=password
client_id=backend-service
client_secret=ecommerce-secret-2024
username=testuser
password=test123

Keycloak validates credentials
Keycloak generates tokens
```

**Step 2: Token Response**
```
Keycloak → Client:

{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expires_in": 300,
  "refresh_expires_in": 1800,
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "Bearer"
}

Client stores access_token
```

**Step 3: API Request with Token**
```
Client → API Gateway: GET /api/products

Headers:
Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Step 4: Token Validation**
```
API Gateway extracts token from Authorization header
API Gateway → Keycloak: Validate token

Keycloak checks:
- Token signature valid?
- Token not expired?
- Token not revoked?
- User still active?

Keycloak → API Gateway: Valid/Invalid
```

**Step 5: Request Forwarding**
```
If token valid:
  API Gateway → Eureka: Where is PRODUCT-SERVICE?
  Eureka → API Gateway: product-service:8082
  API Gateway → Product Service: GET /api/products
  Product Service → MongoDB: Query products
  Product Service → API Gateway: Response
  API Gateway → Client: Response

If token invalid:
  API Gateway → Client: 401 Unauthorized
```

**Step 6: Token Refresh (When Expired)**
```
After 5 minutes, access_token expires

Client → Keycloak: POST /realms/ecommerce/protocol/openid-connect/token

Request Body:
grant_type=refresh_token
client_id=backend-service
client_secret=ecommerce-secret-2024
refresh_token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

Keycloak → Client: New access_token + refresh_token
```

**Implementation - API Gateway Filter**:
```java
@Component
public class AuthenticationFilter implements GlobalFilter {
    
    @Autowired
    private KeycloakService keycloakService;
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, 
                             GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        
        // Extract token
        String token = extractToken(request);
        
        if (token == null) {
            return unauthorized(exchange);
        }
        
        // Validate with Keycloak
        try {
            boolean valid = keycloakService.validateToken(token);
            if (!valid) {
                return unauthorized(exchange);
            }
        } catch (Exception e) {
            log.error("Token validation failed", e);
            return unauthorized(exchange);
        }
        
        // Token valid, forward request
        return chain.filter(exchange);
    }
    
    private String extractToken(ServerHttpRequest request) {
        List<String> headers = request.getHeaders().get("Authorization");
        if (headers == null || headers.isEmpty()) {
            return null;
        }
        
        String authHeader = headers.get(0);
        if (authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        
        return null;
    }
    
    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }
}
```

**Keycloak Configuration**:
```json
{
  "realm": "ecommerce",
  "clients": [{
    "clientId": "backend-service",
    "secret": "ecommerce-secret-2024",
    "directAccessGrantsEnabled": true,
    "serviceAccountsEnabled": true
  }],
  "users": [{
    "username": "testuser",
    "email": "test@ecommerce.com",
    "credentials": [{
      "type": "password",
      "value": "test123"
    }],
    "realmRoles": ["user"]
  }]
}
```

**Security Benefits**:
1. **Centralized Authentication**: Single source of truth
2. **Stateless**: No session storage needed
3. **Scalable**: Gateway can scale horizontally
4. **Secure**: Industry-standard OAuth2
5. **Flexible**: Easy to add social login, MFA

**Result**: Secure, scalable authentication with centralized token management."

---

### Q29: How do you secure inter-service communication?
**Answer**:
"I use multiple layers of security for inter-service communication:

**Current Implementation - Trust API Gateway**:

**Architecture**:
```
Internet → API Gateway (validates token) → Microservices (trust gateway)
```

**Why This Works**:
1. All external requests go through gateway
2. Gateway validates tokens
3. Services trust requests from gateway
4. Services not exposed to internet

**Network Security**:
```yaml
# docker-compose.yml
networks:
  ecommerce-network:
    driver: bridge
    internal: true  # Not accessible from outside
```

**Service Configuration**:
```properties
# Services only listen on internal network
server.address=0.0.0.0
server.port=8083

# Eureka on internal network
eureka.client.service-url.defaultZone=http://eureka-server:8761/eureka
```

**Production Enhancements I Would Add**:

**1. Mutual TLS (mTLS)**:
```
Each service has certificate
Services verify each other's certificates
Encrypted communication
```

**Implementation**:
```properties
# Service A
server.ssl.enabled=true
server.ssl.key-store=classpath:service-a.p12
server.ssl.key-store-password=secret
server.ssl.trust-store=classpath:truststore.p12

# Service B
server.ssl.enabled=true
server.ssl.key-store=classpath:service-b.p12
```

**2. Service Mesh (Istio)**:
```
Istio handles:
- Automatic mTLS between services
- Traffic encryption
- Access control policies
- Service-to-service authentication
```

**3. API Keys for Internal Services**:
```java
@Component
public class InternalServiceFilter implements Filter {
    
    @Value("${internal.api.key}")
    private String apiKey;
    
    @Override
    public void doFilter(ServletRequest request, 
                        ServletResponse response, 
                        FilterChain chain) {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        
        String requestApiKey = httpRequest.getHeader("X-Internal-API-Key");
        
        if (!apiKey.equals(requestApiKey)) {
            ((HttpServletResponse) response).setStatus(403);
            return;
        }
        
        chain.doFilter(request, response);
    }
}
```

**4. Network Policies (Kubernetes)**:
```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: order-service-policy
spec:
  podSelector:
    matchLabels:
      app: order-service
  ingress:
  - from:
    - podSelector:
        matchLabels:
          app: api-gateway
    ports:
    - protocol: TCP
      port: 8083
```

**Current Security Measures**:

**1. Docker Network Isolation**:
```
Services communicate on private network
Not accessible from host machine
Only API Gateway exposed
```

**2. Eureka Authentication**:
```properties
eureka.client.service-url.defaultZone=http://eureka:eureka123@eureka-server:8761/eureka
```

**3. No Direct Service Exposure**:
```yaml
# Only gateway exposed to host
api-gateway:
  ports:
    - "8081:8081"

# Services not exposed
order-service:
  # No ports mapping to host
```

**4. Environment-based Secrets**:
```properties
# application-docker.properties
spring.datasource.password=${DB_PASSWORD:mysql}
keycloak.client-secret=${KEYCLOAK_SECRET:ecommerce-secret-2024}
```

**Kafka Security** (Production):
```properties
# Enable SASL authentication
spring.kafka.properties.security.protocol=SASL_SSL
spring.kafka.properties.sasl.mechanism=PLAIN
spring.kafka.properties.sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username="kafka" password="secret";

# Enable SSL encryption
spring.kafka.properties.ssl.truststore.location=/path/to/truststore.jks
spring.kafka.properties.ssl.truststore.password=secret
```

**Result**: Multi-layered security with gateway validation, network isolation, and production-ready enhancements available."

---

### Q30: What would you do if someone steals a valid token?
**Answer**:
"Token theft is a serious security concern. Here's my defense strategy:

**Prevention Measures**:

**1. Short Token Expiry**:
```
Access Token: 5 minutes
Refresh Token: 30 minutes

Stolen token only valid for 5 minutes
Attacker must steal refresh token too
```

**Configuration**:
```json
// Keycloak realm settings
{
  "accessTokenLifespan": 300,
  "refreshTokenLifespan": 1800
}
```

**2. HTTPS Only**:
```
All communication over HTTPS
Tokens encrypted in transit
Man-in-the-middle attacks prevented
```

**3. Secure Storage**:
```javascript
// Client-side (React/Angular)
// DON'T store in localStorage (vulnerable to XSS)
localStorage.setItem('token', token); // ❌ BAD

// DO store in httpOnly cookie
document.cookie = `token=${token}; HttpOnly; Secure; SameSite=Strict`; // ✓ GOOD

// Or use memory storage
let token = null; // ✓ GOOD (lost on page refresh)
```

**4. Token Binding**:
```java
// Bind token to IP address
@Component
public class TokenBindingFilter implements GlobalFilter {
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, 
                             GatewayFilterChain chain) {
        String token = extractToken(exchange.getRequest());
        String clientIp = getClientIp(exchange.getRequest());
        
        // Decode token and check IP
        String tokenIp = extractIpFromToken(token);
        
        if (!clientIp.equals(tokenIp)) {
            log.warn("Token used from different IP. Token IP: {}, Request IP: {}", 
                     tokenIp, clientIp);
            return unauthorized(exchange);
        }
        
        return chain.filter(exchange);
    }
}
```

**Detection Measures**:

**1. Anomaly Detection**:
```java
@Service
public class TokenAnomalyDetector {
    
    @Autowired
    private RedisTemplate<String, String> redis;
    
    public boolean detectAnomaly(String token, String userId, String ip) {
        String key = "user:" + userId + ":activity";
        
        // Get last known IP
        String lastIp = redis.opsForValue().get(key + ":ip");
        
        // Check if IP changed suddenly
        if (lastIp != null && !lastIp.equals(ip)) {
            // Get last activity time
            String lastActivity = redis.opsForValue().get(key + ":time");
            long timeDiff = System.currentTimeMillis() - Long.parseLong(lastActivity);
            
            // If IP changed within 5 minutes, suspicious
            if (timeDiff < 300000) {
                log.warn("Suspicious activity: User {} accessed from {} and {} within 5 minutes", 
                         userId, lastIp, ip);
                return true;
            }
        }
        
        // Update last activity
        redis.opsForValue().set(key + ":ip", ip);
        redis.opsForValue().set(key + ":time", String.valueOf(System.currentTimeMillis()));
        
        return false;
    }
}
```

**2. Rate Limiting**:
```java
@Bean
public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
    return builder.routes()
        .route("order-service", r -> r.path("/api/orders/**")
            .filters(f -> f.requestRateLimiter(c -> c
                .setRateLimiter(redisRateLimiter())
                .setKeyResolver(userKeyResolver())))
            .uri("lb://ORDER-SERVICE"))
        .build();
}

// Max 10 requests per minute per user
@Bean
public RedisRateLimiter redisRateLimiter() {
    return new RedisRateLimiter(10, 20);
}
```

**3. Audit Logging**:
```java
@Aspect
@Component
public class SecurityAuditAspect {
    
    @Around("@annotation(org.springframework.web.bind.annotation.PostMapping)")
    public Object auditSecurityEvent(ProceedingJoinPoint joinPoint) throws Throwable {
        String userId = SecurityContextHolder.getContext()
            .getAuthentication().getName();
        String ip = getClientIp();
        String action = joinPoint.getSignature().getName();
        
        log.info("Security Audit: User={}, IP={}, Action={}", userId, ip, action);
        
        // Store in database for analysis
        auditRepository.save(new AuditLog(userId, ip, action, LocalDateTime.now()));
        
        return joinPoint.proceed();
    }
}
```

**Response Measures**:

**1. Token Revocation**:
```java
@Service
public class TokenRevocationService {
    
    @Autowired
    private RedisTemplate<String, String> redis;
    
    public void revokeToken(String token) {
        String tokenId = extractTokenId(token);
        
        // Add to revocation list
        redis.opsForValue().set("revoked:" + tokenId, "true", 
                                Duration.ofMinutes(30));
        
        log.warn("Token revoked: {}", tokenId);
    }
    
    public boolean isRevoked(String token) {
        String tokenId = extractTokenId(token);
        return redis.hasKey("revoked:" + tokenId);
    }
}

// Check in filter
if (tokenRevocationService.isRevoked(token)) {
    return unauthorized(exchange);
}
```

**2. Force Re-authentication**:
```java
@RestController
public class SecurityController {
    
    @PostMapping("/api/security/force-logout")
    public ResponseEntity<?> forceLogout(@RequestParam String userId) {
        // Revoke all tokens for user
        tokenRevocationService.revokeAllUserTokens(userId);
        
        // Notify user
        notificationService.send(userId, "Your session has been terminated for security reasons");
        
        return ResponseEntity.ok("User logged out");
    }
}
```

**3. Alert Admin**:
```java
@Service
public class SecurityAlertService {
    
    public void alertSuspiciousActivity(String userId, String ip, String reason) {
        Alert alert = new Alert();
        alert.setSeverity("HIGH");
        alert.setMessage("Suspicious activity detected for user " + userId);
        alert.setDetails("IP: " + ip + ", Reason: " + reason);
        alert.setTimestamp(LocalDateTime.now());
        
        // Send to monitoring system
        monitoringService.sendAlert(alert);
        
        // Send email to security team
        emailService.sendToSecurityTeam(alert);
        
        // Log for analysis
        log.error("SECURITY ALERT: {}", alert);
    }
}
```

**Complete Flow - Token Theft Detected**:
```
1. User logs in from IP 192.168.1.1
2. Token issued
3. Attacker steals token
4. Attacker uses token from IP 10.0.0.1
5. System detects IP change within 5 minutes
6. System flags as suspicious
7. System revokes token
8. System forces re-authentication
9. System alerts security team
10. System notifies user
11. User changes password
12. New token issued
```

**Result**: Multi-layered defense with prevention, detection, and response measures for token theft."

---

This is Part 3. Continue to next part for remaining questions...

## 8. DOCKER & DEPLOYMENT QUESTIONS

### Q31: Explain your Docker setup. How many containers and why?
**Answer**:
"I have 18 containers total - 10 microservices + 8 infrastructure services.

**Infrastructure Containers (8)**:
1. Keycloak (8080) - OAuth2 authentication
2. Kafka (9092) - Message broker
3. Zookeeper (2181) - Kafka coordination
4. MySQL (3307) - Orders/Inventory database
5. MongoDB (27017) - Products database
6. Zipkin (9411) - Distributed tracing
7. Prometheus (9090) - Metrics collection
8. Grafana (3000) - Dashboards

**Microservice Containers (10)**:
9. Eureka Server (8761)
10. API Gateway (8081)
11. Product Service (8082)
12. Order Service (8083)
13. Inventory Service (8084)
14. Notification Service (8085)
15. Payment Service (8086)
16. Warehouse Service (8087)
17. Shipping Service (8088)
18. Delivery Service (8089)

**Docker Network**: ecommerce-network (bridge)
**Volumes**: mongo-data, mysql-data (persistent storage)
**Start**: docker-compose up -d

**Result**: Complete containerized platform, one command starts everything."

---

### Q32: How do containers communicate?
**Answer**:
"Containers use Docker DNS on shared network.

**Configuration**:
```properties
# Order Service connects to MySQL
spring.datasource.url=jdbc:mysql://mysql:3306/orderdb

# Connects to Kafka
spring.kafka.bootstrap-servers=kafka:9092

# Registers with Eureka
eureka.client.service-url.defaultZone=http://eureka-server:8761/eureka
```

**DNS Resolution**: Container name = Hostname
**Network**: All on ecommerce-network
**Security**: Internal only, API Gateway exposed

**Result**: Seamless communication using container names."

---

## 9. MONITORING QUESTIONS

### Q33: How do you monitor your system?
**Answer**:
"Three pillars: Metrics, Traces, Logs.

**1. Metrics (Prometheus + Grafana)**:
- Request rate, error rate, response time
- JVM memory, CPU usage
- Business metrics (orders/min, revenue)

**2. Traces (Zipkin)**:
- Complete request flow across services
- Identify bottlenecks
- Track errors

**3. Logs (Structured)**:
- All services log to console
- Centralized with ELK in production

**4. Health Checks (Actuator)**:
- /actuator/health
- Database, Kafka, Circuit breaker status

**5. Alerts (Prometheus)**:
- High error rate > 5%
- Slow response > 1s
- Service down

**Result**: Complete observability for production."

---

### Q34: How do you debug production issues?
**Answer**:
"Systematic approach:

**Step 1**: Check Grafana dashboards - which service has issues?
**Step 2**: Check logs - what's the error?
**Step 3**: Check Zipkin - where's the bottleneck?
**Step 4**: Check Kafka - messages stuck?
**Step 5**: Check database - data inconsistency?

**Tools**:
- Actuator endpoints
- Docker logs
- Kafka consumer groups
- Database queries

**Example**: Order stuck in PENDING
1. Grafana shows payment service errors
2. Logs show connection refused
3. Payment gateway is down
4. Restart gateway, orders process

**Result**: Quick issue resolution with multiple tools."

---

## 10. PERFORMANCE & SCALABILITY

### Q35: How would you scale to 10x traffic?
**Answer**:
"Horizontal scaling at all layers:

**1. Microservices**: 
- 3-10 replicas per service
- Kubernetes HPA based on CPU

**2. Databases**:
- MySQL: Master-slave replication
- MongoDB: Sharding + replica sets

**3. Kafka**:
- 3 brokers, 10 partitions
- More parallelism

**4. Caching**:
- Redis for products (1 hour)
- Reduce database load

**5. CDN**:
- Static content on CloudFront
- Edge locations

**Result**: 10x capacity with horizontal scaling."

---

### Q36: What are the bottlenecks?
**Answer**:
"Identified bottlenecks:

**1. Payment Processing (10s)**: 95% of order time
- Solution: Async processing

**2. Database Writes**: Single master
- Solution: Write sharding

**3. Inventory Locking**: Concurrent updates
- Solution: Optimistic locking or Redis

**4. Single Kafka Partition**: Limited parallelism
- Solution: 10 partitions, 10 consumers

**5. API Gateway**: Single point of failure
- Solution: Multiple instances + load balancer

**Result**: Each bottleneck has solution."

---

## 11. CODE-LEVEL QUESTIONS

### Q37: Show me the Order creation code.
**Answer**:
```java
@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository repository;
    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;
    
    @CircuitBreaker(name = "orderService", fallbackMethod = "fallbackCreateOrder")
    @Retry(name = "orderService")
    @Transactional
    public Order createOrder(Order order) {
        // 1. Idempotency check
        if (order.getIdempotencyKey() != null) {
            Order existing = repository.findByIdempotencyKey(
                order.getIdempotencyKey()
            );
            if (existing != null) return existing;
        } else {
            order.setIdempotencyKey(UUID.randomUUID().toString());
        }
        
        // 2. Calculate total
        BigDecimal total = order.getLineItems().stream()
            .map(item -> item.getPrice().multiply(
                BigDecimal.valueOf(item.getQuantity())
            ))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalAmount(total);
        
        // 3. Set status
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentStatus(PaymentStatus.PENDING);
        
        // 4. Save
        Order saved = repository.save(order);
        
        // 5. Publish events
        saved.getLineItems().forEach(item -> 
            kafkaTemplate.send("order-events", 
                new OrderPlacedEvent(saved.getId(), 
                    item.getProductId(), item.getQuantity())
            )
        );
        
        return saved;
    }
}
```

**Key Points**:
- Idempotency with UUID
- Circuit breaker for resilience
- Transaction for consistency
- Event publishing for async flow

**Result**: Production-ready order creation."

---

### Q38: How do you handle Kafka events?
**Answer**:
```java
@Service
@Slf4j
public class PaymentEventListener {
    
    @Autowired
    private OrderRepository repository;
    
    @KafkaListener(
        topics = "payment-events",
        groupId = "order-service-group"
    )
    @Transactional
    public void handlePaymentEvent(
        ConsumerRecord<String, PaymentEvent> record,
        Acknowledgment ack
    ) {
        try {
            PaymentEvent event = record.value();
            log.info("Received payment event: {}", event);
            
            Order order = repository.findById(event.getOrderId())
                .orElseThrow();
            
            if (event.getStatus() == PaymentStatus.COMPLETED) {
                // Success path
                order.setStatus(OrderStatus.CONFIRMED);
                order.setPaymentStatus(PaymentStatus.COMPLETED);
                order.setConfirmedAt(LocalDateTime.now());
                repository.save(order);
                
                // Notify warehouse
                publishWarehouseEvent(order);
            } else {
                // Failure path - compensate
                order.setStatus(OrderStatus.CANCELLED);
                order.setPaymentStatus(PaymentStatus.FAILED);
                repository.save(order);
                
                // Release inventory
                publishInventoryRelease(order);
            }
            
            // Commit only after success
            ack.acknowledge();
            
        } catch (Exception e) {
            log.error("Failed to process payment event", e);
            // Don't acknowledge - will retry
            throw e;
        }
    }
}
```

**Key Points**:
- Manual acknowledgment
- Transaction for consistency
- Compensating transaction on failure
- Error handling with retry

**Result**: Reliable event processing."

---

## 12. BEST PRACTICES & IMPROVEMENTS

### Q39: What would you improve in this project?
**Answer**:
"Several improvements for production:

**1. Security Enhancements**:
- mTLS between services
- API rate limiting per user
- Token refresh rotation
- Secrets management (Vault)

**2. Observability**:
- Centralized logging (ELK)
- Custom business metrics
- Distributed tracing sampling
- Real-time alerting (PagerDuty)

**3. Resilience**:
- Chaos engineering tests
- Disaster recovery plan
- Multi-region deployment
- Database backups

**4. Performance**:
- Redis caching layer
- Database query optimization
- Connection pooling tuning
- CDN for static content

**5. Testing**:
- Integration tests
- Load testing
- Chaos testing
- Contract testing

**6. CI/CD**:
- Automated deployments
- Blue-green deployment
- Canary releases
- Rollback automation

**7. Documentation**:
- API documentation (Swagger)
- Architecture diagrams
- Runbooks for operations
- Disaster recovery procedures

**Result**: Production-ready improvements identified."

---

### Q40: What did you learn from this project?
**Answer**:
"Key learnings:

**1. Microservices Complexity**:
- Distributed systems are hard
- Need proper monitoring
- Debugging is challenging
- But benefits outweigh complexity

**2. Event-Driven Architecture**:
- Loose coupling is powerful
- Eventual consistency is acceptable
- Kafka is reliable
- Need idempotency everywhere

**3. Saga Pattern**:
- Distributed transactions possible
- Compensating transactions work
- Need careful design
- State machine helps

**4. Observability is Critical**:
- Can't debug without traces
- Metrics show problems early
- Logs are essential
- Health checks prevent issues

**5. Docker Simplifies Deployment**:
- Consistent environments
- Easy local development
- Kubernetes for production
- Infrastructure as code

**6. Security is Hard**:
- OAuth2 is complex
- Token management tricky
- Need defense in depth
- Regular security audits

**7. Testing is Essential**:
- Unit tests not enough
- Integration tests critical
- Load testing reveals issues
- Chaos testing builds confidence

**Result**: Comprehensive understanding of microservices architecture."

---

## FINAL SUMMARY

**Project Highlights**:
✅ 10 microservices + 8 infrastructure services
✅ Event-driven with Kafka (8 topics)
✅ Saga pattern for distributed transactions
✅ OAuth2 with Keycloak
✅ Fully containerized (Docker Compose)
✅ Complete observability (Zipkin, Prometheus, Grafana)
✅ Production-ready patterns (Circuit breaker, Retry, Idempotency)
✅ Polyglot persistence (MySQL + MongoDB)

**Key Achievements**:
- 100-second order flow (PENDING → DELIVERED)
- Reserve-deduct inventory pattern
- Automatic failure handling
- Zero data loss with Kafka
- Horizontal scalability
- Complete monitoring

**Technologies Mastered**:
- Spring Boot 3.2, Spring Cloud
- Apache Kafka, Zookeeper
- MySQL, MongoDB
- Keycloak OAuth2
- Docker, Docker Compose
- Zipkin, Prometheus, Grafana
- Resilience4j

**Ready to Answer**:
- Architecture decisions
- Technology choices
- Failure scenarios
- Scaling strategies
- Code implementation
- Production deployment
- Monitoring & debugging
- Best practices

---

**Total Questions Covered: 40**
**You are now fully prepared for any interview question about this project!**


## 13. ADVANCED SCENARIO QUESTIONS

### Q41: What happens if Kafka goes down?
**Answer**:
"If Kafka goes down, here's what happens:

**Immediate Impact**:
- Order Service can't publish events
- Payment/Warehouse/Shipping services can't consume events
- New orders fail to process
- Existing orders stuck in current state

**My Handling**:

**1. Circuit Breaker on Kafka Producer**:
```java
@CircuitBreaker(name = "kafka", fallbackMethod = "kafkaFallback")
public void publishEvent(OrderEvent event) {
    kafkaTemplate.send("order-events", event);
}

public void kafkaFallback(OrderEvent event, Exception e) {
    // Store in database for later retry
    eventOutbox.save(event);
    log.error("Kafka down, saved to outbox: {}", event);
}
```

**2. Outbox Pattern**:
```java
// Store events in database when Kafka down
@Entity
public class EventOutbox {
    private String eventType;
    private String payload;
    private boolean processed;
}

// Background job retries
@Scheduled(fixedRate = 60000)
public void retryFailedEvents() {
    if (kafkaHealthCheck.isUp()) {
        List<EventOutbox> pending = outboxRepository.findByProcessedFalse();
        pending.forEach(event -> {
            kafkaTemplate.send(event.getEventType(), event.getPayload());
            event.setProcessed(true);
            outboxRepository.save(event);
        });
    }
}
```

**3. Graceful Degradation**:
```
- Accept orders but mark as "PENDING_PROCESSING"
- Show user: "Order received, processing may be delayed"
- Process when Kafka comes back
```

**Recovery**:
```
1. Kafka comes back online
2. Background job detects Kafka is up
3. Publishes all events from outbox
4. Services consume and process
5. Orders progress normally
```

**Result**: No data loss, graceful degradation, automatic recovery."

---

### Q42: How do you handle database failover?
**Answer**:
"Database failover strategy:

**MySQL Master-Slave Setup**:
```
Master (writes) → Slave1, Slave2, Slave3 (reads)
If Master fails → Promote Slave1 to Master
```

**Spring Boot Configuration**:
```java
@Configuration
public class DataSourceConfig {
    
    @Bean
    public DataSource dataSource() {
        HikariDataSource master = new HikariDataSource();
        master.setJdbcUrl("jdbc:mysql://mysql-master:3306/orderdb");
        
        HikariDataSource slave = new HikariDataSource();
        slave.setJdbcUrl("jdbc:mysql://mysql-slave:3306/orderdb");
        
        return new RoutingDataSource(master, slave);
    }
}

public class RoutingDataSource extends AbstractRoutingDataSource {
    @Override
    protected Object determineCurrentLookupKey() {
        return TransactionSynchronizationManager.isCurrentTransactionReadOnly() 
            ? "slave" : "master";
    }
}
```

**Automatic Failover**:
```properties
spring.datasource.hikari.connection-timeout=5000
spring.datasource.hikari.validation-timeout=3000
spring.datasource.hikari.connection-test-query=SELECT 1
```

**Health Check**:
```java
@Component
public class DatabaseHealthCheck {
    
    @Scheduled(fixedRate = 10000)
    public void checkHealth() {
        try {
            dataSource.getConnection().isValid(3);
        } catch (Exception e) {
            alertService.sendAlert("Database connection failed");
            // Trigger failover
        }
    }
}
```

**Result**: Automatic failover with minimal downtime."

---

### Q43: What if two users order the last item simultaneously?
**Answer**:
"Race condition on inventory - here's my solution:

**Problem**:
```
Inventory: 1 item left
User A orders → Check stock (1 available) ✓
User B orders → Check stock (1 available) ✓
Both orders succeed → Oversold!
```

**Solution 1: Database Lock (Current)**:
```java
@Transactional
public void reserveStock(String productId, int quantity) {
    // Pessimistic lock
    Inventory inv = repository.findByProductIdForUpdate(productId);
    
    int available = inv.getQuantity() - inv.getReservedQuantity();
    if (available < quantity) {
        throw new InsufficientStockException();
    }
    
    inv.setReservedQuantity(inv.getReservedQuantity() + quantity);
    repository.save(inv);
}

// Repository
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT i FROM Inventory i WHERE i.productId = :productId")
Inventory findByProductIdForUpdate(@Param("productId") String productId);
```

**What Happens**:
```
User A: Acquires lock on inventory row
User B: Waits for lock
User A: Reserves item, commits, releases lock
User B: Acquires lock, checks stock (0 available), fails
User B: Gets "Out of stock" error
```

**Solution 2: Optimistic Locking**:
```java
@Entity
public class Inventory {
    @Version
    private Long version;
}

@Transactional
public void reserveStock(String productId, int quantity) {
    Inventory inv = repository.findByProductId(productId);
    
    int available = inv.getQuantity() - inv.getReservedQuantity();
    if (available < quantity) {
        throw new InsufficientStockException();
    }
    
    inv.setReservedQuantity(inv.getReservedQuantity() + quantity);
    repository.save(inv); // Version checked
}
```

**What Happens**:
```
User A: Read (version=1)
User B: Read (version=1)
User A: Update (version=2) ✓
User B: Update fails (version mismatch)
User B: Retry with fresh data
User B: Gets "Out of stock"
```

**Solution 3: Redis Atomic Operations**:
```java
public boolean reserveStock(String productId, int quantity) {
    String key = "inventory:" + productId;
    Long remaining = redisTemplate.opsForValue()
        .decrement(key, quantity);
    
    if (remaining < 0) {
        // Rollback
        redisTemplate.opsForValue().increment(key, quantity);
        return false;
    }
    return true;
}
```

**Result**: No overselling, one user succeeds, other gets proper error."

---

### Q44: How do you handle service version upgrades?
**Answer**:
"Zero-downtime deployment strategy:

**1. Rolling Update (Kubernetes)**:
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: order-service
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
```

**Process**:
```
Current: v1.0 (3 pods)
Deploy: v1.1

Step 1: Create v1.1 pod → 3 v1.0 + 1 v1.1
Step 2: Health check v1.1 → Pass
Step 3: Terminate 1 v1.0 pod → 2 v1.0 + 1 v1.1
Step 4: Create v1.1 pod → 2 v1.0 + 2 v1.1
Step 5: Terminate 1 v1.0 pod → 1 v1.0 + 2 v1.1
Step 6: Create v1.1 pod → 1 v1.0 + 3 v1.1
Step 7: Terminate last v1.0 → 3 v1.1

Zero downtime!
```

**2. Blue-Green Deployment**:
```
Blue (v1.0) → All traffic
Green (v1.1) → Deploy, test
Switch traffic: Blue → Green
Keep Blue for rollback
```

**3. Backward Compatibility**:
```java
// Old version
public class Order {
    private Long id;
    private String status;
}

// New version - add field
public class Order {
    private Long id;
    private String status;
    private String trackingNumber; // New field
    
    // Make it optional for backward compatibility
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String trackingNumber;
}
```

**4. Database Migrations**:
```sql
-- Add column (backward compatible)
ALTER TABLE orders ADD COLUMN tracking_number VARCHAR(255);

-- Don't drop columns immediately
-- Mark as deprecated, drop in next version
```

**5. API Versioning**:
```java
@RestController
@RequestMapping("/api/v1/orders")
public class OrderControllerV1 {
    // Old API
}

@RestController
@RequestMapping("/api/v2/orders")
public class OrderControllerV2 {
    // New API with breaking changes
}
```

**Result**: Zero-downtime upgrades with rollback capability."

---

### Q45: What if Eureka Server goes down?
**Answer**:
"Eureka failure handling:

**Impact**:
- Services can't register
- Services can't discover each other
- New instances can't join

**But Services Keep Working**:
```
Services cache registry locally
Continue using cached service locations
No immediate impact on running services
```

**Eureka Client Configuration**:
```properties
# Cache registry locally
eureka.client.fetch-registry=true
eureka.client.registry-fetch-interval-seconds=30

# Use cached registry if Eureka down
eureka.client.disable-delta=false

# Retry connection
eureka.client.initial-instance-info-replication-interval-seconds=40
```

**High Availability Setup**:
```yaml
# Multiple Eureka servers
eureka-server-1:
  ports: ["8761:8761"]
  
eureka-server-2:
  ports: ["8762:8761"]
  
eureka-server-3:
  ports: ["8763:8761"]

# Services connect to all
eureka.client.service-url.defaultZone=
  http://eureka-server-1:8761/eureka,
  http://eureka-server-2:8762/eureka,
  http://eureka-server-3:8763/eureka
```

**Peer-to-Peer Replication**:
```
Eureka1 ↔ Eureka2 ↔ Eureka3
All replicate registry
If one fails, others continue
```

**Fallback**:
```java
@Configuration
public class ServiceDiscoveryConfig {
    
    @Bean
    public DiscoveryClient discoveryClient() {
        return new FallbackDiscoveryClient();
    }
}

public class FallbackDiscoveryClient implements DiscoveryClient {
    
    private Map<String, String> fallbackUrls = Map.of(
        "ORDER-SERVICE", "http://order-service:8083",
        "PRODUCT-SERVICE", "http://product-service:8082"
    );
    
    @Override
    public List<ServiceInstance> getInstances(String serviceId) {
        // Try Eureka first
        try {
            return eurekaClient.getInstances(serviceId);
        } catch (Exception e) {
            // Fallback to hardcoded URLs
            return createFallbackInstance(serviceId);
        }
    }
}
```

**Result**: Services continue working with cached registry, HA setup prevents single point of failure."

---

### Q46: How do you test your microservices?
**Answer**:
"Multi-level testing strategy:

**1. Unit Tests**:
```java
@SpringBootTest
class OrderServiceTest {
    
    @Mock
    private OrderRepository repository;
    
    @Mock
    private KafkaTemplate kafkaTemplate;
    
    @InjectMocks
    private OrderService service;
    
    @Test
    void createOrder_Success() {
        Order order = new Order();
        order.setLineItems(List.of(
            new LineItem("prod-1", 2, BigDecimal.valueOf(10))
        ));
        
        when(repository.save(any())).thenReturn(order);
        
        Order result = service.createOrder(order);
        
        assertEquals(BigDecimal.valueOf(20), result.getTotalAmount());
        verify(kafkaTemplate, times(1)).send(any(), any());
    }
}
```

**2. Integration Tests**:
```java
@SpringBootTest
@Testcontainers
class OrderServiceIntegrationTest {
    
    @Container
    static MySQLContainer mysql = new MySQLContainer("mysql:8.0");
    
    @Container
    static KafkaContainer kafka = new KafkaContainer();
    
    @Autowired
    private OrderService service;
    
    @Test
    void orderFlow_EndToEnd() {
        // Create order
        Order order = service.createOrder(createTestOrder());
        assertEquals(OrderStatus.PENDING, order.getStatus());
        
        // Simulate payment success
        PaymentEvent event = new PaymentEvent(order.getId(), COMPLETED);
        paymentListener.handlePaymentEvent(event);
        
        // Verify order confirmed
        Order updated = service.getOrderById(order.getId());
        assertEquals(OrderStatus.CONFIRMED, updated.getStatus());
    }
}
```

**3. Contract Tests**:
```java
@SpringBootTest
@AutoConfigureStubRunner(
    ids = "com.ecommerce:payment-service:+:stubs:8086"
)
class PaymentServiceContractTest {
    
    @Test
    void paymentService_ReturnsSuccess() {
        // Verify contract with payment service
        PaymentResponse response = paymentClient.processPayment(request);
        assertEquals("SUCCESS", response.getStatus());
    }
}
```

**4. Load Tests (JMeter)**:
```xml
<ThreadGroup>
  <numThreads>1000</numThreads>
  <rampUp>60</rampUp>
  <duration>600</duration>
</ThreadGroup>

<HTTPSampler>
  <path>/api/orders</path>
  <method>POST</method>
</HTTPSampler>
```

**5. Chaos Testing**:
```java
// Kill random service
@Test
void orderFlow_WithServiceFailure() {
    // Start order
    Order order = createOrder();
    
    // Kill payment service
    docker.stop("payment-service");
    
    // Wait
    Thread.sleep(5000);
    
    // Restart
    docker.start("payment-service");
    
    // Verify order eventually completes
    await().atMost(2, MINUTES)
        .until(() -> getOrder(order.getId()).getStatus() == DELIVERED);
}
```

**6. End-to-End Tests**:
```java
@SpringBootTest
@DirtiesContext
class E2ETest {
    
    @Test
    void completeOrderFlow() {
        // 1. Create product
        Product product = productService.create(testProduct());
        
        // 2. Add inventory
        inventoryService.create(product.getId(), 50);
        
        // 3. Create order
        Order order = orderService.create(testOrder(product.getId()));
        
        // 4. Wait for completion
        await().atMost(2, MINUTES)
            .until(() -> {
                Order current = orderService.getById(order.getId());
                return current.getStatus() == DELIVERED;
            });
        
        // 5. Verify inventory deducted
        Inventory inv = inventoryService.get(product.getId());
        assertEquals(47, inv.getQuantity());
    }
}
```

**Test Coverage**:
```
Unit Tests: 80%+ coverage
Integration Tests: Critical paths
Contract Tests: All service interactions
Load Tests: 1000 concurrent users
Chaos Tests: Random failures
E2E Tests: Complete user journeys
```

**Result**: Comprehensive testing at all levels ensures reliability."

---

### Q47: How do you handle data migration in microservices?
**Answer**:
"Data migration strategy:

**Scenario**: Need to split Order Service into Order Service + Shipping Service

**Step 1: Dual Write**:
```java
@Service
public class OrderService {
    
    @Transactional
    public Order createOrder(Order order) {
        // Write to old location
        Order saved = orderRepository.save(order);
        
        // Also write to new location
        ShippingInfo shipping = new ShippingInfo();
        shipping.setOrderId(saved.getId());
        shipping.setAddress(saved.getShippingAddress());
        shippingRepository.save(shipping);
        
        return saved;
    }
}
```

**Step 2: Backfill Historical Data**:
```java
@Service
public class DataMigrationService {
    
    public void migrateShippingData() {
        List<Order> orders = orderRepository.findAll();
        
        orders.forEach(order -> {
            if (!shippingRepository.existsByOrderId(order.getId())) {
                ShippingInfo shipping = new ShippingInfo();
                shipping.setOrderId(order.getId());
                shipping.setAddress(order.getShippingAddress());
                shippingRepository.save(shipping);
            }
        });
    }
}
```

**Step 3: Switch Reads**:
```java
// Old
public ShippingInfo getShipping(Long orderId) {
    Order order = orderRepository.findById(orderId);
    return order.getShippingInfo();
}

// New
public ShippingInfo getShipping(Long orderId) {
    return shippingRepository.findByOrderId(orderId);
}
```

**Step 4: Stop Dual Write**:
```java
// Remove dual write code
public Order createOrder(Order order) {
    Order saved = orderRepository.save(order);
    // Removed: shippingRepository.save(shipping);
    
    // Publish event instead
    kafkaTemplate.send("order-created", new OrderCreatedEvent(saved));
    return saved;
}
```

**Step 5: Remove Old Data**:
```sql
-- After verification
ALTER TABLE orders DROP COLUMN shipping_address;
```

**Zero-Downtime Migration**:
```
Week 1: Deploy dual write
Week 2: Backfill historical data
Week 3: Verify data consistency
Week 4: Switch reads to new location
Week 5: Stop dual write
Week 6: Remove old columns
```

**Result**: Safe data migration without downtime."

---

### Q48: What's your disaster recovery plan?
**Answer**:
"Comprehensive DR strategy:

**1. Backup Strategy**:
```bash
# MySQL - Daily backups
mysqldump --all-databases > backup-$(date +%Y%m%d).sql
# Upload to S3
aws s3 cp backup.sql s3://backups/mysql/

# MongoDB - Continuous backup
mongodump --out=/backup/$(date +%Y%m%d)
# Upload to S3
aws s3 sync /backup s3://backups/mongodb/

# Retention: 30 days
```

**2. Multi-Region Deployment**:
```
Primary Region: us-east-1
DR Region: us-west-2

Active-Passive:
- Primary handles all traffic
- DR region on standby
- Database replication to DR
- Switch on failure
```

**3. Database Replication**:
```
MySQL:
Primary (us-east-1) → Replica (us-west-2)
Async replication
RPO: 5 minutes

MongoDB:
Replica Set across regions
Automatic failover
```

**4. Disaster Scenarios**:

**Scenario 1: Single Service Failure**
```
Detection: Health check fails
Action: Kubernetes restarts pod
Time: 30 seconds
Impact: Minimal (other replicas handle traffic)
```

**Scenario 2: Database Failure**
```
Detection: Connection timeout
Action: Promote replica to master
Time: 2 minutes
Impact: Brief read-only mode
```

**Scenario 3: Complete Region Failure**
```
Detection: All health checks fail
Action: 
1. DNS failover to DR region
2. Promote DR database to primary
3. Scale up DR services
Time: 10 minutes
Impact: 10 minutes downtime
```

**5. Recovery Procedures**:
```
1. Detect failure (monitoring alerts)
2. Assess impact (which services affected)
3. Execute runbook (documented procedures)
4. Communicate (status page, emails)
5. Restore service (failover or fix)
6. Verify (health checks, smoke tests)
7. Post-mortem (what happened, how to prevent)
```

**6. Testing**:
```
Monthly: Backup restore test
Quarterly: DR failover drill
Annually: Full disaster simulation
```

**Result**: Prepared for any disaster with documented procedures."

---

### Q49: How do you ensure GDPR compliance?
**Answer**:
"GDPR compliance implementation:

**1. Data Inventory**:
```
Personal Data Stored:
- User: email, name, phone, address
- Order: shipping address, billing info
- Payment: last 4 digits (not full card)
```

**2. Right to Access**:
```java
@RestController
public class GDPRController {
    
    @GetMapping("/api/gdpr/my-data")
    public UserDataExport exportMyData(@AuthenticationPrincipal User user) {
        UserDataExport export = new UserDataExport();
        
        // Collect all user data
        export.setProfile(userService.getProfile(user.getId()));
        export.setOrders(orderService.getUserOrders(user.getId()));
        export.setAddresses(addressService.getUserAddresses(user.getId()));
        
        return export;
    }
}
```

**3. Right to Deletion**:
```java
@DeleteMapping("/api/gdpr/delete-my-data")
@Transactional
public void deleteMyData(@AuthenticationPrincipal User user) {
    // Anonymize instead of delete (for audit)
    user.setEmail("deleted-" + UUID.randomUUID() + "@deleted.com");
    user.setName("Deleted User");
    user.setPhone(null);
    user.setDeleted(true);
    userRepository.save(user);
    
    // Delete from all services
    orderService.anonymizeUserOrders(user.getId());
    addressService.deleteUserAddresses(user.getId());
    
    // Publish event
    kafkaTemplate.send("user-deleted", new UserDeletedEvent(user.getId()));
}
```

**4. Data Encryption**:
```java
@Entity
public class User {
    @Convert(converter = EncryptedStringConverter.class)
    private String email;
    
    @Convert(converter = EncryptedStringConverter.class)
    private String phone;
}

public class EncryptedStringConverter implements AttributeConverter<String, String> {
    @Override
    public String convertToDatabaseColumn(String attribute) {
        return encryptionService.encrypt(attribute);
    }
    
    @Override
    public String convertToEntityAttribute(String dbData) {
        return encryptionService.decrypt(dbData);
    }
}
```

**5. Audit Logging**:
```java
@Aspect
@Component
public class DataAccessAudit {
    
    @Around("@annotation(LogDataAccess)")
    public Object logAccess(ProceedingJoinPoint joinPoint) {
        String userId = getCurrentUserId();
        String action = joinPoint.getSignature().getName();
        
        auditLog.save(new AuditEntry(
            userId, action, LocalDateTime.now()
        ));
        
        return joinPoint.proceed();
    }
}
```

**6. Consent Management**:
```java
@Entity
public class UserConsent {
    private String userId;
    private ConsentType type; // MARKETING, ANALYTICS
    private boolean granted;
    private LocalDateTime grantedAt;
}

public void sendMarketingEmail(User user) {
    if (!consentService.hasConsent(user.getId(), MARKETING)) {
        log.info("User {} has not consented to marketing", user.getId());
        return;
    }
    emailService.send(user.getEmail(), marketingContent);
}
```

**7. Data Retention**:
```java
@Scheduled(cron = "0 0 2 * * *") // 2 AM daily
public void deleteOldData() {
    // Delete orders older than 7 years
    LocalDateTime cutoff = LocalDateTime.now().minusYears(7);
    List<Order> oldOrders = orderRepository
        .findByCreatedAtBefore(cutoff);
    
    oldOrders.forEach(order -> {
        orderRepository.delete(order);
        log.info("Deleted old order: {}", order.getId());
    });
}
```

**Result**: Full GDPR compliance with user rights and data protection."

---

### Q50: How would you add a new microservice to this system?
**Answer**:
"Step-by-step process to add Review Service:

**Step 1: Create Service Structure**:
```
review-service/
├── src/main/java/com/ecommerce/review/
│   ├── ReviewServiceApplication.java
│   ├── controller/ReviewController.java
│   ├── service/ReviewService.java
│   ├── repository/ReviewRepository.java
│   ├── model/Review.java
│   └── event/ReviewEventListener.java
├── src/main/resources/
│   ├── application.properties
│   └── application-docker.properties
├── Dockerfile
└── pom.xml
```

**Step 2: Add Dependencies**:
```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.kafka</groupId>
        <artifactId>spring-kafka</artifactId>
    </dependency>
</dependencies>
```

**Step 3: Configure Service**:
```properties
spring.application.name=review-service
server.port=8090

# Eureka
eureka.client.service-url.defaultZone=http://eureka-server:8761/eureka

# Kafka
spring.kafka.bootstrap-servers=kafka:9092

# MongoDB
spring.data.mongodb.uri=mongodb://mongodb:27017/reviewdb
```

**Step 4: Create Dockerfile**:
```dockerfile
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/review-service-1.0.0.jar app.jar
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Step 5: Add to Docker Compose**:
```yaml
review-service:
  image: tejaswini2703/review-service:1.0.0
  container_name: review-service
  depends_on:
    - eureka-server
    - mongodb
    - kafka
  environment:
    - SPRING_PROFILES_ACTIVE=docker
  ports:
    - "8090:8090"
  networks:
    - ecommerce-network
```

**Step 6: Implement Service**:
```java
@RestController
@RequestMapping("/api/reviews")
public class ReviewController {
    
    @Autowired
    private ReviewService service;
    
    @PostMapping
    public Review createReview(@RequestBody Review review) {
        return service.createReview(review);
    }
    
    @GetMapping("/product/{productId}")
    public List<Review> getProductReviews(@PathVariable String productId) {
        return service.getReviewsByProduct(productId);
    }
}
```

**Step 7: Subscribe to Events**:
```java
@Service
public class ReviewEventListener {
    
    @KafkaListener(topics = "order-status-events")
    public void handleOrderDelivered(OrderStatusEvent event) {
        if (event.getStatus() == OrderStatus.DELIVERED) {
            // Send review request email
            emailService.sendReviewRequest(event.getOrderId());
        }
    }
}
```

**Step 8: Update API Gateway**:
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: review-service
          uri: lb://REVIEW-SERVICE
          predicates:
            - Path=/api/reviews/**
```

**Step 9: Deploy**:
```bash
# Build
mvn clean package

# Build Docker image
docker build -t tejaswini2703/review-service:1.0.0 .

# Push to registry
docker push tejaswini2703/review-service:1.0.0

# Start
docker-compose up -d review-service
```

**Step 10: Verify**:
```bash
# Check Eureka
curl http://localhost:8761/eureka/apps/REVIEW-SERVICE

# Test endpoint
curl http://localhost:8081/api/reviews/product/prod-123
```

**Result**: New service integrated seamlessly with existing system."

---

**Total Questions: 50 comprehensive interview questions covering every aspect!**
