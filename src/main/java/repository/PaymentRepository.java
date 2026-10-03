package repository;

import model.Payment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository {
    void save(Payment payment);
    void update(Payment payment);
    Optional<Payment> findById(UUID id);
    Optional<Payment> findByReservationId(UUID reservationId);
    List<Payment> findAll();
    void deleteById(UUID id);
}