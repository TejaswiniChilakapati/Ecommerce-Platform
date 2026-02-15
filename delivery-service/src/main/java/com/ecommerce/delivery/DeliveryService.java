package com.ecommerce.delivery;

import com.ecommerce.delivery.event.OrderStatusEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryService {
    private final KafkaTemplate<String, Map<String, Object>> kafkaTemplate;
    private final Map<Long, Boolean> cancelledOrders = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "order-status-events", groupId = "delivery-group")
    public void handleShippingCompleted(String message) {
        try {
            OrderStatusEvent event = objectMapper.readValue(message, OrderStatusEvent.class);
            if ("SHIPPED".equals(event.getNewStatus().toString())) {
                if (cancelledOrders.containsKey(event.getOrderId())) {
                    log.info("Skipping delivery for cancelled order: {}", event.getOrderId());
                    return;
                }
                processDelivery(event.getOrderId());
            } else if ("CANCELLED".equals(event.getNewStatus().toString())) {
                cancelledOrders.put(event.getOrderId(), true);
                log.info("Order {} marked as cancelled in delivery service", event.getOrderId());
            }
        } catch (Exception e) {
            log.error("Failed to handle shipping event: {}", e.getMessage(), e);
        }
    }

    @Async
    public void processDelivery(Long orderId) {
        CompletableFuture.delayedExecutor(25, TimeUnit.SECONDS).execute(() -> {
            if (cancelledOrders.containsKey(orderId)) {
                log.info("Delivery cancelled for order: {}", orderId);
                return;
            }
            
            log.info("Delivering order: {}", orderId);
            Map<String, Object> event = new HashMap<>();
            event.put("orderId", orderId);
            event.put("status", "DELIVERED");
            kafkaTemplate.send("delivery-events", event);
            log.info("Delivered: {}", orderId);
        });
    }
}
