package com.example.invoiceapp.service;

import com.example.invoiceapp.dto.StockAdjustmentRequest;
import com.example.invoiceapp.dto.StockDTO;
import com.example.invoiceapp.dto.StockMovementDTO;
import com.example.invoiceapp.model.Organization;
import com.example.invoiceapp.model.Product;
import com.example.invoiceapp.model.Stock;
import com.example.invoiceapp.model.StockMovement;
import com.example.invoiceapp.model.Warehouse;
import com.example.invoiceapp.model.enums.StockMovementType;
import com.example.invoiceapp.model.enums.SyncOperation;
import com.example.invoiceapp.repository.OrganizationRepository;
import com.example.invoiceapp.repository.ProductRepository;
import com.example.invoiceapp.repository.StockMovementRepository;
import com.example.invoiceapp.repository.StockRepository;
import com.example.invoiceapp.repository.WarehouseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class InventoryService {

    private final StockRepository stockRepo;
    private final StockMovementRepository movementRepo;
    private final ProductRepository productRepo;
    private final WarehouseRepository warehouseRepo;
    private final OrganizationRepository orgRepo;
    private final SyncQueueService syncQueueService;
    private final DeviceIdentityService deviceIdentityService;

    public InventoryService(StockRepository stockRepo,
                            StockMovementRepository movementRepo,
                            ProductRepository productRepo,
                            WarehouseRepository warehouseRepo,
                            OrganizationRepository orgRepo,
                            SyncQueueService syncQueueService,
                            DeviceIdentityService deviceIdentityService) {
        this.stockRepo = stockRepo;
        this.movementRepo = movementRepo;
        this.productRepo = productRepo;
        this.warehouseRepo = warehouseRepo;
        this.orgRepo = orgRepo;
        this.syncQueueService = syncQueueService;
        this.deviceIdentityService = deviceIdentityService;
    }

    @Transactional
    public StockMovement recordMovement(Long organizationId,
                                        Long warehouseId,
                                        Long productId,
                                        StockMovementType type,
                                        BigDecimal quantity,
                                        String referenceType,
                                        Long referenceId,
                                        String notes) {
        if (organizationId == null) throw new IllegalArgumentException("Organization ID is required");
        if (warehouseId == null) throw new IllegalArgumentException("Warehouse ID is required");
        if (productId == null) throw new IllegalArgumentException("Product ID is required");
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Movement quantity must be positive");
        }

        Organization org = orgRepo.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));
        Product product = productRepo.findByIdAndOrganizationId(productId, organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found in organization"));
        Warehouse warehouse = warehouseRepo.findById(warehouseId)
                .orElseThrow(() -> new IllegalArgumentException("Warehouse not found"));

        // Determine stock change sign:
        // STOCK_IN, PURCHASE, RETURN = +quantity
        // STOCK_OUT, SALE = -quantity
        // ADJUSTMENT, TRANSFER depend on context; default ADJUSTMENT = delta, or we treat positive quantity as addition
        BigDecimal delta;
        switch (type) {
            case STOCK_IN, PURCHASE, RETURN -> delta = quantity;
            case STOCK_OUT, SALE -> delta = quantity.negate();
            case ADJUSTMENT -> delta = quantity; // can be passed negative by caller if needed
            case TRANSFER -> delta = quantity.negate();
            default -> delta = quantity;
        }

        // Pessimistic write lock on Stock entry to prevent concurrent race conditions
        Stock stock = stockRepo.findByOrganizationIdAndWarehouseIdAndProductId(organizationId, warehouseId, productId)
                .orElseGet(() -> {
                    Stock newStock = new Stock();
                    newStock.setOrganizationId(organizationId);
                    newStock.setWarehouseId(warehouseId);
                    newStock.setProductId(productId);
                    newStock.setQuantity(BigDecimal.ZERO);
                    return newStock;
                });

        BigDecimal newQty = stock.getQuantity().add(delta);
        if (newQty.compareTo(BigDecimal.ZERO) < 0 && !org.isAllowNegativeInventory()) {
            throw new IllegalStateException(String.format(
                    "Insufficient stock for product '%s'. Current: %s, Requested reduction: %s",
                    product.getName(), stock.getQuantity(), quantity
            ));
        }

        stock.setQuantity(newQty);
        stockRepo.save(stock);

        // Record auditable StockMovement
        StockMovement movement = new StockMovement();
        movement.setOrganizationId(organizationId);
        movement.setWarehouseId(warehouseId);
        movement.setProductId(productId);
        movement.setMovementType(type);
        movement.setQuantity(quantity);
        movement.setReferenceType(referenceType);
        movement.setReferenceId(referenceId);
        movement.setDeviceId(deviceIdentityService.localDeviceId());
        movement.setNotes(notes);
        movement.setCreatedAt(Instant.now());
        StockMovement savedMovement = movementRepo.save(movement);

        // Enqueue sync if Cloud Sync is active
        syncQueueService.enqueue(
                organizationId,
                "StockMovement",
                savedMovement.getPublicId(),
                SyncOperation.CREATE,
                toDTO(savedMovement, product.getName(), product.getSku(), warehouse.getName()),
                null
        );

        return savedMovement;
    }

    @Transactional
    public StockMovement adjustStock(Long organizationId, StockAdjustmentRequest req) {
        return recordMovement(
                organizationId,
                req.getWarehouseId(),
                req.getProductId(),
                req.getMovementType() != null ? req.getMovementType() : StockMovementType.ADJUSTMENT,
                req.getQuantity(),
                "MANUAL_ADJUSTMENT",
                null,
                req.getNotes()
        );
    }

    public List<StockDTO> listStock(Long organizationId) {
        if (organizationId == null) return List.of();
        List<Stock> stocks = stockRepo.findByOrganizationId(organizationId);
        Map<Long, String> productNames = productRepo.findByOrganizationIdAndDeletedFalseOrderByNameAsc(organizationId)
                .stream().collect(Collectors.toMap(Product::getId, Product::getName, (a, b) -> a));
        Map<Long, String> productSkus = productRepo.findByOrganizationIdAndDeletedFalseOrderByNameAsc(organizationId)
                .stream().collect(Collectors.toMap(Product::getId, Product::getSku, (a, b) -> a));
        Map<Long, String> warehouseNames = warehouseRepo.findByOrganizationIdAndDeletedFalseOrderByNameAsc(organizationId)
                .stream().collect(Collectors.toMap(Warehouse::getId, Warehouse::getName, (a, b) -> a));

        return stocks.stream().map(s -> new StockDTO(
                s.getId(),
                s.getWarehouseId(),
                warehouseNames.getOrDefault(s.getWarehouseId(), "Unknown Warehouse"),
                s.getProductId(),
                productNames.getOrDefault(s.getProductId(), "Unknown Product"),
                productSkus.getOrDefault(s.getProductId(), ""),
                s.getQuantity()
        )).collect(Collectors.toList());
    }

    public List<StockMovementDTO> listMovements(Long organizationId) {
        if (organizationId == null) return List.of();
        List<StockMovement> movements = movementRepo.findByOrganizationIdOrderByCreatedAtDesc(organizationId);
        Map<Long, String> productNames = productRepo.findByOrganizationIdAndDeletedFalseOrderByNameAsc(organizationId)
                .stream().collect(Collectors.toMap(Product::getId, Product::getName, (a, b) -> a));
        Map<Long, String> productSkus = productRepo.findByOrganizationIdAndDeletedFalseOrderByNameAsc(organizationId)
                .stream().collect(Collectors.toMap(Product::getId, Product::getSku, (a, b) -> a));
        Map<Long, String> warehouseNames = warehouseRepo.findByOrganizationIdAndDeletedFalseOrderByNameAsc(organizationId)
                .stream().collect(Collectors.toMap(Warehouse::getId, Warehouse::getName, (a, b) -> a));

        return movements.stream().map(m -> toDTO(
                m,
                productNames.getOrDefault(m.getProductId(), "Product #" + m.getProductId()),
                productSkus.getOrDefault(m.getProductId(), ""),
                warehouseNames.getOrDefault(m.getWarehouseId(), "Warehouse #" + m.getWarehouseId())
        )).collect(Collectors.toList());
    }

    public BigDecimal getAvailableStock(Long organizationId, Long warehouseId, Long productId) {
        return stockRepo.findByOrganizationIdAndWarehouseIdAndProductId(organizationId, warehouseId, productId)
                .map(Stock::getQuantity)
                .orElse(BigDecimal.ZERO);
    }

    public StockMovementDTO toDTO(StockMovement m, String productName, String sku, String warehouseName) {
        if (m == null) return null;
        StockMovementDTO dto = new StockMovementDTO();
        dto.setId(m.getId());
        dto.setPublicId(m.getPublicId());
        dto.setWarehouseId(m.getWarehouseId());
        dto.setWarehouseName(warehouseName);
        dto.setProductId(m.getProductId());
        dto.setProductName(productName);
        dto.setProductSku(sku);
        dto.setMovementType(m.getMovementType());
        dto.setQuantity(m.getQuantity());
        dto.setReferenceType(m.getReferenceType());
        dto.setReferenceId(m.getReferenceId());
        dto.setDeviceId(m.getDeviceId());
        dto.setNotes(m.getNotes());
        dto.setCreatedAt(m.getCreatedAt());
        return dto;
    }
}
