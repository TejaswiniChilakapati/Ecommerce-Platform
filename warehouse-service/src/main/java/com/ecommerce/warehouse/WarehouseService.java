package com.ecommerce.warehouse;

import com.ecommerce.warehouse.event.OrderStatusEvent;
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
public class WarehouseService {
    private final KafkaTemplate<String, Map<String, Object>> kafkaTemplate;
    private final Map<Long, Boolean> cancelledOrders = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "order-status-events", groupId = "warehouse-group")
    public void handleOrderConfirmed(String message) {
        try {
            OrderStatusEvent event = objectMapper.readValue(message, OrderStatusEvent.class);
            if ("CONFIRMED".equals(event.getNewStatus().toString())) {
                if (cancelledOrders.containsKey(event.getOrderId())) {
                    log.info("Skipping warehouse for cancelled order: {}", event.getOrderId());
                    return;
                }
                processWarehouse(event.getOrderId());
            } else if ("CANCELLED".equals(event.getNewStatus().toString())) {
                cancelledOrders.put(event.getOrderId(), true);
                log.info("Order {} marked as cancelled in warehouse service", event.getOrderId());
            }
        } catch (Exception e) {
            log.error("Error processing order status: {}", e.getMessage());
        }
    }

    @Async
    public void processWarehouse(Long orderId) {
        CompletableFuture.delayedExecutor(15, TimeUnit.SECONDS).execute(() -> {
            if (cancelledOrders.containsKey(orderId)) {
                log.info("Warehouse cancelled for order: {}", orderId);
                return;
            }
            
            log.info("Processing warehouse for order: {}", orderId);
            boolean success = Math.random() > 0.05;
            
            Map<String, Object> warehouseEvent = new HashMap<>();
            warehouseEvent.put("orderId", orderId);
            warehouseEvent.put("status", success ? "COMPLETED" : "FAILED");
            warehouseEvent.put("message", success ? "Items picked" : "Item unavailable");
            
            kafkaTemplate.send("warehouse-events", warehouseEvent);
            log.info(success ? "Warehouse completed: {}" : "Warehouse failed: {}", orderId);
        });
    }
}
