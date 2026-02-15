package com.ecommerce.order.service;

import com.ecommerce.order.enums.OrderStatus;
import com.ecommerce.order.enums.PaymentStatus;
import com.ecommerce.order.event.OrderStatusEvent;
import com.ecommerce.order.event.PaymentResultEvent;
import com.ecommerce.order.model.Order;
import com.ecommerce.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventListener {
    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, Map<String, Object>> inventoryKafkaTemplate;
    private final KafkaTemplate<String, OrderStatusEvent> statusKafkaTemplate;

    @KafkaListener(topics = "payment-events", groupId = "order-payment-group")
    public void handlePaymentEvent(PaymentResultEvent event) {
        Long orderId = event.getOrderId();
        String status = event.getStatus();
        
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order != null) {
            if ("COMPLETED".equals(status)) {
                order.setPaymentStatus(PaymentStatus.COMPLETED);
                order.setStatus(OrderStatus.CONFIRMED);
                order.setConfirmedAt(LocalDateTime.now());
                order.setPaymentTransactionId(event.getTransactionId());
                orderRepository.save(order);
                statusKafkaTemplate.send("order-status-events", 
                    new OrderStatusEvent(orderId, OrderStatus.PENDING, OrderStatus.CONFIRMED, "Payment completed"));
                log.info("Order {} confirmed", orderId);
            } else {
                order.setPaymentStatus(PaymentStatus.FAILED);
                order.setStatus(OrderStatus.CANCELLED);
                order.setFailureReason(event.getMessage());
                order.setCancelledAt(LocalDateTime.now());
                orderRepository.save(order);
                
                order.getLineItems().forEach(item -> {
                    Map<String, Object> releaseEvent = new HashMap<>();
                    releaseEvent.put("orderId", orderId);
                    releaseEvent.put("productId", item.getProductId());
                    releaseEvent.put("quantity", item.getQuantity());
                    releaseEvent.put("action", "RELEASE");
                    inventoryKafkaTemplate.send("inventory-reservation-events", releaseEvent);
                });
                log.warn("Order {} cancelled - payment failed", orderId);
            }
        }
    }

    @KafkaListener(topics = "warehouse-events", groupId = "order-warehouse-group")
    public void handleWarehouseEvent(Map<String, Object> event) {
        Long orderId = ((Number) event.get("orderId")).longValue();
        String status = (String) event.get("status");
        
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order != null) {
            if ("COMPLETED".equals(status)) {
                order.setStatus(OrderStatus.PROCESSING);
                order.setProcessingAt(LocalDateTime.now());
                orderRepository.save(order);
                statusKafkaTemplate.send("order-status-events", 
                    new OrderStatusEvent(orderId, OrderStatus.CONFIRMED, OrderStatus.PROCESSING, "Items picked"));
                log.info("Order {} processing", orderId);
            } else {
                order.setStatus(OrderStatus.CANCELLED);
                order.setFailureReason((String) event.get("message"));
                order.setCancelledAt(LocalDateTime.now());
                orderRepository.save(order);
                
                order.getLineItems().forEach(item -> {
                    Map<String, Object> releaseEvent = new HashMap<>();
                    releaseEvent.put("orderId", orderId);
                    releaseEvent.put("productId", item.getProductId());
                    releaseEvent.put("quantity", item.getQuantity());
                    releaseEvent.put("action", "RELEASE");
                    inventoryKafkaTemplate.send("inventory-reservation-events", releaseEvent);
                });
                log.warn("Order {} cancelled - warehouse failed", orderId);
            }
        }
    }

    @KafkaListener(topics = "shipping-events", groupId = "order-shipping-group")
    public void handleShippingEvent(Map<String, Object> event) {
        Long orderId = ((Number) event.get("orderId")).longValue();
        String status = (String) event.get("status");
        
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order != null) {
            if ("COMPLETED".equals(status)) {
                order.setStatus(OrderStatus.SHIPPED);
                order.setShippedAt(LocalDateTime.now());
                orderRepository.save(order);
                statusKafkaTemplate.send("order-status-events", 
                    new OrderStatusEvent(orderId, OrderStatus.PROCESSING, OrderStatus.SHIPPED, "Order shipped"));
                log.info("Order {} shipped", orderId);
            } else {
                order.setRequiresManualIntervention(true);
                order.setFailureReason((String) event.get("message"));
                orderRepository.save(order);
                log.warn("Order {} shipping failed - manual intervention required", orderId);
            }
        }
    }

    @KafkaListener(topics = "delivery-events", groupId = "order-delivery-group")
    public void handleDeliveryEvent(Map<String, Object> event) {
        Long orderId = ((Number) event.get("orderId")).longValue();
        String status = (String) event.get("status");
        
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order != null) {
            if ("DELIVERED".equals(status)) {
                order.setStatus(OrderStatus.DELIVERED);
                order.setDeliveredAt(LocalDateTime.now());
                orderRepository.save(order);
                statusKafkaTemplate.send("order-status-events", 
                    new OrderStatusEvent(orderId, OrderStatus.SHIPPED, OrderStatus.DELIVERED, "Order delivered"));
                log.info("Order {} delivered", orderId);
                triggerInventoryDeduction(orderId);
            } else {
                order.setRequiresManualIntervention(true);
                order.setFailureReason((String) event.get("message"));
                orderRepository.save(order);
                log.warn("Order {} delivery failed - manual intervention required", orderId);
            }
        }
    }
    
    private void triggerInventoryDeduction(Long orderId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order != null) {
            order.getLineItems().forEach(item -> {
                Map<String, Object> inventoryEvent = new HashMap<>();
                inventoryEvent.put("orderId", orderId);
                inventoryEvent.put("productId", item.getProductId());
                inventoryEvent.put("quantity", item.getQuantity());
                inventoryEvent.put("action", "DEDUCT");
                
                inventoryKafkaTemplate.send("inventory-reservation-events", inventoryEvent);
                log.info("Sent inventory deduction event for product {}", item.getProductId());
            });
        }
    }
}
