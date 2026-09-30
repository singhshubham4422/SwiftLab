package com.example.invoiceapp.dto;

import java.math.BigDecimal;

public class ProductDTO {
    private Long id;
    private String publicId;
    private String sku;
    private String barcode;
    private String name;
    private String description;
    private Long categoryId;
    private String categoryName;
    private Long brandId;
    private String brandName;
    private Long unitId;
    private String unitName;
    private BigDecimal purchasePrice = BigDecimal.ZERO;
    private BigDecimal sellingPrice = BigDecimal.ZERO;
    private BigDecimal taxRate = BigDecimal.ZERO;
    private BigDecimal minimumStock = BigDecimal.ZERO;
    private BigDecimal currentStock = BigDecimal.ZERO;
    private boolean active = true;

    public ProductDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPublicId() { return publicId; }
    public void setPublicId(String publicId) { this.publicId = publicId; }
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
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public Long getBrandId() { return brandId; }
    public void setBrandId(Long brandId) { this.brandId = brandId; }
    public String getBrandName() { return brandName; }
    public void setBrandName(String brandName) { this.brandName = brandName; }
    public Long getUnitId() { return unitId; }
    public void setUnitId(Long unitId) { this.unitId = unitId; }
    public String getUnitName() { return unitName; }
    public void setUnitName(String unitName) { this.unitName = unitName; }
    public BigDecimal getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(BigDecimal purchasePrice) { this.purchasePrice = purchasePrice; }
    public BigDecimal getSellingPrice() { return sellingPrice; }
    public void setSellingPrice(BigDecimal sellingPrice) { this.sellingPrice = sellingPrice; }
    public BigDecimal getTaxRate() { return taxRate; }
    public void setTaxRate(BigDecimal taxRate) { this.taxRate = taxRate; }
    public BigDecimal getMinimumStock() { return minimumStock; }
    public void setMinimumStock(BigDecimal minimumStock) { this.minimumStock = minimumStock; }
    public BigDecimal getCurrentStock() { return currentStock; }
    public void setCurrentStock(BigDecimal currentStock) { this.currentStock = currentStock; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
