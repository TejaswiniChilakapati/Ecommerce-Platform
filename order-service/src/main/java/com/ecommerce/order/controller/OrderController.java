package com.ecommerce.order.controller;

import com.ecommerce.order.enums.OrderStatus;
import com.ecommerce.order.model.Order;
import com.ecommerce.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService service;

    @GetMapping
    public List<Order> getAll() {
        return service.getAllOrders();
    }
    
    @GetMapping("/paged")
    public Page<Order> getAllPaged(@PageableDefault(size = 20) Pageable pageable) {
        return service.getAllOrders(pageable);
    }
    
    @GetMapping("/{id}")
    public Order getById(@PathVariable Long id) {
        return service.getOrderById(id);
    }

    @PostMapping
    public Order create(@RequestBody Order order) {
        return service.createOrder(order);
    }
    
    @DeleteMapping("/{id}/cancel")
    public ResponseEntity<String> cancel(@PathVariable Long id) {
        try {
            service.cancelOrder(id);
            return ResponseEntity.ok("Order cancelled successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @PatchMapping("/{id}/status")
    public ResponseEntity<Order> updateStatus(@PathVariable Long id, @RequestParam OrderStatus status) {
        try {
            Order updated = service.updateOrderStatus(id, status);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
