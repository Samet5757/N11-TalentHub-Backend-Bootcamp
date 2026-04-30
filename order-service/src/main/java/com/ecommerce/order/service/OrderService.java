package com.ecommerce.order.service;

import com.ecommerce.order.dto.OrderItemRequest;
import com.ecommerce.order.dto.OrderItemResponse;
import com.ecommerce.order.dto.OrderRequest;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderItem;
import com.ecommerce.order.entity.OrderStatus;
import com.ecommerce.order.exception.OrderNotFoundException;
import com.ecommerce.order.notification.OrderEmailNotificationService;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.order.saga.OrderSagaPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private static final Map<OrderStatus, EnumSet<OrderStatus>> ALLOWED_TRANSITIONS = Map.of(
            OrderStatus.PENDING, EnumSet.of(OrderStatus.INVENTORY_RESERVED, OrderStatus.PAYMENT_AUTHORIZED, OrderStatus.COMPLETED, OrderStatus.FAILED, OrderStatus.CANCELLED),
            OrderStatus.INVENTORY_RESERVED, EnumSet.of(OrderStatus.PAYMENT_PENDING, OrderStatus.PAYMENT_AUTHORIZED, OrderStatus.COMPLETED, OrderStatus.FAILED, OrderStatus.CANCELLED),
            OrderStatus.PAYMENT_PENDING, EnumSet.of(OrderStatus.PAYMENT_AUTHORIZED, OrderStatus.FAILED, OrderStatus.CANCELLED),
            OrderStatus.PAYMENT_AUTHORIZED, EnumSet.of(OrderStatus.APPROVED, OrderStatus.COMPLETED, OrderStatus.SHIPPED, OrderStatus.FAILED, OrderStatus.CANCELLED),
            OrderStatus.APPROVED, EnumSet.of(OrderStatus.SHIPPED, OrderStatus.COMPLETED, OrderStatus.FAILED, OrderStatus.CANCELLED),
            OrderStatus.SHIPPED, EnumSet.of(OrderStatus.DELIVERED, OrderStatus.COMPLETED, OrderStatus.FAILED, OrderStatus.CANCELLED),
            OrderStatus.DELIVERED, EnumSet.of(OrderStatus.COMPLETED),
            OrderStatus.COMPLETED, EnumSet.noneOf(OrderStatus.class),
            OrderStatus.FAILED, EnumSet.of(OrderStatus.CANCELLED, OrderStatus.FAILED),
            OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class)
    );
    private static final Set<OrderStatus> CUSTOMER_CANCELLABLE = EnumSet.of(
            OrderStatus.PENDING,
            OrderStatus.INVENTORY_RESERVED,
            OrderStatus.PAYMENT_PENDING,
            OrderStatus.PAYMENT_AUTHORIZED,
            OrderStatus.APPROVED
    );

    private final OrderRepository orderRepository;
    private final OrderSagaPublisher orderSagaPublisher;
    private final OrderEmailNotificationService orderEmailNotificationService;

    public OrderService(OrderRepository orderRepository,
                        OrderSagaPublisher orderSagaPublisher,
                        OrderEmailNotificationService orderEmailNotificationService) {
        this.orderRepository = orderRepository;
        this.orderSagaPublisher = orderSagaPublisher;
        this.orderEmailNotificationService = orderEmailNotificationService;
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByCustomerId(Long customerId) {
        log.debug("Listing orders for customerId={}", customerId);
        return orderRepository.findByCustomerIdOrderByCreatedAtDescIdDesc(customerId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
        return toResponse(order);
    }

    public OrderResponse createOrder(OrderRequest request) {
        validateCreateRequest(request);
        log.info("Creating order for customerId={}, sellerId={}, itemCount={}",
                request.customerId(), request.sellerId(), request.items() == null ? 0 : request.items().size());

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
        log.info("Order created id={}, finalAmount={}, status={}", saved.getId(), saved.getFinalAmount(), saved.getStatus());
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

        OrderStatus currentStatus = order.getStatus();
        if (currentStatus == targetStatus) {
            return toResponse(order);
        }
        if (!isTransitionAllowed(currentStatus, targetStatus)) {
            log.warn("Ignoring invalid order status transition id={} {} -> {}", id, currentStatus, targetStatus);
            return toResponse(order);
        }

        order.setStatus(targetStatus);
        log.info("Updating order status id={} -> {}", id, targetStatus);
        Order updatedOrder = orderRepository.save(order);
        if (targetStatus == OrderStatus.COMPLETED) {
            orderEmailNotificationService.sendOrderCompletedMail(updatedOrder);
        }
        return toResponse(updatedOrder);
    }

    public OrderResponse cancelOrderByCustomer(Long id, Long customerId) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
        if (!order.getCustomerId().equals(customerId)) {
            throw new IllegalArgumentException("Order does not belong to customer");
        }
        if (!CUSTOMER_CANCELLABLE.contains(order.getStatus())) {
            throw new IllegalStateException("Order cannot be cancelled in current status: " + order.getStatus());
        }
        order.setStatus(OrderStatus.CANCELLED);
        log.info("Customer cancelled order id={}, customerId={}", id, customerId);
        return toResponse(orderRepository.save(order));
    }

    private boolean isTransitionAllowed(OrderStatus from, OrderStatus to) {
        if (from == null || to == null) {
            return false;
        }
        return ALLOWED_TRANSITIONS.getOrDefault(from, EnumSet.noneOf(OrderStatus.class)).contains(to);
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
