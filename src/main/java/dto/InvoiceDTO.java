package dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class InvoiceDTO {
    private String invoiceNumber;
    private String reservationCode;
    private String clientName;
    private BigDecimal subtotalHt;
    private BigDecimal tvaAmount;
    private BigDecimal totalTtc;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private boolean paid;

    public InvoiceDTO() {}

    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }
    public String getReservationCode() { return reservationCode; }
    public void setReservationCode(String reservationCode) { this.reservationCode = reservationCode; }
    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    public BigDecimal getSubtotalHt() { return subtotalHt; }
    public void setSubtotalHt(BigDecimal subtotalHt) { this.subtotalHt = subtotalHt; }
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
}