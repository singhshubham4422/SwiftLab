package com.example.invoiceapp.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "stock", uniqueConstraints = {
        @UniqueConstraint(name = "uk_stock_org_wh_product", columnNames = {"organization_id", "warehouse_id", "product_id"})
})
public class Stock extends SyncedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;
    @Column(name = "product_id", nullable = false)
    private Long productId;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity = BigDecimal.ZERO;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
}
