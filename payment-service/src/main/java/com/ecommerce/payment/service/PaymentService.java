package com.ecommerce.payment.service;
import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import java.util.List;
@Service
public class PaymentService {
    private final PaymentRepository paymentRepository;
    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }
    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }
    public Payment processPayment(Payment payment) {
        return paymentRepository.save(payment);
    }
    public Payment getPaymentByOrderId(Long orderId) {
        return paymentRepository.findAll().stream()
                .filter(p -> p.getOrderId().equals(orderId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Payment not found for order id: " + orderId));
    }
}
