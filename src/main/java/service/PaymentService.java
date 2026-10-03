package service;

import exception.PaymentException;
import model.Payment;
import repository.PaymentRepository;

import java.util.List;
import java.util.UUID;

public class PaymentService {
    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public Payment getById(UUID id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentException("Payment not found"));
    }

    public Payment getByReservation(UUID reservationId) {
        return paymentRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new PaymentException("No payment for reservation"));
    }

    public List<Payment> getAll() {
        return paymentRepository.findAll();
    }
}