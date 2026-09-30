package com.example.invoiceapp.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "invoices", uniqueConstraints = {
        @UniqueConstraint(name = "uk_invoice_org_number", columnNames = {"organization_id", "invoice_number"})
})
public class Invoice extends SyncedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @Column(nullable = false)
    private String invoiceNumber;

    private LocalDate issueDate;
    private LocalDate dueDate;

    private String paymentTerms = "Net 15 Days";

    private Long customerId;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String customerAddress;

    private String status = "PENDING";

    private double taxRate = 18.0;
    private double discount = 0.0;
    private double amountPaid = 0.0;

    @Column(length = 1000)
    private String notes;

    private ZonedDateTime generatedAt;

    @Column(nullable = true)
    private Boolean synced = false;
    private String supabaseId;

    private Long saleId;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "invoice_id")
    private List<InvoiceItem> items = new ArrayList<>();

    public Invoice() {
        this.issueDate = LocalDate.now();
        this.dueDate = LocalDate.now().plusDays(15);
        this.generatedAt = ZonedDateTime.now();
    }

    public boolean hasPaymentTerms() {
        return paymentTerms != null && !paymentTerms.trim().isEmpty();
    }

    public String getFormattedGeneratedAt() {
        if (generatedAt == null) generatedAt = ZonedDateTime.now();
        return generatedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z"));
    }

    public double getSubtotal() {
        if (items == null) return 0.0;
        return items.stream().mapToDouble(InvoiceItem::getTotal).sum();
    }

    public double getTaxAmount() {
        return (getSubtotal() - discount > 0 ? (getSubtotal() - discount) : 0) * (taxRate / 100.0);
    }

    public double getTotal() {
        double base = getSubtotal() - discount;
        if (base < 0) base = 0;
        return base + getTaxAmount();
    }

    public double getOutstanding() {
        double out = getTotal() - amountPaid;
        return Math.max(out, 0);
    }

    public void addItem(InvoiceItem item) {
        items.add(item);
    }

    public void removeItem(InvoiceItem item) {
        items.remove(item);
    }

    public void refreshPaymentStatus() {
        if ("CANCELLED".equalsIgnoreCase(status) || "DRAFT".equalsIgnoreCase(status)) {
            return;
        }
        double total = getTotal();
        if (amountPaid <= 0.0001) {
            if (dueDate != null && dueDate.isBefore(LocalDate.now())) {
                status = "OVERDUE";
            } else {
                status = "PENDING";
            }
        } else if (amountPaid + 0.0001 >= total) {
            status = "PAID";
        } else {
            status = "PARTIALLY_PAID";
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }

    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public String getPaymentTerms() { return paymentTerms; }
    public void setPaymentTerms(String paymentTerms) { this.paymentTerms = paymentTerms; }

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

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getTaxRate() { return taxRate; }
    public void setTaxRate(double taxRate) { this.taxRate = taxRate; }

    public double getDiscount() { return discount; }
    public void setDiscount(double discount) { this.discount = discount; }

    public double getAmountPaid() { return amountPaid; }
    public void setAmountPaid(double amountPaid) { this.amountPaid = amountPaid; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public ZonedDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(ZonedDateTime generatedAt) { this.generatedAt = generatedAt; }

    public boolean isSynced() { return synced; }
    public void setSynced(boolean synced) { this.synced = synced; }

    public String getSupabaseId() { return supabaseId; }
    public void setSupabaseId(String supabaseId) { this.supabaseId = supabaseId; }

    public Long getSaleId() { return saleId; }
    public void setSaleId(Long saleId) { this.saleId = saleId; }

    public List<InvoiceItem> getItems() { return items; }
    public void setItems(List<InvoiceItem> items) { this.items = items; }
}
