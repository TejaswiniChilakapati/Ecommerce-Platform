package com.ecommerce.inventory.service;

import com.ecommerce.inventory.dto.InventoryRequest;
import com.ecommerce.inventory.dto.InventoryResponse;
import com.ecommerce.inventory.model.Inventory;
import com.ecommerce.inventory.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {
    private final InventoryRepository repository;

    public Inventory createInventory(InventoryRequest request) {
        Inventory inventory = new Inventory();
        inventory.setProductId(request.getProductId());
        inventory.setQuantity(request.getQuantity());
        inventory.setReservedQuantity(0);
        return repository.save(inventory);
    }

    /**
     * Check if requested quantity is available for a product.
     */
    public InventoryResponse checkStock(String productId, Integer requestedQuantity) {
        return repository.findByProductId(productId)
            .map(inv -> new InventoryResponse(productId, inv.getAvailableQuantity() >= requestedQuantity))
            .orElse(new InventoryResponse(productId, false));
    }

    @Transactional
    public boolean reserveStock(String productId, Integer quantity) {
        return repository.findByProductId(productId)
            .map(inventory -> {
                if (inventory.getAvailableQuantity() >= quantity) {
                    inventory.setReservedQuantity(inventory.getReservedQuantity() + quantity);
                    repository.save(inventory);
                    log.info("Reserved {} units of product {}", quantity, productId);
                    return true;
                }
                log.warn("Insufficient stock for product {}", productId);
                return false;
            })
            .orElse(false);
    }

    @Transactional
    public void deductReservedStock(String productId, Integer quantity) {
        repository.findByProductId(productId).ifPresent(inventory -> {
            if (inventory.getReservedQuantity() >= quantity) {
                inventory.setQuantity(inventory.getQuantity() - quantity);
                inventory.setReservedQuantity(inventory.getReservedQuantity() - quantity);
                repository.save(inventory);
                log.info("Deducted {} units of product {}. Available: {}",
                    quantity, productId, inventory.getAvailableQuantity());
            } else {
                log.warn("Attempted to deduct more than reserved for product {}", productId);
            }
        });
    }

    @Transactional
    public void releaseReservation(String productId, Integer quantity) {
        repository.findByProductId(productId).ifPresent(inventory -> {
            if (inventory.getReservedQuantity() >= quantity) {
                inventory.setReservedQuantity(inventory.getReservedQuantity() - quantity);
                repository.save(inventory);
                log.info("Released {} units of product {}", quantity, productId);
            } else {
                log.warn("Attempted to release more than reserved for product {}", productId);
            }
        });
    }
}