package com.ecommerce.inventory.controller;

import com.ecommerce.inventory.dto.InventoryRequest;
import com.ecommerce.inventory.dto.InventoryResponse;
import com.ecommerce.inventory.model.Inventory;
import com.ecommerce.inventory.repository.InventoryRepository;
import com.ecommerce.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {
    private final InventoryService service;
    private final InventoryRepository repository;

    @GetMapping("/{productId}")
    public Inventory getByProductId(@PathVariable String productId) {
        return repository.findByProductId(productId)
            .orElse(null);
    }

    @GetMapping
    public List<Inventory> getAll() {
        return repository.findAll();
    }

    /**
     * Check if requested quantity is available for a product.
     * Example: /api/inventory/check?productId=123&quantity=10
     */
    @GetMapping("/check")
    public InventoryResponse checkStock(
            @RequestParam String productId,
            @RequestParam Integer quantity) {
        return service.checkStock(productId, quantity);
    }

    @PostMapping
    public Inventory create(@RequestBody InventoryRequest request) {
        return service.createInventory(request);
    }
}