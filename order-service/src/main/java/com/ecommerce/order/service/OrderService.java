package com.ecommerce.order.service;

import com.ecommerce.order.dto.OrderItemRequest;
import com.ecommerce.order.dto.OrderItemResponse;
import com.ecommerce.order.dto.OrderRequest;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderItem;
import com.ecommerce.order.entity.OrderStatus;
import com.ecommerce.order.exception.OrderNotFoundException;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.order.saga.OrderSagaPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderSagaPublisher orderSagaPublisher;

    public OrderService(OrderRepository orderRepository, OrderSagaPublisher orderSagaPublisher) {
        this.orderRepository = orderRepository;
        this.orderSagaPublisher = orderSagaPublisher;
    }

    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
        return toResponse(order);
    }

    public OrderResponse createOrder(OrderRequest request) {
        validateCreateRequest(request);

        double totalAmount = request.totalAmount();
        double discountAmount = request.discountAmount() == null ? 0.0 : request.discountAmount();
        double finalAmount = Math.max(0.0, totalAmount - discountAmount);

        Order order = new Order();
        order.setCustomerId(request.customerId());
        order.setSellerId(request.sellerId());
        order.setTotalAmount(totalAmount);
        order.setDiscountAmount(discountAmount);
        order.setFinalAmount(finalAmount);
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());

        List<OrderItem> mappedItems = mapItems(order, request.items());
        order.getItems().clear();
        order.getItems().addAll(mappedItems);

        Order saved = orderRepository.save(order);
        orderSagaPublisher.publishOrderCreated(saved);
        return toResponse(saved);
    }

    public OrderResponse updateOrderStatus(Long id, String status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        OrderStatus targetStatus;
        try {
            targetStatus = OrderStatus.valueOf(status.toUpperCase());
        } catch (Exception exception) {
            throw new IllegalArgumentException("Unsupported order status: " + status);
        }

        order.setStatus(targetStatus);
        return toResponse(orderRepository.save(order));
    }

    private List<OrderItem> mapItems(Order order, List<OrderItemRequest> itemRequests) {
        if (itemRequests == null) {
            return List.of();
        }

        List<OrderItem> items = new java.util.ArrayList<>();
        for (OrderItemRequest itemRequest : itemRequests) {
            if (itemRequest == null) {
                continue;
            }
            if (itemRequest.productId() == null || itemRequest.productId() <= 0) {
                throw new IllegalArgumentException("Item productId must be greater than zero");
            }
            if (itemRequest.quantity() == null || itemRequest.quantity() <= 0) {
                throw new IllegalArgumentException("Item quantity must be greater than zero");
            }
            if (itemRequest.unitPrice() == null || itemRequest.unitPrice() <= 0) {
                throw new IllegalArgumentException("Item unitPrice must be greater than zero");
            }

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProductId(itemRequest.productId());
            item.setQuantity(itemRequest.quantity());
            item.setUnitPrice(itemRequest.unitPrice());
            items.add(item);
        }

        return items;
    }

    private void validateCreateRequest(OrderRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Order request cannot be empty");
        }
        if (request.customerId() == null || request.customerId() <= 0) {
            throw new IllegalArgumentException("customerId must be greater than zero");
        }
        if (request.sellerId() == null || request.sellerId() <= 0) {
            throw new IllegalArgumentException("sellerId must be greater than zero");
        }
        if (request.totalAmount() == null || request.totalAmount() < 0) {
            throw new IllegalArgumentException("totalAmount must be zero or greater");
        }
        if (request.discountAmount() != null && request.discountAmount() < 0) {
            throw new IllegalArgumentException("discountAmount cannot be negative");
        }
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getId(),
                        item.getOrder() != null ? item.getOrder().getId() : null,
                        item.getProductId(),
                        item.getQuantity(),
                        item.getUnitPrice()
                ))
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getSellerId(),
                order.getTotalAmount(),
                order.getDiscountAmount(),
                order.getFinalAmount(),
                order.getStatus(),
                order.getCreatedAt(),
                itemResponses
        );
    }
}
