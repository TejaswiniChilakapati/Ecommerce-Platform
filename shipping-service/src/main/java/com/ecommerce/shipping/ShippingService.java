package com.ecommerce.shipping;

import com.ecommerce.shipping.event.OrderStatusEvent;
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
public class ShippingService {
    private final KafkaTemplate<String, Map<String, Object>> kafkaTemplate;
    private final Map<Long, Boolean> cancelledOrders = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "order-status-events", groupId = "shipping-group")
    public void handleWarehouseCompleted(String message) {
        try {
            OrderStatusEvent event = objectMapper.readValue(message, OrderStatusEvent.class);
            if ("PROCESSING".equals(event.getNewStatus().toString())) {
                if (cancelledOrders.containsKey(event.getOrderId())) {
                    log.info("Skipping shipping for cancelled order: {}", event.getOrderId());
                    return;
                }
                processShipping(event.getOrderId());
            } else if ("CANCELLED".equals(event.getNewStatus().toString())) {
                cancelledOrders.put(event.getOrderId(), true);
                log.info("Order {} marked as cancelled in shipping service", event.getOrderId());
            }
        } catch (Exception e) {
            log.error("Error processing order status: {}", e.getMessage());
        }
    }

    @Async
    public void processShipping(Long orderId) {
        CompletableFuture.delayedExecutor(20, TimeUnit.SECONDS).execute(() -> {
            if (cancelledOrders.containsKey(orderId)) {
                log.info("Shipping cancelled for order: {}", orderId);
                return;
            }
            
            log.info("Shipping order: {}", orderId);
            Map<String, Object> event = new HashMap<>();
            event.put("orderId", orderId);
            event.put("status", "COMPLETED");
            kafkaTemplate.send("shipping-events", event);
            log.info("Shipped: {}", orderId);
        });
    }
}
