package com.ecommerce.notification.consumer;

import com.ecommerce.notification.event.OrderPlacedEvent;
import com.ecommerce.notification.event.OrderStatusEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class NotificationConsumer {
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @KafkaListener(topics = "order-events", groupId = "notification-group")
    public void consumeOrderPlaced(String message) {
        try {
            OrderPlacedEvent event = objectMapper.readValue(message, OrderPlacedEvent.class);
            log.info("ORDER PLACED - Order ID: {}, Product: {}, Quantity: {}", 
                event.getOrderId(), event.getProductId(), event.getQuantity());
        } catch (Exception e) {
            log.error("Error processing order placed event: {}", e.getMessage());
        }
    }
    
    @KafkaListener(topics = "order-status-events", groupId = "notification-status-group")
    public void consumeStatusChange(String message) {
        try {
            Map<String, Object> event = objectMapper.readValue(message, Map.class);
            log.info("STATUS UPDATE - Order #{}: {} -> {} | {}", 
                event.get("orderId"), event.get("oldStatus"), event.get("newStatus"), 
                event.getOrDefault("message", "Status changed"));
        } catch (Exception e) {
            log.error("Error processing status change: {}", e.getMessage());
        }
    }
}
