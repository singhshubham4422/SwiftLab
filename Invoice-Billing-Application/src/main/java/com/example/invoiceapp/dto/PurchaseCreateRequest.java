package com.example.invoiceapp.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PurchaseCreateRequest {
    private Long supplierId;

    @NotNull(message = "Warehouse ID is required")
    private Long warehouseId;

    private LocalDate purchaseDate = LocalDate.now();
    private String notes;
    private boolean receiveNow = false;

    @NotEmpty(message = "Items list cannot be empty")
    @Valid
    private List<ItemRequest> items = new ArrayList<>();

    public PurchaseCreateRequest() {}

    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }
    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }
    public LocalDate getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(LocalDate purchaseDate) { this.purchaseDate = purchaseDate; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public boolean isReceiveNow() { return receiveNow; }
    public void setReceiveNow(boolean receiveNow) { this.receiveNow = receiveNow; }
    public List<ItemRequest> getItems() { return items; }
    public void setItems(List<ItemRequest> items) { this.items = items; }

    public static class ItemRequest {
        @NotNull(message = "Product ID is required")
        private Long productId;
        private String description;
        @NotNull(message = "Quantity is required")
        private BigDecimal quantity = BigDecimal.ONE;
        @NotNull(message = "Unit cost is required")
        private BigDecimal unitCost = BigDecimal.ZERO;

        public ItemRequest() {}

        public ItemRequest(Long productId, String description, BigDecimal quantity, BigDecimal unitCost) {
            this.productId = productId;
            this.description = description;
            this.quantity = quantity;
            this.unitCost = unitCost;
        }

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public BigDecimal getQuantity() { return quantity; }
        public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
        public BigDecimal getUnitCost() { return unitCost; }
        public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }
    }
}
