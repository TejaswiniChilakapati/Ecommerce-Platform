package com.ecommerce.inventory.dto;

import lombok.Data;

@Data
public class InventoryRequest {
    private String productId;
    private Integer quantity;
}
