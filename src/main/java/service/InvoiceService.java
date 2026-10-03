package service;

import exception.InvoiceException;
import model.Invoice;
import repository.InvoiceRepository;

import java.util.List;
import java.util.UUID;

public class InvoiceService {
    private final InvoiceRepository invoiceRepository;

    public InvoiceService(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    public Invoice getByNumber(String number) {
        return invoiceRepository.findByInvoiceNumber(number)
                .orElseThrow(() -> new InvoiceException("Invoice not found"));
    }

    public Invoice getByReservation(UUID reservationId) {
        return invoiceRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new InvoiceException("No invoice for reservation"));
    }

    public List<Invoice> getAll() {
        return invoiceRepository.findAll();
    }

    public List<Invoice> getUnpaid() {
        return invoiceRepository.findUnpaidInvoices();
    }
}