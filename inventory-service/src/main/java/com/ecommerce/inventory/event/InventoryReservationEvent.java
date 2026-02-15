package com.ecommerce.inventory.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InventoryReservationEvent {
    private Long orderId;
    private String productId;
    private Integer quantity;
    private String action; // RESERVE, RELEASE, DEDUCT
}
