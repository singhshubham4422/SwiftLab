package com.example.invoiceapp.dto;

import java.math.BigDecimal;

public class StockDTO {
    private Long id;
    private Long warehouseId;
    private String warehouseName;
    private Long productId;
    private String productName;
    private String sku;
    private BigDecimal quantity = BigDecimal.ZERO;

    public StockDTO() {}

    public StockDTO(Long id, Long warehouseId, String warehouseName, Long productId, String productName, String sku, BigDecimal quantity) {
        this.id = id;
        this.warehouseId = warehouseId;
        this.warehouseName = warehouseName;
        this.productId = productId;
        this.productName = productName;
        this.sku = sku;
        this.quantity = quantity;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }
    public String getWarehouseName() { return warehouseName; }
    public void setWarehouseName(String warehouseName) { this.warehouseName = warehouseName; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
}
