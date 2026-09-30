package com.example.invoiceapp.dto;

import com.example.invoiceapp.model.enums.PurchaseStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PurchaseDTO {
    private Long id;
    private String publicId;
    private String purchaseNumber;
    private Long supplierId;
    private String supplierName;
    private Long warehouseId;
    private String warehouseName;
    private LocalDate purchaseDate;
    private PurchaseStatus status;
    private BigDecimal total = BigDecimal.ZERO;
    private String notes;
    private List<PurchaseItemDTO> items = new ArrayList<>();

    public PurchaseDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPublicId() { return publicId; }
    public void setPublicId(String publicId) { this.publicId = publicId; }
    public String getPurchaseNumber() { return purchaseNumber; }
    public void setPurchaseNumber(String purchaseNumber) { this.purchaseNumber = purchaseNumber; }
    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }
    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }
    public String getWarehouseName() { return warehouseName; }
    public void setWarehouseName(String warehouseName) { this.warehouseName = warehouseName; }
    public LocalDate getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(LocalDate purchaseDate) { this.purchaseDate = purchaseDate; }
    public PurchaseStatus getStatus() { return status; }
    public void setStatus(PurchaseStatus status) { this.status = status; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public List<PurchaseItemDTO> getItems() { return items; }
    public void setItems(List<PurchaseItemDTO> items) { this.items = items; }
}
