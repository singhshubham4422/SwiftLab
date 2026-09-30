package com.example.invoiceapp.dto;

import com.example.invoiceapp.model.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class SaleCreateRequest {
    private Long customerId;

    @NotNull(message = "Warehouse ID is required")
    private Long warehouseId;

    private LocalDate saleDate = LocalDate.now();
    private String notes;
    private boolean completeNow = true;
    private boolean markPaid = false;
    private PaymentMethod paymentMethod = PaymentMethod.CASH;

    @NotEmpty(message = "Items list cannot be empty")
    @Valid
    private List<ItemRequest> items = new ArrayList<>();

    public SaleCreateRequest() {}

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }
    public LocalDate getSaleDate() { return saleDate; }
    public void setSaleDate(LocalDate saleDate) { this.saleDate = saleDate; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public boolean isCompleteNow() { return completeNow; }
    public void setCompleteNow(boolean completeNow) { this.completeNow = completeNow; }
    public boolean isMarkPaid() { return markPaid; }
    public void setMarkPaid(boolean markPaid) { this.markPaid = markPaid; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
    public List<ItemRequest> getItems() { return items; }
    public void setItems(List<ItemRequest> items) { this.items = items; }

    public static class ItemRequest {
        @NotNull(message = "Product ID is required")
        private Long productId;
        private String description;
        @NotNull(message = "Quantity is required")
        private BigDecimal quantity = BigDecimal.ONE;
        @NotNull(message = "Unit price is required")
        private BigDecimal unitPrice = BigDecimal.ZERO;

        public ItemRequest() {}

        public ItemRequest(Long productId, String description, BigDecimal quantity, BigDecimal unitPrice) {
            this.productId = productId;
            this.description = description;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
        }

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public BigDecimal getQuantity() { return quantity; }
        public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
        public BigDecimal getUnitPrice() { return unitPrice; }
        public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    }
}
