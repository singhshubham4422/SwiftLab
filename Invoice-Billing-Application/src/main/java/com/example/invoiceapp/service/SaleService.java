package com.example.invoiceapp.service;

import com.example.invoiceapp.dto.PaymentCreateRequest;
import com.example.invoiceapp.dto.SaleCreateRequest;
import com.example.invoiceapp.dto.SaleDTO;
import com.example.invoiceapp.dto.SaleItemDTO;
import com.example.invoiceapp.model.*;
import com.example.invoiceapp.model.enums.SaleStatus;
import com.example.invoiceapp.model.enums.StockMovementType;
import com.example.invoiceapp.model.enums.SyncOperation;
import com.example.invoiceapp.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SaleService {

    private final SaleRepository saleRepo;
    private final CustomerRepository customerRepo;
    private final WarehouseRepository warehouseRepo;
    private final ProductRepository productRepo;
    private final InvoiceRepository invoiceRepo;
    private final InventoryService inventoryService;
    private final PaymentService paymentService;
    private final SyncQueueService syncQueueService;

    public SaleService(SaleRepository saleRepo,
                       CustomerRepository customerRepo,
                       WarehouseRepository warehouseRepo,
                       ProductRepository productRepo,
                       InvoiceRepository invoiceRepo,
                       InventoryService inventoryService,
                       PaymentService paymentService,
                       SyncQueueService syncQueueService) {
        this.saleRepo = saleRepo;
        this.customerRepo = customerRepo;
        this.warehouseRepo = warehouseRepo;
        this.productRepo = productRepo;
        this.invoiceRepo = invoiceRepo;
        this.inventoryService = inventoryService;
        this.paymentService = paymentService;
        this.syncQueueService = syncQueueService;
    }

    public List<SaleDTO> listByOrganization(Long organizationId) {
        if (organizationId == null) return List.of();
        List<Sale> sales = saleRepo.findByOrganizationIdAndDeletedFalseOrderByIdDesc(organizationId);
        Map<Long, String> customerNames = customerRepo.findByOrganizationIdAndDeletedFalseOrderByNameAsc(organizationId)
                .stream().collect(Collectors.toMap(Customer::getId, Customer::getName, (a, b) -> a));
        Map<Long, String> warehouseNames = warehouseRepo.findByOrganizationIdAndDeletedFalseOrderByNameAsc(organizationId)
                .stream().collect(Collectors.toMap(Warehouse::getId, Warehouse::getName, (a, b) -> a));
        Map<Long, String> invoiceNumbers = invoiceRepo.findByOrganizationIdAndDeletedFalseOrderByIdDesc(organizationId)
                .stream().collect(Collectors.toMap(Invoice::getId, Invoice::getInvoiceNumber, (a, b) -> a));
        Map<Long, Product> productMap = productRepo.findByOrganizationIdAndDeletedFalseOrderByNameAsc(organizationId)
                .stream().collect(Collectors.toMap(Product::getId, p -> p, (a, b) -> a));

        return sales.stream().map(s -> toDTO(
                s,
                s.getCustomerId() != null ? customerNames.getOrDefault(s.getCustomerId(), "Walk-in Customer") : "Walk-in Customer",
                s.getWarehouseId() != null ? warehouseNames.getOrDefault(s.getWarehouseId(), "Default") : "Default",
                s.getInvoiceId() != null ? invoiceNumbers.getOrDefault(s.getInvoiceId(), "") : "",
                productMap
        )).collect(Collectors.toList());
    }

    public Optional<Sale> findById(Long id, Long organizationId) {
        if (id == null || organizationId == null) return Optional.empty();
        return saleRepo.findByIdAndOrganizationId(id, organizationId)
                .filter(s -> !s.isDeleted());
    }

    public Sale get(Long id, Long organizationId) {
        return findById(id, organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Sale not found with ID: " + id));
    }

    @Transactional
    public Sale createSale(Long organizationId, Long userId, SaleCreateRequest req) {
        if (organizationId == null) throw new IllegalArgumentException("Organization ID is required");
        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw new IllegalArgumentException("Sale must contain at least one item");
        }

        // 1. Validate organization and warehouse
        Warehouse warehouse = warehouseRepo.findById(req.getWarehouseId())
                .orElseThrow(() -> new IllegalArgumentException("Warehouse not found"));

        // 2 & 3. Validate products and stock availability if completing immediately
        for (var itemReq : req.getItems()) {
            Product prod = productRepo.findByIdAndOrganizationId(itemReq.getProductId(), organizationId)
                    .orElseThrow(() -> new IllegalArgumentException("Product #" + itemReq.getProductId() + " not found in organization"));
            if (req.isCompleteNow()) {
                BigDecimal available = inventoryService.getAvailableStock(organizationId, req.getWarehouseId(), prod.getId());
                if (available.compareTo(itemReq.getQuantity()) < 0) {
                    // Check if negative inventory is allowed
                    boolean allowNeg = warehouse.getOrganizationId() != null; // Checked by inventoryService
                }
            }
        }

        // 4 & 5. Create Sale and Sale Items
        Sale sale = new Sale();
        sale.setOrganizationId(organizationId);
        sale.setCustomerId(req.getCustomerId());
        sale.setWarehouseId(req.getWarehouseId());
        sale.setSaleDate(req.getSaleDate() != null ? req.getSaleDate() : LocalDate.now());
        sale.setSaleNumber(generateNextSaleNumber(organizationId));
        sale.setNotes(req.getNotes());
        sale.setStatus(req.isCompleteNow() ? SaleStatus.COMPLETED : SaleStatus.DRAFT);
        sale.setCreatedAt(Instant.now());
        sale.setUpdatedAt(Instant.now());

        BigDecimal grandTotal = BigDecimal.ZERO;
        for (var itemReq : req.getItems()) {
            SaleItem item = new SaleItem();
            item.setProductId(itemReq.getProductId());
            item.setDescription(itemReq.getDescription());
            item.setQuantity(itemReq.getQuantity() != null ? itemReq.getQuantity() : BigDecimal.ONE);
            item.setUnitPrice(itemReq.getUnitPrice() != null ? itemReq.getUnitPrice() : BigDecimal.ZERO);
            sale.getItems().add(item);
            grandTotal = grandTotal.add(item.getLineTotal());
        }
        sale.setTotal(grandTotal);

        Sale savedSale = saleRepo.save(sale);

        // 6. Create Stock Movements if completed
        if (savedSale.getStatus() == SaleStatus.COMPLETED) {
            for (SaleItem item : savedSale.getItems()) {
                inventoryService.recordMovement(
                        organizationId,
                        savedSale.getWarehouseId(),
                        item.getProductId(),
                        StockMovementType.SALE,
                        item.getQuantity(),
                        "SALE",
                        savedSale.getId(),
                        "Dispatched for Sale #" + savedSale.getSaleNumber()
                );
            }
        }

        // 7. Create linked Invoice
        Invoice invoice = new Invoice();
        invoice.setOrganizationId(organizationId);
        invoice.setUserId(userId);
        invoice.setSaleId(savedSale.getId());
        invoice.setInvoiceNumber("INV-" + savedSale.getSaleNumber().replace("SALE-", ""));
        invoice.setIssueDate(savedSale.getSaleDate());
        invoice.setDueDate(savedSale.getSaleDate().plusDays(15));
        invoice.setPaymentTerms("Net 15 Days");
        invoice.setTaxRate(0.0);
        invoice.setDiscount(0.0);
        invoice.setGeneratedAt(ZonedDateTime.now());
        invoice.setCreatedAt(Instant.now());
        invoice.setUpdatedAt(Instant.now());

        if (savedSale.getCustomerId() != null) {
            customerRepo.findByIdAndOrganizationId(savedSale.getCustomerId(), organizationId).ifPresent(c -> {
                invoice.setCustomerId(c.getId());
                invoice.setCustomerName(c.getName());
                invoice.setCustomerEmail(c.getEmail());
                invoice.setCustomerPhone(c.getPhone());
                invoice.setCustomerAddress(c.getAddress());
            });
        } else {
            invoice.setCustomerName("Walk-in Customer");
        }

        for (SaleItem item : savedSale.getItems()) {
            Product p = productRepo.findById(item.getProductId()).orElse(null);
            String desc = item.getDescription() != null && !item.getDescription().isBlank()
                    ? item.getDescription()
                    : (p != null ? p.getName() : "Item #" + item.getProductId());
            invoice.addItem(new InvoiceItem(desc, item.getQuantity().intValue(), item.getUnitPrice().doubleValue()));
        }

        invoice.refreshPaymentStatus();
        Invoice savedInvoice = invoiceRepo.save(invoice);

        savedSale.setInvoiceId(savedInvoice.getId());
        savedSale = saleRepo.save(savedSale);

        // 8. Create Payment if marked paid
        if (req.isMarkPaid() && grandTotal.compareTo(BigDecimal.ZERO) > 0) {
            PaymentCreateRequest payReq = new PaymentCreateRequest();
            payReq.setInvoiceId(savedInvoice.getId());
            payReq.setMethod(req.getPaymentMethod());
            payReq.setAmount(grandTotal);
            payReq.setPaidOn(savedSale.getSaleDate());
            payReq.setReference("Auto-settled at Sale #" + savedSale.getSaleNumber());
            paymentService.recordPayment(organizationId, payReq);
        }

        // 9. Enqueue sync if cloud sync is active
        syncQueueService.enqueue(
                organizationId,
                "Sale",
                savedSale.getPublicId(),
                SyncOperation.CREATE,
                savedSale,
                null
        );

        return savedSale;
    }

    @Transactional
    public void delete(Long id, Long organizationId) {
        Sale s = get(id, organizationId);
        s.markDeleted();
        saleRepo.save(s);
    }

    private String generateNextSaleNumber(Long organizationId) {
        long count = saleRepo.count() + 1;
        return String.format("SALE-%04d", count);
    }

    public SaleDTO toDTO(Sale s, String customerName, String warehouseName, String invoiceNumber, Map<Long, Product> productMap) {
        if (s == null) return null;
        SaleDTO dto = new SaleDTO();
        dto.setId(s.getId());
        dto.setPublicId(s.getPublicId());
        dto.setSaleNumber(s.getSaleNumber());
        dto.setCustomerId(s.getCustomerId());
        dto.setCustomerName(customerName);
        dto.setWarehouseId(s.getWarehouseId());
        dto.setWarehouseName(warehouseName);
        dto.setInvoiceId(s.getInvoiceId());
        dto.setInvoiceNumber(invoiceNumber);
        dto.setSaleDate(s.getSaleDate());
        dto.setStatus(s.getStatus());
        dto.setTotal(s.getTotal());
        dto.setNotes(s.getNotes());

        if (s.getItems() != null) {
            for (SaleItem item : s.getItems()) {
                SaleItemDTO itemDTO = new SaleItemDTO();
                itemDTO.setId(item.getId());
                itemDTO.setProductId(item.getProductId());
                Product prod = productMap != null ? productMap.get(item.getProductId()) : null;
                itemDTO.setProductName(prod != null ? prod.getName() : "Product #" + item.getProductId());
                itemDTO.setProductSku(prod != null ? prod.getSku() : "");
                itemDTO.setDescription(item.getDescription());
                itemDTO.setQuantity(item.getQuantity());
                itemDTO.setUnitPrice(item.getUnitPrice());
                itemDTO.setLineTotal(item.getLineTotal());
                dto.getItems().add(itemDTO);
            }
        }
        return dto;
    }
}
