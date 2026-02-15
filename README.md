# E‑Commerce Microservices Platform

1. Overview
-----------
Event-driven e-commerce system.
Built with Spring Boot microservices + Kafka + Keycloak.
Uses Saga pattern → reliable, consistent, fault tolerant.

---

2. Technology Stack
-------------------
Backend:
- Spring Boot 3.2.x
- Spring Cloud (Eureka, Gateway)
- Spring Kafka
- Spring Data JPA
- Spring Security OAuth2

Databases:
- MySQL 8.0 (Orders, Inventory)
- MongoDB 7.0 (Products)

Messaging:
- Apache Kafka 7.4.4
- Zookeeper 7.4.4

Security:
- Keycloak 24.0 (OAuth2/OpenID Connect)
- Bearer Token Authentication

Observability:
- Zipkin (Distributed Tracing)
- Prometheus (Metrics)
- Grafana (Dashboards)

Resilience:
- Resilience4j (Circuit Breaker, Retry, Bulkhead, Rate Limiter)

Build & Deploy:
- Maven 3.8+
- Docker & Docker Compose
- Java 17+

---

3. Core Components
------------------
Microservices:
- API Gateway → entry point, routes, validates tokens
- Eureka → service discovery
- Product Service → product catalog
- Order Service → saga coordinator
- Inventory Service → reserve/deduct stock
- Payment Service → payment handling (with test failures)
- Warehouse Service → picking/prep
- Shipping Service → courier dispatch
- Delivery Service → mark delivered
- Notification Service → logs + notifications

Infra:
- Keycloak → auth (OAuth2, Bearer)
- Kafka + Zookeeper → event backbone
- MySQL → orders + inventory
- MongoDB → products
- Zipkin → tracing
- Prometheus + Grafana → metrics + dashboards

---

4. Patterns
-----------
- Saga → distributed transactions, rollback on failure
- Event-driven → async via Kafka
- Microservices → independent DB per service
- Reserve-Deduct → inventory consistency
- Security → OAuth2 + Bearer

---

5. Order Flow (Happy Path)
--------------------------
1. Client login via Keycloak 
2. Client → API Gateway (token)
3. Order Service → create order (PENDING)
4. Inventory Service → reserve stock
5. Payment Service → CONFIRMED
6. Warehouse → PROCESSING
7. Shipping → SHIPPED
8. Delivery → DELIVERED
9. Inventory Service → deduct reserved
10. Notification Service → log completion

---

6. Failure Handling
-------------------
- Payment fail → CANCELLED, stock released
- Warehouse/Shipping fail → CANCELLED, stock released
- Manual cancel → allowed before PROCESSING

---

7. Security Flow
----------------
1. Client → Keycloak (login)
2. Keycloak → tokens (access + refresh)
3. Client → Gateway (Bearer token)
4. Gateway validates

---

8. Observability
----------------
- Zipkin → trace
- Prometheus → metrics
- Grafana → dashboards
- Actuator → health endpoints


----

9. Quick Reference
------------------
Order Status → PENDING → CONFIRMED → PROCESSING → SHIPPED → DELIVERED
Inventory Formula → available = quantity - reserved_quantity
Token Expiry → access token 5 min, refresh token 30 min