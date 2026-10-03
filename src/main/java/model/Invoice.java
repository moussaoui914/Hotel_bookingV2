package model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class Invoice {
    private UUID id;
    private String invoiceNumber;
    private UUID reservationId;
    private UUID paymentId;
    private BigDecimal subtotalHt;
    private BigDecimal tvaRate;
    private BigDecimal tvaAmount;
    private BigDecimal totalTtc;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private boolean paid;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Invoice() {
        this.id = UUID.randomUUID();
        this.tvaRate = new BigDecimal("20.00");
        this.issueDate = LocalDate.now();
        this.dueDate = LocalDate.now().plusDays(30);
        this.paid = false;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Invoice(UUID reservationId, UUID paymentId, BigDecimal subtotalHt) {
        this();
        this.reservationId = reservationId;
        this.paymentId = paymentId;
        this.subtotalHt = subtotalHt;
        calculateTotals();
    }

    public void calculateTotals() {
        if (subtotalHt != null) {
            this.tvaAmount = subtotalHt.multiply(tvaRate).divide(new BigDecimal("100"));
            this.totalTtc = subtotalHt.add(tvaAmount);
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }
    public UUID getReservationId() { return reservationId; }
    public void setReservationId(UUID reservationId) { this.reservationId = reservationId; }
    public UUID getPaymentId() { return paymentId; }
    public void setPaymentId(UUID paymentId) { this.paymentId = paymentId; }
    public BigDecimal getSubtotalHt() { return subtotalHt; }
    public void setSubtotalHt(BigDecimal subtotalHt) { this.subtotalHt = subtotalHt; calculateTotals(); }
    public BigDecimal getTvaRate() { return tvaRate; }
    public void setTvaRate(BigDecimal tvaRate) { this.tvaRate = tvaRate; calculateTotals(); }
    public BigDecimal getTvaAmount() { return tvaAmount; }
    public void setTvaAmount(BigDecimal tvaAmount) { this.tvaAmount = tvaAmount; }
    public BigDecimal getTotalTtc() { return totalTtc; }
    public void setTotalTtc(BigDecimal totalTtc) { this.totalTtc = totalTtc; }
    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public boolean isPaid() { return paid; }
    public void setPaid(boolean paid) { this.paid = paid; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}