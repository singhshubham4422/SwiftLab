package com.example.invoiceapp.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class InvoiceDTO {
    private Long id;
    private String publicId;
    private String invoiceNumber;
    private Long userId;
    private Long customerId;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String customerAddress;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private String paymentTerms;
    private String status;
    private double taxRate = 18.0;
    private double discount = 0.0;
    private double subtotal = 0.0;
    private double taxAmount = 0.0;
    private double total = 0.0;
    private double amountPaid = 0.0;
    private double outstanding = 0.0;
    private String notes;
    private Long saleId;
    private boolean synced = false;
    private List<InvoiceItemDTO> items = new ArrayList<>();

    public InvoiceDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPublicId() { return publicId; }
    public void setPublicId(String publicId) { this.publicId = publicId; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
    public String getCustomerAddress() { return customerAddress; }
    public void setCustomerAddress(String customerAddress) { this.customerAddress = customerAddress; }
    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public String getPaymentTerms() { return paymentTerms; }
    public void setPaymentTerms(String paymentTerms) { this.paymentTerms = paymentTerms; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public double getTaxRate() { return taxRate; }
    public void setTaxRate(double taxRate) { this.taxRate = taxRate; }
    public double getDiscount() { return discount; }
    public void setDiscount(double discount) { this.discount = discount; }
    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }
    public double getTaxAmount() { return taxAmount; }
    public void setTaxAmount(double taxAmount) { this.taxAmount = taxAmount; }
    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }
    public double getAmountPaid() { return amountPaid; }
    public void setAmountPaid(double amountPaid) { this.amountPaid = amountPaid; }
    public double getOutstanding() { return outstanding; }
    public void setOutstanding(double outstanding) { this.outstanding = outstanding; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Long getSaleId() { return saleId; }
    public void setSaleId(Long saleId) { this.saleId = saleId; }
    public boolean isSynced() { return synced; }
    public void setSynced(boolean synced) { this.synced = synced; }
    public List<InvoiceItemDTO> getItems() { return items; }
    public void setItems(List<InvoiceItemDTO> items) { this.items = items; }
}
