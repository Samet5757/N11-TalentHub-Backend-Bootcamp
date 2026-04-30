package com.ecommerce.product.saga;

import com.ecommerce.common.event.OrderCreatedEvent;
import com.ecommerce.common.event.OrderItemPayload;
import com.ecommerce.product.service.ProductService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryReservationService {

    private final ProductService productService;

    public InventoryReservationService(ProductService productService) {
        this.productService = productService;
    }

    @Transactional
    public void reserveStocksAtomically(OrderCreatedEvent event) {
        for (OrderItemPayload item : event.items()) {
            productService.reserveStock(item.productId(), item.quantity());
        }
    }
}
