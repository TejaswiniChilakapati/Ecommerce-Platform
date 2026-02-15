package com.ecommerce.payment.service;

import com.ecommerce.payment.event.OrderPlacedEvent;
import com.ecommerce.payment.event.PaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {
    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    @Async
    public void processPayment(OrderPlacedEvent event, Map<Long, Boolean> cancelledOrders) {
        CompletableFuture.delayedExecutor(10, TimeUnit.SECONDS).execute(() -> {
            try {
                if (cancelledOrders.containsKey(event.getOrderId())) {
                    log.info("Payment cancelled for order: {}", event.getOrderId());
                    return;
                }
                
                log.info("Processing payment for order: {}", event.getOrderId());
                
                boolean paymentSuccess = simulatePaymentGateway(event);
                
                if (paymentSuccess) {
                    String transactionId = UUID.randomUUID().toString();
                    PaymentEvent paymentEvent = new PaymentEvent(
                        event.getOrderId(),
                        "COMPLETED",
                        transactionId,
                        "Payment successful"
                    );
                    kafkaTemplate.send("payment-events", paymentEvent);
                    log.info("Payment completed for order: {}", event.getOrderId());
                } else {
                    PaymentEvent paymentEvent = new PaymentEvent(
                        event.getOrderId(),
                        "FAILED",
                        null,
                        "Payment gateway declined"
                    );
                    kafkaTemplate.send("payment-events", paymentEvent);
                    log.warn("Payment declined for order: {}", event.getOrderId());
                }
            } catch (Exception e) {
                log.error("Payment processing error for order {}: {}", event.getOrderId(), e.getMessage(), e);
                PaymentEvent paymentEvent = new PaymentEvent(
                    event.getOrderId(),
                    "FAILED",
                    null,
                    "Payment gateway timeout: " + e.getClass().getSimpleName()
                );
                kafkaTemplate.send("payment-events", paymentEvent);
            }
        });
    }
    
    private boolean simulatePaymentGateway(OrderPlacedEvent event) {
        return secureRandom.nextDouble() > 0.05;
    }
}
