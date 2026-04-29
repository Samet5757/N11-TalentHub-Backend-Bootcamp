package com.ecommerce.payment.service;

import com.ecommerce.payment.client.OrderClient;
import com.ecommerce.payment.dto.PaymentRequest;
import com.ecommerce.payment.dto.PaymentResponse;
import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.entity.PaymentStatus;
import com.ecommerce.payment.exception.PaymentNotFoundException;
import com.ecommerce.payment.repository.PaymentRepository;
import com.iyzipay.Options;
import com.iyzipay.model.Address;
import com.iyzipay.model.BasketItem;
import com.iyzipay.model.BasketItemType;
import com.iyzipay.model.Buyer;
import com.iyzipay.model.Currency;
import com.iyzipay.model.Locale;
import com.iyzipay.model.PaymentCard;
import com.iyzipay.model.PaymentChannel;
import com.iyzipay.model.PaymentGroup;
import com.iyzipay.model.Status;
import com.iyzipay.request.CreatePaymentRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class PaymentService {

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

    public PaymentResponse processPayment(PaymentRequest request) {
        validateRequest(request);

        Payment payment = new Payment();
        payment.setOrderId(request.orderId());
        payment.setAmount(request.amount());
        payment.setPaymentDate(LocalDateTime.now());

        CreatePaymentRequest iyzicoRequest = buildIyzicoPaymentRequest(request);
        System.out.println("KULLANILAN IYZICO URL: " + iyzipayOptions.getBaseUrl());
        System.out.println("KULLANILAN API KEY: " + maskValue(iyzipayOptions.getApiKey()));
        com.iyzipay.model.Payment iyzicoPayment = com.iyzipay.model.Payment.create(iyzicoRequest, iyzipayOptions);
        boolean success = iyzicoPayment != null && Status.SUCCESS.getValue().equalsIgnoreCase(iyzicoPayment.getStatus());
        if (!success && iyzicoPayment != null) {
            System.err.println("Iyzico Hata Mesajı: " + iyzicoPayment.getErrorMessage());
        }
        payment.setPaymentStatus(success ? PaymentStatus.SUCCESS : PaymentStatus.FAILED);

        Payment saved = paymentRepository.save(payment);

        if (success) {
            orderClient.updateOrderStatus(saved.getOrderId(), "PAYMENT_AUTHORIZED");
        }

        return toResponse(saved);
    }

    public PaymentResponse getPaymentByOrderId(Long orderId) {
        return paymentRepository.findAll().stream()
                .filter(p -> p.getOrderId().equals(orderId))
                .findFirst()
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
        iyzicoRequest.setPaymentCard(buildTestCard());
        iyzicoRequest.setBuyer(buildDummyBuyer(request.orderId()));
        iyzicoRequest.setShippingAddress(buildDummyAddress("Teslimat Musterisi"));
        iyzicoRequest.setBillingAddress(buildDummyAddress("Fatura Musterisi"));
        iyzicoRequest.setBasketItems(buildDummyBasketItems(request.orderId(), amount));
        return iyzicoRequest;
    }

    private PaymentCard buildTestCard() {
        PaymentCard paymentCard = new PaymentCard();
        paymentCard.setCardHolderName("John Doe");
        paymentCard.setCardNumber("5890040000000016");
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
                payment.getAmount(),
                payment.getPaymentDate(),
                payment.getPaymentStatus()
        );
    }

    private String maskValue(String value) {
        if (value == null || value.length() < 8) {
            return "****";
        }
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }
}
