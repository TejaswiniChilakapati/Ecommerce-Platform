package com.ecommerce.inventory.consumer;

import com.ecommerce.inventory.event.InventoryReservationEvent;
import com.ecommerce.inventory.event.OrderPlacedEvent;
import com.ecommerce.inventory.service.InventoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryConsumer {
    private final InventoryService service;
    private final KafkaTemplate<String, InventoryReservationEvent> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "order-events", groupId = "inventory-group")
    @Bulkhead(name = "inventoryService")
    @RateLimiter(name = "inventoryService")
    public void handleOrderPlaced(String message) {
        try {
            OrderPlacedEvent event = objectMapper.readValue(message, OrderPlacedEvent.class);
            log.info("Received order event: {}", event);
            boolean reserved = service.reserveStock(event.getProductId(), event.getQuantity());
            
            if (!reserved) {
                log.error("Failed to reserve stock for order {}", event.getOrderId());
            }
        } catch (Exception e) {
            log.error("Error processing order event: {}", e.getMessage());
        }
    }
    
    @KafkaListener(topics = "inventory-reservation-events", groupId = "inventory-reservation-group")
    public void handleInventoryReservation(String message) {
        try {
            Map<String, Object> event = objectMapper.readValue(message, Map.class);
            log.info("Inventory action: {} for order {}", event.get("action"), event.get("orderId"));
            
            String action = (String) event.get("action");
            String productId = (String) event.get("productId");
            Integer quantity = ((Number) event.get("quantity")).intValue();
            
            switch (action) {
                case "DEDUCT":
                    service.deductReservedStock(productId, quantity);
                    break;
                case "RELEASE":
                    service.releaseReservation(productId, quantity);
                    break;
            }
        } catch (Exception e) {
            log.error("Error processing inventory reservation: {}", e.getMessage());
        }
    }
}
