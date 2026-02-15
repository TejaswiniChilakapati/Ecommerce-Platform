package com.ecommerce.payment.consumer;

import com.ecommerce.payment.event.OrderPlacedEvent;
import com.ecommerce.payment.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentConsumer {
    private final PaymentService paymentService;
    private final Map<Long, Boolean> cancelledOrders = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "order-payment-events", groupId = "payment-group")
    public void handleOrderPlaced(String message) {
        try {
            OrderPlacedEvent event = objectMapper.readValue(message, OrderPlacedEvent.class);
            if (cancelledOrders.containsKey(event.getOrderId())) {
                log.info("Skipping payment for cancelled order: {}", event.getOrderId());
                return;
            }
            log.info("Received order for payment: {}", event.getOrderId());
            paymentService.processPayment(event, cancelledOrders);
        } catch (Exception e) {
            log.error("Failed to process payment event: {}", e.getMessage(), e);
        }
    }
    
    @KafkaListener(topics = "order-status-events", groupId = "payment-cancellation-group")
    public void handleOrderStatusChange(String message) {
        try {
            Map<String, Object> event = objectMapper.readValue(message, Map.class);
            Long orderId = ((Number) event.get("orderId")).longValue();
            String newStatus = (String) event.get("newStatus");
            
            if ("CANCELLED".equals(newStatus)) {
                cancelledOrders.put(orderId, true);
                log.info("Order {} marked as cancelled in payment service", orderId);
            }
        } catch (Exception e) {
            log.error("Error handling order status change: {}", e.getMessage());
        }
    }
}
