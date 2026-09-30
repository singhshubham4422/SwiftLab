package com.example.invoiceapp.dto;

import com.example.invoiceapp.model.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PaymentCreateRequest {
    @NotNull(message = "Invoice ID is required")
    private Long invoiceId;

    @NotNull(message = "Payment method is required")
    private PaymentMethod method = PaymentMethod.CASH;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than zero")
    private BigDecimal amount;

    private LocalDate paidOn = LocalDate.now();
    private String reference;
    private String notes;

    public PaymentCreateRequest() {}

    public Long getInvoiceId() { return invoiceId; }
    public void setInvoiceId(Long invoiceId) { this.invoiceId = invoiceId; }
    public PaymentMethod getMethod() { return method; }
    public void setMethod(PaymentMethod method) { this.method = method; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public LocalDate getPaidOn() { return paidOn; }
    public void setPaidOn(LocalDate paidOn) { this.paidOn = paidOn; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
