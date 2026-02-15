package com.ecommerce.order.dto;

import lombok.Data;
import java.util.List;

@Data
public class OrderRequest {
    private List<OrderLineItemDto> lineItems;
}
