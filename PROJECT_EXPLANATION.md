# E-Commerce Microservices Platform - Complete End-to-End Explanation

## Table of Contents
1. Project Overview
2. System Architecture
3. Technology Stack
4. Microservices Breakdown
5. Data Flow & Communication
6. Security Implementation
7. Order Lifecycle (Complete Flow)
8. Failure Handling & Saga Pattern
9. Database Design
10. Deployment & Containerization
11. Monitoring & Observability
12. Testing Guide

---

## 1. Project Overview

### What is this project?
A production-ready e-commerce platform built using microservices architecture. It handles the complete order lifecycle from product browsing to delivery, with robust failure handling and distributed transaction management.

### Key Characteristics
- **Event-Driven**: Services communicate asynchronously via Apache Kafka
- **Saga Pattern**: Distributed transactions with compensating actions
- **OAuth2 Security**: Keycloak-based authentication with Bearer tokens
- **Containerized**: All services run in Docker containers
- **Observable**: Complete tracing, metrics, and monitoring
- **Resilient**: Circuit breakers, retries, and fault tolerance

### Business Flow
```
Customer → Browse Products → Add to Cart → Place Order → Payment → 
Warehouse Picks Items → Shipping Dispatches → Delivery Completes → Order Fulfilled
```

---

## 2. System Architecture

### Architecture Layers

**1. Security Layer**
- Keycloak (OAuth2 Server) - Handles authentication and authorization

**2. API Layer**
- API Gateway - Single entry point, routes requests, validates tokens
- Eureka Server - Service discovery and registry

**3. Microservices Layer (10 Services)**
- Product Service - Product catalog management
- Order Service - Saga coordinator, orchestrates order flow
- Inventory Service - Stock management (reserve/deduct pattern)
- Payment Service - Payment processing
- Warehouse Service - Item picking and preparation
- Shipping Service - Courier dispatch
- Delivery Service - Final delivery
- Notification Service - Event logging and notifications

**4. Messaging Layer**
- Apache Kafka - Event streaming backbone
- Zookeeper - Kafka cluster coordination

**5. Data Layer**
- MySQL - Orders and Inventory (relational data)
- MongoDB - Products (document store)

**6. Observability Layer**
- Zipkin - Distributed tracing
- Prometheus - Metrics collection
- Grafana - Visualization dashboards

### Communication Patterns

**Synchronous (REST)**
- Client → API Gateway → Microservices
- Used for: CRUD operations, queries

**Asynchronous (Kafka Events)**
- Service → Kafka Topic → Service
- Used for: Order flow, status updates, notifications

---

## 3. Technology Stack

### Backend Framework
- Spring Boot 3.2.x - Core framework
- Spring Cloud - Microservices infrastructure (Eureka, Gateway)
- Spring Kafka - Event streaming integration
- Spring Data JPA - Database access for MySQL
- Spring Data MongoDB - MongoDB integration
- Spring Security OAuth2 - Authentication

### Databases
- MySQL 8.0 - Orders, Inventory (ACID transactions)
- MongoDB 7.0 - Products (flexible schema)

### Messaging
- Apache Kafka 7.4.4 - Event streaming platform
- Zookeeper 7.4.4 - Kafka coordination

### Security
- Keycloak 24.0 - OAuth2/OpenID Connect server
- Bearer Token Authentication - Stateless auth

### Observability
- Zipkin - Distributed request tracing
- Prometheus - Time-series metrics database
- Grafana - Metrics visualization
- Spring Boot Actuator - Health checks and metrics endpoints

### Resilience
- Resilience4j - Circuit breaker, retry, bulkhead, rate limiter

### Build & Deployment
- Maven 3.8+ - Build automation
- Docker - Containerization
- Docker Compose - Multi-container orchestration
- Java 17+ - Runtime environment

---

## 4. Microservices Breakdown

### 4.1 Eureka Server (Port 8761)
**Purpose**: Service discovery and registry

**Functionality**:
- All microservices register themselves on startup
- Services can discover each other dynamically
- Health monitoring of registered services
- Load balancing information

**Key Configuration**:
```properties
eureka.client.register-with-eureka=false
eureka.client.fetch-registry=false
```

**Security**: Basic Auth (eureka/eureka123)

---

### 4.2 API Gateway (Port 8081)
**Purpose**: Single entry point for all client requests

**Functionality**:
- Routes requests to appropriate microservices
- Validates Bearer tokens with Keycloak
- Service discovery via Eureka
- Load balancing
- Request/response logging

**Key Features**:
- Dynamic routing based on service discovery
- Token validation before forwarding requests
- Distributed tracing integration

**Routes**:
- `/product-service/**` → Product Service
- `/order-service/**` → Order Service
- `/inventory-service/**` → Inventory Service

---

### 4.3 Product Service (Port 8082)
**Purpose**: Product catalog management

**Database**: MongoDB (productdb)

**Endpoints**:
- `POST /api/products` - Create product
- `GET /api/products` - Get all products
- `GET /api/products/{id}` - Get product by ID

**Data Model**:
```json
{
  "_id": "prod-123",
  "name": "iPhone 15 Pro",
  "description": "Latest Apple smartphone",
  "price": 999.99,
  "createdAt": "2024-01-15T10:30:00Z"
}
```

**Key Features**:
- MongoDB for flexible product schema
- No complex transactions needed
- Fast read operations

---

### 4.4 Order Service (Port 8083)
**Purpose**: Saga coordinator - orchestrates entire order flow

**Database**: MySQL (orderdb)

**Endpoints**:
- `POST /api/orders` - Create order
- `GET /api/orders` - Get all orders
- `GET /api/orders/{id}` - Get order by ID
- `GET /api/orders/paged` - Paginated orders
- `PATCH /api/orders/{id}/status` - Update status
- `DELETE /api/orders/{id}/cancel` - Cancel order

**Data Model**:
```sql
orders (
  id, idempotency_key, status, payment_status,
  total_amount, payment_transaction_id,
  created_at, confirmed_at, processing_at,
  shipped_at, delivered_at, cancelled_at
)

order_line_items (
  id, order_id, product_id, quantity, price
)
```

**Key Responsibilities**:
1. Create order with PENDING status
2. Publish events to Kafka
3. Listen for payment/warehouse/shipping/delivery events
4. Update order status based on events
5. Handle failures and cancellations
6. Implement idempotency

**Kafka Topics (Producer)**:
- `order-events` - Order created
- `order-payment-events` - Trigger payment
- `order-status-events` - Status changes
- `inventory-reservation-events` - Stock deduction/release

**Kafka Topics (Consumer)**:
- `payment-events` - Payment result
- `warehouse-events` - Warehouse result
- `shipping-events` - Shipping result
- `delivery-events` - Delivery result

**Resilience**:
- Circuit Breaker: 50% failure threshold
- Retry: 3 attempts with exponential backoff
- Fallback: Create order with CANCELLED status

---

### 4.5 Inventory Service (Port 8084)
**Purpose**: Stock management with reserve-deduct pattern

**Database**: MySQL (inventorydb)

**Endpoints**:
- `POST /api/inventory` - Create inventory
- `GET /api/inventory/{productId}` - Get inventory
- `GET /api/inventory/check` - Check stock availability
- `GET /api/inventory` - Get all inventory

**Data Model**:
```sql
inventory (
  id, product_id, quantity, reserved_quantity, updated_at
)
```

**Formula**: `available = quantity - reserved_quantity`

**Key Operations**:

1. **Reserve Stock** (Order Created):
   - Increase `reserved_quantity`
   - Decrease available stock
   - Does NOT decrease actual quantity

2. **Deduct Stock** (Order Delivered):
   - Decrease `quantity`
   - Decrease `reserved_quantity`
   - Permanent stock reduction

3. **Release Stock** (Order Cancelled):
   - Decrease `reserved_quantity`
   - Increase available stock
   - Rollback reservation

**Kafka Topics (Consumer)**:
- `order-events` - Reserve stock
- `inventory-reservation-events` - Deduct or release stock

**Example Flow**:
```
Initial: quantity=50, reserved=0, available=50
Order Created: quantity=50, reserved=3, available=47
Order Delivered: quantity=47, reserved=0, available=47
```

---

### 4.6 Payment Service (Port 8086)
**Purpose**: Payment processing with simulated failures

**Endpoints**: None (event-driven only)

**Functionality**:
- Listens to `order-payment-events`
- Simulates payment processing (~10 seconds)
- 5% random failure rate (for testing)
- Publishes result to `payment-events`

**Payment Flow**:
1. Receive payment request from Kafka
2. Validate order details
3. Process payment (simulated delay)
4. Random 5% failure for testing
5. Publish SUCCESS or FAILED event

**Event Structure**:
```json
{
  "orderId": 123,
  "amount": 999.99,
  "status": "COMPLETED",
  "transactionId": "txn-abc123"
}
```

**Failure Handling**:
- If payment fails → Order status becomes CANCELLED
- Inventory reservation is released
- Compensating transaction triggered

---

### 4.7 Warehouse Service (Port 8087)
**Purpose**: Item picking and preparation

**Endpoints**: None (event-driven only)

**Functionality**:
- Listens to `order-status-events` (CONFIRMED status)
- Simulates warehouse operations (~18 seconds)
- Publishes result to `warehouse-events`

**Warehouse Flow**:
1. Receive CONFIRMED order
2. Pick items from warehouse
3. Prepare for shipping
4. Update order to PROCESSING
5. Notify shipping service

---

### 4.8 Shipping Service (Port 8088)
**Purpose**: Courier dispatch management

**Endpoints**: None (event-driven only)

**Functionality**:
- Listens to `order-status-events` (PROCESSING status)
- Simulates shipping operations (~20 seconds)
- Publishes result to `shipping-events`

**Shipping Flow**:
1. Receive PROCESSING order
2. Assign courier
3. Dispatch package
4. Update order to SHIPPED
5. Notify delivery service

---

### 4.9 Delivery Service (Port 8089)
**Purpose**: Final delivery completion

**Endpoints**: None (event-driven only)

**Functionality**:
- Listens to `order-status-events` (SHIPPED status)
- Simulates delivery operations (~52 seconds)
- Publishes result to `delivery-events`

**Delivery Flow**:
1. Receive SHIPPED order
2. Out for delivery
3. Delivery completed
4. Update order to DELIVERED
5. Trigger inventory deduction

---

### 4.10 Notification Service (Port 8085)
**Purpose**: Event logging and notifications

**Endpoints**: None (event-driven only)

**Functionality**:
- Listens to all `order-status-events`
- Logs every status change
- Can send emails/SMS (not implemented)
- Audit trail for orders

**Logged Events**:
- Order created
- Payment completed/failed
- Warehouse processing
- Shipping dispatched
- Delivery completed
- Order cancelled

---

## 5. Data Flow & Communication

### 5.1 Synchronous Communication (REST)

**Client → API Gateway → Microservices**

Example: Create Product
```
1. Client sends: POST /api/products with Bearer token
2. API Gateway validates token with Keycloak
3. API Gateway discovers Product Service via Eureka
4. API Gateway forwards request to Product Service
5. Product Service saves to MongoDB
6. Response flows back: Product Service → Gateway → Client
```

### 5.2 Asynchronous Communication (Kafka)

**Service → Kafka Topic → Service**

Example: Order Payment Flow
```
1. Order Service publishes to order-payment-events
2. Kafka stores event
3. Payment Service consumes event
4. Payment Service processes payment
5. Payment Service publishes to payment-events
6. Order Service consumes event
7. Order Service updates status
```

### 5.3 Kafka Topics Overview

| Topic | Producer | Consumer | Purpose |
|-------|----------|----------|---------|
| order-events | Order Service | Inventory, Notification | Stock reservation |
| order-payment-events | Order Service | Payment Service | Payment trigger |
| payment-events | Payment Service | Order Service | Payment result |
| order-status-events | Order Service | Warehouse, Shipping, Delivery, Notification | Status updates |
| warehouse-events | Warehouse Service | Order Service | Warehouse result |
| shipping-events | Shipping Service | Order Service | Shipping result |
| delivery-events | Delivery Service | Order Service | Delivery result |
| inventory-reservation-events | Order Service | Inventory Service | Stock deduction/release |

---

## 6. Security Implementation

### 6.1 Keycloak OAuth2 Setup

**Realm**: ecommerce
**Client**: backend-service
**Client Secret**: ecommerce-secret-2024
**Test User**: testuser / test123

### 6.2 Authentication Flow

```
1. Client → Keycloak: POST /realms/ecommerce/protocol/openid-connect/token
   Body: grant_type=password, client_id=backend-service, 
         client_secret=ecommerce-secret-2024, 
         username=testuser, password=test123

2. Keycloak → Client: access_token, refresh_token

3. Client → API Gateway: API Request with Authorization: Bearer {token}

4. API Gateway → Keycloak: Validate token

5. Keycloak → API Gateway: Token valid/invalid

6. API Gateway → Microservice: Forward request (if valid)
```

### 6.3 Token Management

**Access Token**:
- Expiry: 5 minutes
- Type: Bearer
- Used for: All API requests

**Refresh Token**:
- Expiry: 30 minutes
- Used for: Getting new access token

### 6.4 Security at Each Layer

**API Gateway**:
- Validates all incoming tokens
- Rejects requests with invalid/expired tokens
- Forwards only authenticated requests

**Microservices**:
- Trust API Gateway validation
- No direct token validation needed
- Focus on business logic

**Keycloak**:
- Centralized authentication
- User management
- Role-based access control

---

This is Part 1 of the explanation. Continue to next section for Order Lifecycle and remaining topics.

## 7. Order Lifecycle (Complete Flow)

### 7.1 Happy Path Timeline

**T+0s: Order Creation (PENDING)**
```
1. Client authenticates with Keycloak → Gets Bearer token
2. Client sends POST /api/orders with token
3. API Gateway validates token
4. Order Service creates order with status=PENDING
5. Order Service generates idempotencyKey (UUID)
6. Order Service calculates total amount
7. Order Service saves to MySQL
8. Order Service publishes to order-events (for inventory)
9. Order Service publishes to order-payment-events (for payment)
10. Response returned to client with order ID
```

**T+0s: Inventory Reservation**
```
11. Inventory Service consumes order-events
12. For each line item:
    - Check if stock available
    - Increase reserved_quantity
    - Decrease available stock
13. If insufficient stock → Publish failure event
```

**T+10s: Payment Processing (CONFIRMED)**
```
14. Payment Service consumes order-payment-events
15. Simulate payment processing (10 second delay)
16. 95% success, 5% random failure
17. If success:
    - Publish payment-events with status=COMPLETED
18. If failure:
    - Publish payment-events with status=FAILED
```

**T+10s: Payment Success Handler**
```
19. Order Service consumes payment-events
20. If payment COMPLETED:
    - Update order status to CONFIRMED
    - Set confirmedAt timestamp
    - Set paymentStatus to COMPLETED
    - Save payment transaction ID
    - Publish order-status-events (CONFIRMED)
21. If payment FAILED:
    - Update order status to CANCELLED
    - Publish inventory-reservation-events (RELEASE)
    - Inventory releases reserved stock
```

**T+28s: Warehouse Processing (PROCESSING)**
```
22. Warehouse Service consumes order-status-events (CONFIRMED)
23. Simulate warehouse operations (18 second delay)
24. Pick items from warehouse
25. Prepare for shipping
26. Publish warehouse-events with status=SUCCESS
27. Order Service consumes warehouse-events
28. Update order status to PROCESSING
29. Set processingAt timestamp
30. Publish order-status-events (PROCESSING)
```

**T+48s: Shipping Dispatch (SHIPPED)**
```
31. Shipping Service consumes order-status-events (PROCESSING)
32. Simulate shipping operations (20 second delay)
33. Assign courier
34. Dispatch package
35. Publish shipping-events with status=SUCCESS
36. Order Service consumes shipping-events
37. Update order status to SHIPPED
38. Set shippedAt timestamp
39. Publish order-status-events (SHIPPED)
```

**T+100s: Delivery Completion (DELIVERED)**
```
40. Delivery Service consumes order-status-events (SHIPPED)
41. Simulate delivery operations (52 second delay)
42. Out for delivery
43. Delivery completed
44. Publish delivery-events with status=SUCCESS
45. Order Service consumes delivery-events
46. Update order status to DELIVERED
47. Set deliveredAt timestamp
48. Publish inventory-reservation-events (DEDUCT)
49. Inventory Service consumes event
50. Decrease quantity (permanent deduction)
51. Decrease reserved_quantity
52. Order complete!
```

**Throughout: Notification Service**
```
- Listens to all order-status-events
- Logs every status change
- Creates audit trail
- Can trigger emails/SMS
```

### 7.2 Status Progression

```
PENDING (0s)
   ↓
CONFIRMED (10s) - Payment successful
   ↓
PROCESSING (28s) - Warehouse picked items
   ↓
SHIPPED (48s) - Courier dispatched
   ↓
DELIVERED (100s) - Delivery completed
```

### 7.3 Inventory State Changes

```
Initial State:
- quantity: 50
- reserved_quantity: 0
- available: 50

After Order Created (PENDING):
- quantity: 50
- reserved_quantity: 3
- available: 47

After Order Delivered (DELIVERED):
- quantity: 47
- reserved_quantity: 0
- available: 47
```

---

## 8. Failure Handling & Saga Pattern

### 8.1 Saga Pattern Implementation

**What is Saga Pattern?**
A design pattern for managing distributed transactions across microservices. Instead of a single ACID transaction, it uses a sequence of local transactions with compensating actions for rollback.

**Our Implementation: Orchestration-based Saga**
- Order Service acts as the saga coordinator
- Each step publishes events
- Failures trigger compensating transactions
- All operations are idempotent

### 8.2 Payment Failure Scenario

**Timeline**:
```
T+0s: Order Created (PENDING)
1. Order Service creates order
2. Inventory reserves stock (50 → 47 available)
3. Order Service publishes payment event

T+10s: Payment Fails
4. Payment Service processes payment
5. Random 5% failure occurs
6. Payment Service publishes payment-events (FAILED)

T+10s: Compensating Transaction
7. Order Service consumes payment failure event
8. Order Service updates status to CANCELLED
9. Order Service publishes inventory-reservation-events (RELEASE)
10. Inventory Service releases reserved stock (47 → 50 available)
11. Notification Service logs cancellation

Result: Order cancelled, inventory restored, no data inconsistency
```

**Code Flow**:
```java
// Payment Service
if (Math.random() < 0.05) {
    // 5% failure
    event.setStatus(PaymentStatus.FAILED);
} else {
    event.setStatus(PaymentStatus.COMPLETED);
}
kafkaTemplate.send("payment-events", event);

// Order Service - Payment Event Listener
if (event.getStatus() == PaymentStatus.FAILED) {
    order.setStatus(OrderStatus.CANCELLED);
    order.setPaymentStatus(PaymentStatus.FAILED);
    repository.save(order);
    
    // Release inventory
    releaseEvent.put("action", "RELEASE");
    kafkaTemplate.send("inventory-reservation-events", releaseEvent);
}
```

### 8.3 Warehouse Failure Scenario

**Timeline**:
```
T+0s-10s: Order Created and Payment Successful (CONFIRMED)
T+28s: Warehouse Processing Fails
1. Warehouse Service encounters error
2. Publishes warehouse-events (FAILED)
3. Order Service updates status to CANCELLED
4. Releases inventory reservation
5. Refund initiated (if implemented)
```

### 8.4 Manual Cancellation

**Allowed States**: PENDING, CONFIRMED
**Not Allowed**: PROCESSING, SHIPPED, DELIVERED

**Code**:
```java
public void cancelOrder(Long orderId) {
    Order order = repository.findById(orderId)
        .orElseThrow(() -> new RuntimeException("Order not found"));
    
    if (order.getStatus() == OrderStatus.DELIVERED) {
        throw new RuntimeException("Cannot cancel delivered order");
    }
    
    if (order.getStatus() == OrderStatus.SHIPPED) {
        throw new RuntimeException("Cannot cancel shipped order");
    }
    
    if (order.getStatus() == OrderStatus.PROCESSING) {
        order.setRequiresManualIntervention(true);
        throw new RuntimeException("Manual intervention required");
    }
    
    // Cancel and release inventory
    order.setStatus(OrderStatus.CANCELLED);
    order.setCancelledAt(LocalDateTime.now());
    repository.save(order);
    
    // Release reserved stock
    order.getLineItems().forEach(item -> {
        releaseEvent.put("action", "RELEASE");
        kafkaTemplate.send("inventory-reservation-events", releaseEvent);
    });
}
```

### 8.5 Idempotency Implementation

**Purpose**: Prevent duplicate orders from network retries or user double-clicks

**Implementation**:
```java
public Order createOrder(Order order) {
    // Check if order already exists
    if (order.getIdempotencyKey() != null) {
        Order existing = repository.findByIdempotencyKey(
            order.getIdempotencyKey()
        );
        if (existing != null) {
            return existing; // Return existing order
        }
    } else {
        // Generate new idempotency key
        order.setIdempotencyKey(UUID.randomUUID().toString());
    }
    
    // Create new order
    order.setStatus(OrderStatus.PENDING);
    return repository.save(order);
}
```

**Database Constraint**:
```sql
CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    idempotency_key VARCHAR(255) UNIQUE NOT NULL,
    ...
);
```

**Result**: Same request twice returns same order ID, no duplicate charges

---

## 9. Database Design

### 9.1 MySQL - Order Service

**orders table**:
```sql
CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    idempotency_key VARCHAR(255) UNIQUE NOT NULL,
    status VARCHAR(50) NOT NULL,
    payment_status VARCHAR(50),
    total_amount DECIMAL(19,2) NOT NULL,
    payment_transaction_id VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    confirmed_at TIMESTAMP,
    processing_at TIMESTAMP,
    shipped_at TIMESTAMP,
    delivered_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    failure_reason TEXT,
    requires_manual_intervention BOOLEAN DEFAULT FALSE,
    INDEX idx_status (status),
    INDEX idx_idempotency (idempotency_key),
    INDEX idx_created_at (created_at)
);
```

**order_line_items table**:
```sql
CREATE TABLE order_line_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL,
    price DECIMAL(19,2) NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    INDEX idx_order_id (order_id),
    INDEX idx_product_id (product_id)
);
```

**Relationships**:
- One order has many line items (1:N)
- Cascade delete: Deleting order deletes line items

### 9.2 MySQL - Inventory Service

**inventory table**:
```sql
CREATE TABLE inventory (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id VARCHAR(255) UNIQUE NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 0,
    reserved_quantity INTEGER NOT NULL DEFAULT 0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP 
                ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_product_id (product_id),
    CHECK (quantity >= 0),
    CHECK (reserved_quantity >= 0),
    CHECK (reserved_quantity <= quantity)
);
```

**Business Rules**:
- `available = quantity - reserved_quantity`
- `reserved_quantity` cannot exceed `quantity`
- Both values must be non-negative

**Operations**:
```sql
-- Reserve stock
UPDATE inventory 
SET reserved_quantity = reserved_quantity + 3 
WHERE product_id = 'prod-123';

-- Deduct stock (after delivery)
UPDATE inventory 
SET quantity = quantity - 3,
    reserved_quantity = reserved_quantity - 3
WHERE product_id = 'prod-123';

-- Release stock (on cancellation)
UPDATE inventory 
SET reserved_quantity = reserved_quantity - 3
WHERE product_id = 'prod-123';
```

### 9.3 MongoDB - Product Service

**products collection**:
```json
{
    "_id": "prod-123",
    "name": "iPhone 15 Pro",
    "description": "Latest Apple smartphone with A17 Pro chip",
    "price": 999.99,
    "category": "Electronics",
    "brand": "Apple",
    "specifications": {
        "storage": "256GB",
        "color": "Titanium Blue",
        "warranty": "1 year"
    },
    "images": [
        "https://example.com/image1.jpg",
        "https://example.com/image2.jpg"
    ],
    "createdAt": "2024-01-15T10:30:00Z",
    "updatedAt": "2024-01-15T10:30:00Z"
}
```

**Why MongoDB?**
- Flexible schema for product attributes
- Easy to add new fields without migrations
- Fast read operations for product catalog
- No complex joins needed

---

## 10. Deployment & Containerization

### 10.1 Docker Architecture

**18 Containers Total**:

**Infrastructure (8)**:
1. keycloak:8080
2. kafka:9092
3. zookeeper:2181
4. mysql:3307
5. mongodb:27017
6. zipkin:9411
7. prometheus:9090
8. grafana:3000

**Microservices (10)**:
9. eureka-server:8761
10. api-gateway:8081
11. product-service:8082
12. order-service:8083
13. inventory-service:8084
14. notification-service:8085
15. payment-service:8086
16. warehouse-service:8087
17. shipping-service:8088
18. delivery-service:8089

### 10.2 Docker Network

**Network**: ecommerce-network (bridge driver)

**Benefits**:
- All containers can communicate using container names
- Isolated from host network
- DNS resolution built-in

**Example**:
```yaml
# Order Service connects to MySQL
spring.datasource.url=jdbc:mysql://mysql:3306/orderdb

# Order Service connects to Kafka
spring.kafka.bootstrap-servers=kafka:9092

# Order Service registers with Eureka
eureka.client.service-url.defaultZone=http://eureka-server:8761/eureka
```

### 10.3 Dockerfile Example

**order-service/Dockerfile**:
```dockerfile
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/order-service-1.0.0.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Build Process**:
```bash
# Build JAR
mvn clean package

# Build Docker image
docker build -t tejaswini2703/order-service:1.0.0 .

# Push to Docker Hub
docker push tejaswini2703/order-service:1.0.0
```

### 10.4 Docker Compose

**Start Everything**:
```bash
docker-compose up -d
```

**What Happens**:
1. Creates ecommerce-network
2. Starts infrastructure containers (Keycloak, Kafka, MySQL, etc.)
3. Waits for dependencies (depends_on)
4. Starts microservice containers
5. Services register with Eureka
6. System ready in ~2 minutes

**Environment Variables**:
```yaml
environment:
  - SPRING_PROFILES_ACTIVE=docker
```

This activates `application-docker.properties` which uses Docker hostnames.

### 10.5 Volumes

**Persistent Data**:
```yaml
volumes:
  mongo-data:  # MongoDB data persists across restarts
  mysql-data:  # MySQL data persists across restarts
```

**Benefits**:
- Data survives container restarts
- Can backup volumes
- Can migrate data

---

## 11. Monitoring & Observability

### 11.1 Zipkin - Distributed Tracing

**URL**: http://localhost:9411

**Purpose**: Track requests across multiple services

**How it Works**:
1. Client makes request to API Gateway
2. Gateway adds trace ID to request
3. Each service adds span to trace
4. All spans sent to Zipkin
5. Zipkin visualizes complete flow

**Example Trace**:
```
Trace ID: abc123
├─ api-gateway (50ms)
├─ order-service (120ms)
│  ├─ MySQL query (30ms)
│  └─ Kafka publish (10ms)
├─ inventory-service (80ms)
│  └─ MySQL query (40ms)
└─ payment-service (10000ms)
   └─ Payment processing (9950ms)

Total: 10250ms
```

**Benefits**:
- Identify slow services
- Find bottlenecks
- Debug distributed issues
- Understand service dependencies

### 11.2 Prometheus - Metrics

**URL**: http://localhost:9090

**Purpose**: Collect and store time-series metrics

**Metrics Collected**:
- HTTP request count
- HTTP request duration
- JVM memory usage
- CPU usage
- Database connection pool
- Kafka consumer lag

**Example Queries**:
```promql
# Request rate per service
rate(http_server_requests_seconds_count{service="order-service"}[5m])

# 95th percentile response time
histogram_quantile(0.95, http_server_requests_seconds_bucket)

# Memory usage
jvm_memory_used_bytes{service="order-service"}

# Error rate
rate(http_server_requests_seconds_count{status="500"}[5m])
```

**Configuration** (prometheus.yml):
```yaml
scrape_configs:
  - job_name: 'spring-boot'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: 
        - 'order-service:8083'
        - 'product-service:8082'
        - 'inventory-service:8084'
```

### 11.3 Grafana - Dashboards

**URL**: http://localhost:3000 (admin/admin)

**Purpose**: Visualize Prometheus metrics

**Setup**:
1. Add Prometheus datasource: http://prometheus:9090
2. Import dashboard: 11378 (Spring Boot 2.1 Statistics)
3. Create custom dashboards

**Dashboard Panels**:
- Request rate over time (line chart)
- Response time percentiles (graph)
- Error rate (gauge)
- JVM memory usage (area chart)
- Active database connections (stat)
- Order status distribution (pie chart)

**Alerts**:
- High error rate (> 5%)
- Slow response time (> 1s)
- High memory usage (> 80%)
- Service down

### 11.4 Spring Boot Actuator

**Endpoints**:
- `/actuator/health` - Service health status
- `/actuator/metrics` - Available metrics
- `/actuator/prometheus` - Prometheus format metrics
- `/actuator/info` - Application info

**Health Check**:
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "MySQL",
        "validationQuery": "isValid()"
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 500GB,
        "free": 250GB
      }
    },
    "kafka": {
      "status": "UP"
    }
  }
}
```

---

## 12. Testing Guide

### 12.1 Prerequisites

**Start Infrastructure**:
```bash
docker-compose up -d
```

**Verify Services**:
- Keycloak: http://localhost:8080 (admin/admin)
- Eureka: http://localhost:8761 (eureka/eureka123)
- All 10 services should be UP in Eureka

### 12.2 Get Authentication Token

**Request**:
```bash
POST http://localhost:8080/realms/ecommerce/protocol/openid-connect/token
Content-Type: application/x-www-form-urlencoded

grant_type=password
&client_id=backend-service
&client_secret=ecommerce-secret-2024
&username=testuser
&password=test123
```

**Response**:
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expires_in": 300,
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "Bearer"
}
```

**Save**: Copy `access_token` for all subsequent requests

### 12.3 Complete Order Flow Test

**Step 1: Create Product**
```bash
POST http://localhost:8082/api/products
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "name": "Gaming Laptop",
  "description": "High-performance gaming laptop",
  "price": 1299.99
}
```

Response: Save `id` (e.g., "prod-gaming-001")

**Step 2: Create Inventory**
```bash
POST http://localhost:8084/api/inventory
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "productId": "prod-gaming-001",
  "quantity": 50
}
```

Response: Confirms 50 units available

**Step 3: Check Stock**
```bash
GET http://localhost:8084/api/inventory/check?productId=prod-gaming-001&quantity=3
Authorization: Bearer {access_token}
```

Response: `{"available": true}`

**Step 4: Create Order**
```bash
POST http://localhost:8083/api/orders
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "lineItems": [
    {
      "productId": "prod-gaming-001",
      "quantity": 3,
      "price": 1299.99
    }
  ]
}
```

Response: Save `id` (e.g., 1) and note `status: "PENDING"`

**Step 5: Track Order Progress**
```bash
GET http://localhost:8083/api/orders/1
Authorization: Bearer {access_token}
```

Call every 20 seconds and watch status change:
- 0s: PENDING
- 10s: CONFIRMED
- 28s: PROCESSING
- 48s: SHIPPED
- 100s: DELIVERED

**Step 6: Verify Inventory Deduction**
```bash
GET http://localhost:8084/api/inventory/prod-gaming-001
Authorization: Bearer {access_token}
```

Response: `quantity: 47` (50 - 3)

### 12.4 Test Scenarios

**Scenario 1: Idempotency**
- Create order twice with same data
- Should return same order ID
- Inventory deducted only once

**Scenario 2: Out of Stock**
- Create inventory with 5 units
- Try to order 10 units
- Should fail with "Insufficient inventory"

**Scenario 3: Payment Failure**
- Create 20 orders
- ~1 will fail (5% random)
- Failed order status: CANCELLED
- Inventory released

**Scenario 4: Manual Cancellation**
- Create order (PENDING)
- Cancel immediately: Success
- Create order, wait 50s (SHIPPED)
- Try to cancel: Error "Cannot cancel shipped order"

### 12.5 Monitoring During Tests

**Zipkin**:
- Go to http://localhost:9411
- Search for order ID
- View complete trace across services

**Prometheus**:
- Go to http://localhost:9090
- Query: `http_server_requests_seconds_count`
- See request counts per service

**Grafana**:
- Go to http://localhost:3000
- View dashboards
- Monitor real-time metrics

---

## Summary

This e-commerce platform demonstrates:

✅ **Microservices Architecture** - 10 independent services
✅ **Event-Driven Design** - Kafka-based async communication
✅ **Saga Pattern** - Distributed transaction management
✅ **OAuth2 Security** - Keycloak authentication
✅ **Containerization** - Docker & Docker Compose
✅ **Observability** - Zipkin, Prometheus, Grafana
✅ **Resilience** - Circuit breakers, retries, fault tolerance
✅ **Database Per Service** - MySQL & MongoDB
✅ **Service Discovery** - Eureka
✅ **API Gateway** - Single entry point

**Production-Ready Features**:
- Idempotency
- Failure handling
- Compensating transactions
- Health checks
- Distributed tracing
- Metrics collection
- Horizontal scalability

**Total Lines of Code**: ~5000+
**Services**: 10 microservices + 8 infrastructure
**Databases**: 2 (MySQL, MongoDB)
**Message Broker**: Kafka with 8 topics
**Deployment**: Fully containerized with Docker Compose

---

**End of Complete Project Explanation**
