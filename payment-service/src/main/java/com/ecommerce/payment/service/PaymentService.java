package com.ecommerce.payment.service;

import com.ecommerce.payment.client.OrderClient;
import com.ecommerce.payment.dto.*;
import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.entity.PaymentStatus;
import com.ecommerce.payment.exception.PaymentNotFoundException;
import com.ecommerce.payment.repository.PaymentRepository;
import com.iyzipay.Options;
import com.iyzipay.model.*;
import com.iyzipay.request.CreatePaymentRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final OrderClient orderClient;
    private final Options iyzipayOptions;

    public PaymentService(PaymentRepository paymentRepository, OrderClient orderClient, Options iyzipayOptions) {
        this.paymentRepository = paymentRepository;
        this.orderClient = orderClient;
        this.iyzipayOptions = iyzipayOptions;
    }

    public List<PaymentResponse> getAllPayments() {
        return paymentRepository.findAll().stream().map(this::toResponse).toList();
    }

    public PaymentIntentResponse createPaymentIntent(PaymentIntentRequest request, String idempotencyKey) {
        log.info("Creating payment intent for orderId={}", request == null ? null : request.orderId());
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Payment existing = paymentRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
            if (existing != null) {
                log.info("Returning existing payment intent for idempotencyKey={}", idempotencyKey);
                return new PaymentIntentResponse(existing.getPaymentIntentId(), existing.getOrderId(), existing.getAmount(), existing.getPaymentStatus());
            }
        }

        if (request == null || request.orderId() == null || request.orderId() <= 0 || request.amount() == null || request.amount() <= 0) {
            throw new IllegalArgumentException("orderId and amount must be valid");
        }

        Payment payment = new Payment();
        payment.setOrderId(request.orderId());
        payment.setAmount(request.amount());
        payment.setPaymentDate(LocalDateTime.now());
        payment.setPaymentIntentId(UUID.randomUUID().toString());
        payment.setIdempotencyKey((idempotencyKey == null || idempotencyKey.isBlank()) ? null : idempotencyKey);
        payment.setPaymentStatus(PaymentStatus.PENDING);

        Payment saved = paymentRepository.save(payment);
        return new PaymentIntentResponse(saved.getPaymentIntentId(), saved.getOrderId(), saved.getAmount(), saved.getPaymentStatus());
    }

    public PaymentResponse confirmPaymentIntent(String paymentIntentId, PaymentConfirmRequest request, String idempotencyKey) {
        log.info("Confirming payment intent id={}", paymentIntentId);
        if (paymentIntentId == null || paymentIntentId.isBlank()) {
            throw new IllegalArgumentException("paymentIntentId is required");
        }

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Payment existing = paymentRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
            if (existing != null && existing.getPaymentStatus() == PaymentStatus.SUCCESS) {
                return toResponse(existing);
            }
        }

        Payment payment = paymentRepository.findByPaymentIntentId(paymentIntentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment intent not found"));

        if (payment.getPaymentStatus() == PaymentStatus.SUCCESS) {
            return toResponse(payment);
        }

        PaymentRequest payRequest = new PaymentRequest(
                payment.getOrderId(),
                request != null ? request.cardNumber() : null,
                payment.getAmount()
        );

        return processExistingPayment(payment, payRequest, idempotencyKey);
    }

    public PaymentResponse processPayment(PaymentRequest request) {
        return processPayment(request, null);
    }

    public PaymentResponse processPayment(PaymentRequest request, String idempotencyKey) {
        log.info("Processing payment for orderId={}", request == null ? null : request.orderId());
        validateRequest(request);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Payment existing = paymentRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
            if (existing != null) {
                return toResponse(existing);
            }
        }

        Payment payment = paymentRepository.findByOrderId(request.orderId()).orElse(null);
        if (payment == null) {
            payment = new Payment();
            payment.setOrderId(request.orderId());
            payment.setAmount(request.amount());
            payment.setPaymentDate(LocalDateTime.now());
            payment.setPaymentIntentId(UUID.randomUUID().toString());
        }

        return processExistingPayment(payment, request, idempotencyKey);
    }

    private PaymentResponse processExistingPayment(Payment payment, PaymentRequest request, String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            payment.setIdempotencyKey(idempotencyKey);
        }

        boolean success;
        if (isIyzipayConfigured()) {
            CreatePaymentRequest iyzicoRequest = buildIyzicoPaymentRequest(request);
            com.iyzipay.model.Payment iyzicoPayment = com.iyzipay.model.Payment.create(iyzicoRequest, iyzipayOptions);
            success = iyzicoPayment != null && Status.SUCCESS.getValue().equalsIgnoreCase(iyzicoPayment.getStatus());
        } else {
            // Local fallback: if gateway credentials are missing, simulate a successful payment flow.
            success = true;
        }
        payment.setPaymentStatus(success ? PaymentStatus.SUCCESS : PaymentStatus.FAILED);
        payment.setPaymentDate(LocalDateTime.now());

        Payment saved = paymentRepository.save(payment);

        if (success) {
            log.info("Payment successful for orderId={}, updating order status", saved.getOrderId());
            orderClient.updateOrderStatus(saved.getOrderId(), "PAYMENT_AUTHORIZED");
            orderClient.updateOrderStatus(saved.getOrderId(), "COMPLETED");
        } else {
            log.warn("Payment failed for orderId={}", saved.getOrderId());
        }

        return toResponse(saved);
    }

    private boolean isIyzipayConfigured() {
        return iyzipayOptions != null
                && iyzipayOptions.getApiKey() != null
                && !iyzipayOptions.getApiKey().isBlank()
                && iyzipayOptions.getSecretKey() != null
                && !iyzipayOptions.getSecretKey().isBlank();
    }

    public PaymentResponse getPaymentByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId)
                .map(this::toResponse)
                .orElseThrow(() -> new PaymentNotFoundException(orderId));
    }

    private void validateRequest(PaymentRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Payment request cannot be empty");
        }
        if (request.orderId() == null || request.orderId() <= 0) {
            throw new IllegalArgumentException("orderId must be greater than zero");
        }
        if (request.amount() == null || request.amount() <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }
        if (request.cardNumber() == null || request.cardNumber().isBlank()) {
            throw new IllegalArgumentException("cardNumber is required");
        }
    }

    private CreatePaymentRequest buildIyzicoPaymentRequest(PaymentRequest request) {
        BigDecimal amount = BigDecimal.valueOf(request.amount());

        CreatePaymentRequest iyzicoRequest = new CreatePaymentRequest();
        iyzicoRequest.setLocale(Locale.TR.getValue());
        iyzicoRequest.setConversationId(String.valueOf(request.orderId()));
        iyzicoRequest.setPrice(amount);
        iyzicoRequest.setPaidPrice(amount);
        iyzicoRequest.setCurrency(Currency.TRY.name());
        iyzicoRequest.setInstallment(1);
        iyzicoRequest.setBasketId("BASKET-" + request.orderId());
        iyzicoRequest.setPaymentChannel(PaymentChannel.WEB.name());
        iyzicoRequest.setPaymentGroup(PaymentGroup.PRODUCT.name());
        iyzicoRequest.setPaymentCard(buildTestCard(request.cardNumber()));
        iyzicoRequest.setBuyer(buildDummyBuyer(request.orderId()));
        iyzicoRequest.setShippingAddress(buildDummyAddress("Teslimat Musterisi"));
        iyzicoRequest.setBillingAddress(buildDummyAddress("Fatura Musterisi"));
        iyzicoRequest.setBasketItems(buildDummyBasketItems(request.orderId(), amount));
        return iyzicoRequest;
    }

    private PaymentCard buildTestCard(String cardNumber) {
        PaymentCard paymentCard = new PaymentCard();
        paymentCard.setCardHolderName("John Doe");
        paymentCard.setCardNumber(cardNumber);
        paymentCard.setExpireMonth("12");
        paymentCard.setExpireYear("2028");
        paymentCard.setCvc("000");
        paymentCard.setRegisterCard(0);
        return paymentCard;
    }

    private Buyer buildDummyBuyer(Long orderId) {
        Buyer buyer = new Buyer();
        buyer.setId("BY-" + orderId);
        buyer.setName("Test");
        buyer.setSurname("User");
        buyer.setGsmNumber("+905350000000");
        buyer.setEmail("test.user@example.com");
        buyer.setIdentityNumber("74300864791");
        buyer.setLastLoginDate("2024-01-01 10:00:00");
        buyer.setRegistrationDate("2024-01-01 09:00:00");
        buyer.setRegistrationAddress("Maslak Mahallesi, Istanbul");
        buyer.setIp("127.0.0.1");
        buyer.setCity("Istanbul");
        buyer.setCountry("Turkey");
        buyer.setZipCode("34485");
        return buyer;
    }

    private Address buildDummyAddress(String contactName) {
        Address address = new Address();
        address.setContactName(contactName);
        address.setCity("Istanbul");
        address.setCountry("Turkey");
        address.setAddress("Maslak Mahallesi, Buyukdere Caddesi No:1");
        address.setZipCode("34485");
        return address;
    }

    private List<BasketItem> buildDummyBasketItems(Long orderId, BigDecimal amount) {
        BasketItem basketItem = new BasketItem();
        basketItem.setId("BI-" + orderId);
        basketItem.setName("Order Payment");
        basketItem.setCategory1("Marketplace");
        basketItem.setCategory2("General");
        basketItem.setItemType(BasketItemType.PHYSICAL.name());
        basketItem.setPrice(amount);

        List<BasketItem> basketItems = new ArrayList<>();
        basketItems.add(basketItem);
        return basketItems;
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getPaymentIntentId(),
                payment.getIdempotencyKey(),
                payment.getAmount(),
                payment.getPaymentDate(),
                payment.getPaymentStatus()
        );
    }
}
