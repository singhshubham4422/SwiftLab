package com.example.invoiceapp.model;

import com.example.invoiceapp.model.enums.StockMovementType;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "stock_movements", uniqueConstraints = {
        @UniqueConstraint(name = "uk_stock_movement_public", columnNames = {"organization_id", "public_id"})
})
public class StockMovement extends SyncedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long warehouseId;
    private Long productId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StockMovementType movementType;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;
    private String referenceType;
    private Long referenceId;
    private String deviceId;
    private String notes;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public StockMovementType getMovementType() { return movementType; }
    public void setMovementType(StockMovementType movementType) { this.movementType = movementType; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public String getReferenceType() { return referenceType; }
    public void setReferenceType(String referenceType) { this.referenceType = referenceType; }
    public Long getReferenceId() { return referenceId; }
    public void setReferenceId(Long referenceId) { this.referenceId = referenceId; }
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
