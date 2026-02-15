package com.ecommerce.order.service;

import com.ecommerce.order.enums.OrderStatus;
import com.ecommerce.order.enums.PaymentStatus;
import com.ecommerce.order.event.OrderPlacedEvent;
import com.ecommerce.order.event.PaymentEvent;
import com.ecommerce.order.model.Order;
import com.ecommerce.order.repository.OrderRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    private final OrderRepository repository;
    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;
    private final KafkaTemplate<String, PaymentEvent> paymentKafkaTemplate;
    private final KafkaTemplate<String, Map<String, Object>> inventoryKafkaTemplate;

    @CircuitBreaker(name = "orderService", fallbackMethod = "fallbackCreateOrder")
    @Retry(name = "orderService")
    public Order createOrder(Order order) {
        // Idempotency check
        if (order.getIdempotencyKey() != null) {
            Order existing = repository.findByIdempotencyKey(order.getIdempotencyKey());
            if (existing != null) {
                return existing;
            }
        } else {
            order.setIdempotencyKey(UUID.randomUUID().toString());
        }
        
        // Calculate total
        BigDecimal total = order.getLineItems().stream()
            .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalAmount(total);
        
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentStatus(PaymentStatus.PENDING);
        Order saved = repository.save(order);
        
        saved.getLineItems().forEach(item -> 
            kafkaTemplate.send("order-events", new OrderPlacedEvent(saved.getId(), item.getProductId(), item.getQuantity()))
        );
        
        paymentKafkaTemplate.send("order-payment-events", 
            new PaymentEvent(saved.getId(), saved.getTotalAmount(), saved.getIdempotencyKey()));
        
        return saved;
    }

    public Order fallbackCreateOrder(Order order, Exception e) {
        Order failedOrder = new Order();
        failedOrder.setStatus(OrderStatus.CANCELLED);
        failedOrder.setPaymentStatus(PaymentStatus.FAILED);
        failedOrder.setFailureReason("System error: " + e.getMessage());
        return repository.save(failedOrder);
    }
    
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
            order.setFailureReason("Cancellation requested during processing");
            repository.save(order);
            throw new RuntimeException("Order in processing - manual intervention required");
        }
        
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(java.time.LocalDateTime.now());
        repository.save(order);
        
        // Publish cancellation event
        Map<String, Object> cancellationEvent = new HashMap<>();
        cancellationEvent.put("orderId", orderId);
        cancellationEvent.put("newStatus", "CANCELLED");
        cancellationEvent.put("oldStatus", order.getStatus().toString());
        inventoryKafkaTemplate.send("order-status-events", cancellationEvent);
        
        order.getLineItems().forEach(item -> {
            Map<String, Object> releaseEvent = new HashMap<>();
            releaseEvent.put("orderId", orderId);
            releaseEvent.put("productId", item.getProductId());
            releaseEvent.put("quantity", item.getQuantity());
            releaseEvent.put("action", "RELEASE");
            inventoryKafkaTemplate.send("inventory-reservation-events", releaseEvent);
        });
    }
    
    public Page<Order> getAllOrders(Pageable pageable) {
        return repository.findAll(pageable);
    }
    
    public List<Order> getAllOrders() {
        return repository.findAll();
    }
    
    public Order getOrderById(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));
    }
    
    public Order updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = repository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Order not found"));
        
        OrderStatus oldStatus = order.getStatus();
        order.setStatus(newStatus);
        
        LocalDateTime now = LocalDateTime.now();
        switch (newStatus) {
            case CONFIRMED -> {
                order.setConfirmedAt(now);
                log.info("Set confirmedAt to {}", now);
            }
            case PROCESSING -> {
                order.setProcessingAt(now);
                log.info("Set processingAt to {}", now);
            }
            case SHIPPED -> {
                order.setShippedAt(now);
                log.info("Set shippedAt to {}", now);
            }
            case DELIVERED -> {
                order.setDeliveredAt(now);
                log.info("Set deliveredAt to {}", now);
            }
            case CANCELLED -> {
                order.setCancelledAt(now);
                log.info("Set cancelledAt to {}", now);
            }
        }
        
        Order saved = repository.save(order);
        log.info("Order {} status updated from {} to {}", orderId, oldStatus, newStatus);
        return saved;
    }
}
