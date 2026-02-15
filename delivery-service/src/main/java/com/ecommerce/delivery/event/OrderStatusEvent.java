package com.ecommerce.delivery.event;

import com.ecommerce.delivery.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderStatusEvent {
    private Long orderId;
    private OrderStatus oldStatus;
    private OrderStatus newStatus;
    private String message;
}
