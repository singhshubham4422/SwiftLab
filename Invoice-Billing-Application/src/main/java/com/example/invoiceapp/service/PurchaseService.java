package com.example.invoiceapp.service;

import com.example.invoiceapp.dto.PurchaseCreateRequest;
import com.example.invoiceapp.dto.PurchaseDTO;
import com.example.invoiceapp.dto.PurchaseItemDTO;
import com.example.invoiceapp.model.Product;
import com.example.invoiceapp.model.Purchase;
import com.example.invoiceapp.model.PurchaseItem;
import com.example.invoiceapp.model.Supplier;
import com.example.invoiceapp.model.Warehouse;
import com.example.invoiceapp.model.enums.PurchaseStatus;
import com.example.invoiceapp.model.enums.StockMovementType;
import com.example.invoiceapp.model.enums.SyncOperation;
import com.example.invoiceapp.repository.ProductRepository;
import com.example.invoiceapp.repository.PurchaseRepository;
import com.example.invoiceapp.repository.SupplierRepository;
import com.example.invoiceapp.repository.WarehouseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PurchaseService {

    private final PurchaseRepository purchaseRepo;
    private final SupplierRepository supplierRepo;
    private final WarehouseRepository warehouseRepo;
    private final ProductRepository productRepo;
    private final InventoryService inventoryService;
    private final SyncQueueService syncQueueService;

    public PurchaseService(PurchaseRepository purchaseRepo,
                           SupplierRepository supplierRepo,
                           WarehouseRepository warehouseRepo,
                           ProductRepository productRepo,
                           InventoryService inventoryService,
                           SyncQueueService syncQueueService) {
        this.purchaseRepo = purchaseRepo;
        this.supplierRepo = supplierRepo;
        this.warehouseRepo = warehouseRepo;
        this.productRepo = productRepo;
        this.inventoryService = inventoryService;
        this.syncQueueService = syncQueueService;
    }

    public List<PurchaseDTO> listByOrganization(Long organizationId) {
        if (organizationId == null) return List.of();
        List<Purchase> purchases = purchaseRepo.findByOrganizationIdAndDeletedFalseOrderByIdDesc(organizationId);
        Map<Long, String> supplierNames = supplierRepo.findByOrganizationIdAndDeletedFalseOrderByNameAsc(organizationId)
                .stream().collect(Collectors.toMap(Supplier::getId, Supplier::getName, (a, b) -> a));
        Map<Long, String> warehouseNames = warehouseRepo.findByOrganizationIdAndDeletedFalseOrderByNameAsc(organizationId)
                .stream().collect(Collectors.toMap(Warehouse::getId, Warehouse::getName, (a, b) -> a));
        Map<Long, Product> productMap = productRepo.findByOrganizationIdAndDeletedFalseOrderByNameAsc(organizationId)
                .stream().collect(Collectors.toMap(Product::getId, p -> p, (a, b) -> a));

        return purchases.stream().map(p -> toDTO(
                p,
                p.getSupplierId() != null ? supplierNames.getOrDefault(p.getSupplierId(), "Unknown") : "N/A",
                p.getWarehouseId() != null ? warehouseNames.getOrDefault(p.getWarehouseId(), "Unknown") : "N/A",
                productMap
        )).collect(Collectors.toList());
    }

    public Optional<Purchase> findById(Long id, Long organizationId) {
        if (id == null || organizationId == null) return Optional.empty();
        return purchaseRepo.findByIdAndOrganizationId(id, organizationId)
                .filter(p -> !p.isDeleted());
    }

    public Purchase get(Long id, Long organizationId) {
        return findById(id, organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Purchase not found with ID: " + id));
    }

    @Transactional
    public Purchase createPurchase(Long organizationId, PurchaseCreateRequest req) {
        if (organizationId == null) throw new IllegalArgumentException("Organization ID is required");
        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw new IllegalArgumentException("Purchase must contain at least one item");
        }

        Purchase purchase = new Purchase();
        purchase.setOrganizationId(organizationId);
        purchase.setSupplierId(req.getSupplierId());
        purchase.setWarehouseId(req.getWarehouseId());
        purchase.setPurchaseDate(req.getPurchaseDate() != null ? req.getPurchaseDate() : LocalDate.now());
        purchase.setPurchaseNumber(generateNextPurchaseNumber(organizationId));
        purchase.setNotes(req.getNotes());
        purchase.setCreatedAt(Instant.now());
        purchase.setUpdatedAt(Instant.now());

        BigDecimal grandTotal = BigDecimal.ZERO;
        for (var itemReq : req.getItems()) {
            PurchaseItem item = new PurchaseItem();
            item.setProductId(itemReq.getProductId());
            item.setDescription(itemReq.getDescription());
            item.setQuantity(itemReq.getQuantity() != null ? itemReq.getQuantity() : BigDecimal.ONE);
            item.setUnitCost(itemReq.getUnitCost() != null ? itemReq.getUnitCost() : BigDecimal.ZERO);
            purchase.getItems().add(item);
            grandTotal = grandTotal.add(item.getLineTotal());
        }
        purchase.setTotal(grandTotal);

        if (req.isReceiveNow()) {
            purchase.setStatus(PurchaseStatus.RECEIVED);
        } else {
            purchase.setStatus(PurchaseStatus.ORDERED);
        }

        Purchase saved = purchaseRepo.save(purchase);

        // If goods received immediately, generate inventory movements
        if (saved.getStatus() == PurchaseStatus.RECEIVED) {
            for (PurchaseItem item : saved.getItems()) {
                inventoryService.recordMovement(
                        organizationId,
                        saved.getWarehouseId(),
                        item.getProductId(),
                        StockMovementType.PURCHASE,
                        item.getQuantity(),
                        "PURCHASE",
                        saved.getId(),
                        "Received with PO #" + saved.getPurchaseNumber()
                );
            }
        }

        syncQueueService.enqueue(
                organizationId,
                "Purchase",
                saved.getPublicId(),
                SyncOperation.CREATE,
                saved,
                null
        );

        return saved;
    }

    @Transactional
    public Purchase receiveGoods(Long id, Long organizationId) {
        Purchase purchase = get(id, organizationId);
        if (purchase.getStatus() == PurchaseStatus.RECEIVED) {
            throw new IllegalStateException("Purchase #" + purchase.getPurchaseNumber() + " is already marked as RECEIVED.");
        }

        purchase.setStatus(PurchaseStatus.RECEIVED);
        purchase.setUpdatedAt(Instant.now());
        Purchase saved = purchaseRepo.save(purchase);

        for (PurchaseItem item : saved.getItems()) {
            inventoryService.recordMovement(
                    organizationId,
                    saved.getWarehouseId(),
                    item.getProductId(),
                    StockMovementType.PURCHASE,
                    item.getQuantity(),
                    "PURCHASE",
                    saved.getId(),
                    "Goods received for PO #" + saved.getPurchaseNumber()
            );
        }

        syncQueueService.enqueue(
                organizationId,
                "Purchase",
                saved.getPublicId(),
                SyncOperation.UPDATE,
                saved,
                null
        );

        return saved;
    }

    @Transactional
    public void delete(Long id, Long organizationId) {
        Purchase p = get(id, organizationId);
        p.markDeleted();
        purchaseRepo.save(p);
    }

    private String generateNextPurchaseNumber(Long organizationId) {
        long count = purchaseRepo.count() + 1;
        return String.format("PO-%04d", count);
    }

    public PurchaseDTO toDTO(Purchase p, String supplierName, String warehouseName, Map<Long, Product> productMap) {
        if (p == null) return null;
        PurchaseDTO dto = new PurchaseDTO();
        dto.setId(p.getId());
        dto.setPublicId(p.getPublicId());
        dto.setPurchaseNumber(p.getPurchaseNumber());
        dto.setSupplierId(p.getSupplierId());
        dto.setSupplierName(supplierName);
        dto.setWarehouseId(p.getWarehouseId());
        dto.setWarehouseName(warehouseName);
        dto.setPurchaseDate(p.getPurchaseDate());
        dto.setStatus(p.getStatus());
        dto.setTotal(p.getTotal());
        dto.setNotes(p.getNotes());

        if (p.getItems() != null) {
            for (PurchaseItem item : p.getItems()) {
                PurchaseItemDTO itemDTO = new PurchaseItemDTO();
                itemDTO.setId(item.getId());
                itemDTO.setProductId(item.getProductId());
                Product prod = productMap != null ? productMap.get(item.getProductId()) : null;
                itemDTO.setProductName(prod != null ? prod.getName() : "Product #" + item.getProductId());
                itemDTO.setProductSku(prod != null ? prod.getSku() : "");
                itemDTO.setDescription(item.getDescription());
                itemDTO.setQuantity(item.getQuantity());
                itemDTO.setUnitCost(item.getUnitCost());
                itemDTO.setLineTotal(item.getLineTotal());
                dto.getItems().add(itemDTO);
            }
        }
        return dto;
    }
}
