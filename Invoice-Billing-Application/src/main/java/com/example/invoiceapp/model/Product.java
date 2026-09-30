package com.example.invoiceapp.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "products", uniqueConstraints = {
        @UniqueConstraint(name = "uk_product_org_sku", columnNames = {"organization_id", "sku"})
})
public class Product extends SyncedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String sku;
    private String barcode;
    @Column(nullable = false)
    private String name;
    @Column(length = 2000)
    private String description;
    private Long categoryId;
    private Long brandId;
    private Long unitId;
    private BigDecimal purchasePrice = BigDecimal.ZERO;
    private BigDecimal sellingPrice = BigDecimal.ZERO;
    private BigDecimal taxRate = BigDecimal.ZERO;
    private BigDecimal minimumStock = BigDecimal.ZERO;
    private boolean active = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public Long getBrandId() { return brandId; }
    public void setBrandId(Long brandId) { this.brandId = brandId; }
    public Long getUnitId() { return unitId; }
    public void setUnitId(Long unitId) { this.unitId = unitId; }
    public BigDecimal getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(BigDecimal purchasePrice) { this.purchasePrice = purchasePrice; }
    public BigDecimal getSellingPrice() { return sellingPrice; }
    public void setSellingPrice(BigDecimal sellingPrice) { this.sellingPrice = sellingPrice; }
    public BigDecimal getTaxRate() { return taxRate; }
    public void setTaxRate(BigDecimal taxRate) { this.taxRate = taxRate; }
    public BigDecimal getMinimumStock() { return minimumStock; }
    public void setMinimumStock(BigDecimal minimumStock) { this.minimumStock = minimumStock; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
