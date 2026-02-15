package com.ecommerce.order.dto;

import lombok.Data;

@Data
public class InventoryResponse {
    private String productId;
    private boolean inStock;
}
