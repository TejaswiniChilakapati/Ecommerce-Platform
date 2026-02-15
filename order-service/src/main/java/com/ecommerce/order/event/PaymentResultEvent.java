package com.ecommerce.order.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentResultEvent {
    private Long orderId;
    private String status;
    private String transactionId;
    private String message;
}
