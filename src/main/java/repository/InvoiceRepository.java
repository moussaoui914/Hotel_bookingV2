package repository;

import model.Invoice;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository {
    void save(Invoice invoice);
    void update(Invoice invoice);
    Optional<Invoice> findById(UUID id);
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
    Optional<Invoice> findByReservationId(UUID reservationId);
    List<Invoice> findAll();
    List<Invoice> findUnpaidInvoices();
    void deleteById(UUID id);
}