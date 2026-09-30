package com.example.invoiceapp.dto;

import com.example.invoiceapp.model.enums.StockMovementType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class StockAdjustmentRequest {
    @NotNull(message = "Warehouse ID is required")
    private Long warehouseId;

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Movement type is required")
    private StockMovementType movementType;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private BigDecimal quantity;

    private String notes;

    public StockAdjustmentRequest() {}

    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public StockMovementType getMovementType() { return movementType; }
    public void setMovementType(StockMovementType movementType) { this.movementType = movementType; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
