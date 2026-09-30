package com.example.invoiceapp.service;

import com.example.invoiceapp.dto.DashboardSummaryDTO;
import com.example.invoiceapp.model.*;
import com.example.invoiceapp.model.enums.SyncStatus;
import com.example.invoiceapp.repository.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final SaleRepository saleRepo;
    private final PurchaseRepository purchaseRepo;
    private final InvoiceRepository invoiceRepo;
    private final ProductRepository productRepo;
    private final StockRepository stockRepo;
    private final SyncQueueRepository syncQueueRepo;
    private final AppRuntimeService appRuntimeService;
    private final SaleService saleService;
    private final PurchaseService purchaseService;
    private final InvoiceService invoiceService;

    public ReportService(SaleRepository saleRepo,
                         PurchaseRepository purchaseRepo,
                         InvoiceRepository invoiceRepo,
                         ProductRepository productRepo,
                         StockRepository stockRepo,
                         SyncQueueRepository syncQueueRepo,
                         AppRuntimeService appRuntimeService,
                         SaleService saleService,
                         PurchaseService purchaseService,
                         InvoiceService invoiceService) {
        this.saleRepo = saleRepo;
        this.purchaseRepo = purchaseRepo;
        this.invoiceRepo = invoiceRepo;
        this.productRepo = productRepo;
        this.stockRepo = stockRepo;
        this.syncQueueRepo = syncQueueRepo;
        this.appRuntimeService = appRuntimeService;
        this.saleService = saleService;
        this.purchaseService = purchaseService;
        this.invoiceService = invoiceService;
    }

    public DashboardSummaryDTO getDashboardSummary(Long organizationId) {
        DashboardSummaryDTO summary = new DashboardSummaryDTO();
        if (organizationId == null) {
            return summary;
        }

        // 1. Sales & Purchases
        List<Sale> sales = saleRepo.findByOrganizationIdAndDeletedFalseOrderByIdDesc(organizationId);
        List<Purchase> purchases = purchaseRepo.findByOrganizationIdAndDeletedFalseOrderByIdDesc(organizationId);

        BigDecimal totalSales = sales.stream().map(Sale::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPurchases = purchases.stream().map(Purchase::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.setTotalSales(totalSales);
        summary.setTotalPurchases(totalPurchases);

        LocalDate today = LocalDate.now();
        BigDecimal todaySales = saleRepo.findByOrganizationIdAndSaleDate(organizationId, today).stream()
                .map(Sale::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal todayPurchases = purchaseRepo.findByOrganizationIdAndPurchaseDate(organizationId, today).stream()
                .map(Purchase::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.setTodaySales(todaySales);
        summary.setTodayPurchases(todayPurchases);

        // 2. Inventory Valuation & Low Stock
        List<Stock> stocks = stockRepo.findByOrganizationId(organizationId);
        List<Product> products = productRepo.findByOrganizationIdAndDeletedFalseOrderByNameAsc(organizationId);
        Map<Long, BigDecimal> productCostMap = products.stream()
                .collect(Collectors.toMap(Product::getId, Product::getPurchasePrice, (a, b) -> a));
        Map<Long, BigDecimal> productMinStockMap = products.stream()
                .collect(Collectors.toMap(Product::getId, Product::getMinimumStock, (a, b) -> a));

        BigDecimal valuation = BigDecimal.ZERO;
        long lowStockCount = 0;

        Map<Long, BigDecimal> productTotalStock = stocks.stream()
                .collect(Collectors.groupingBy(Stock::getProductId,
                        Collectors.reducing(BigDecimal.ZERO, Stock::getQuantity, BigDecimal::add)));

        for (Product p : products) {
            BigDecimal qty = productTotalStock.getOrDefault(p.getId(), BigDecimal.ZERO);
            BigDecimal cost = p.getPurchasePrice() != null ? p.getPurchasePrice() : BigDecimal.ZERO;
            valuation = valuation.add(qty.multiply(cost));

            BigDecimal minStock = p.getMinimumStock() != null ? p.getMinimumStock() : BigDecimal.ZERO;
            if (minStock.compareTo(BigDecimal.ZERO) > 0 && qty.compareTo(minStock) <= 0) {
                lowStockCount++;
            }
        }
        summary.setInventoryValue(valuation);
        summary.setLowStockCount(lowStockCount);

        // 3. Invoices
        List<Invoice> invoices = invoiceRepo.findByOrganizationIdAndDeletedFalseOrderByIdDesc(organizationId);
        double outstanding = invoices.stream().mapToDouble(Invoice::getOutstanding).sum();
        double paid = invoices.stream().mapToDouble(Invoice::getAmountPaid).sum();
        long overdue = invoices.stream().filter(i -> "OVERDUE".equalsIgnoreCase(i.getStatus())).count();
        summary.setOutstandingInvoicesAmount(outstanding);
        summary.setPaidInvoicesAmount(paid);
        summary.setOverdueInvoicesCount(overdue);

        // 4. Mode & Sync Status
        AppRuntime rt = appRuntimeService.getRuntime();
        summary.setDataMode(rt.getDataMode());
        summary.setOnline(rt.isLastOnline());
        summary.setPendingSyncCount(syncQueueRepo.countByStatus(SyncStatus.PENDING));

        // 5. Recent records
        summary.setRecentSales(saleService.listByOrganization(organizationId).stream().limit(5).collect(Collectors.toList()));
        summary.setRecentPurchases(purchaseService.listByOrganization(organizationId).stream().limit(5).collect(Collectors.toList()));
        summary.setRecentInvoices(invoiceService.listDTOByOrganization(organizationId).stream().limit(5).collect(Collectors.toList()));

        return summary;
    }
}
