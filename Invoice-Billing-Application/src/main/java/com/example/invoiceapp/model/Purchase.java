package com.example.invoiceapp.model;

import com.example.invoiceapp.model.enums.PurchaseStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "purchases")
public class Purchase extends SyncedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String purchaseNumber;
    private Long supplierId;
    private Long warehouseId;
    private LocalDate purchaseDate = LocalDate.now();
    @Enumerated(EnumType.STRING)
    private PurchaseStatus status = PurchaseStatus.DRAFT;
    private BigDecimal total = BigDecimal.ZERO;
    @Column(length = 2000)
    private String notes;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "purchase_id")
    private List<PurchaseItem> items = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPurchaseNumber() { return purchaseNumber; }
    public void setPurchaseNumber(String purchaseNumber) { this.purchaseNumber = purchaseNumber; }
    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }
    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }
    public LocalDate getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(LocalDate purchaseDate) { this.purchaseDate = purchaseDate; }
    public PurchaseStatus getStatus() { return status; }
    public void setStatus(PurchaseStatus status) { this.status = status; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public List<PurchaseItem> getItems() { return items; }
    public void setItems(List<PurchaseItem> items) { this.items = items; }
}
