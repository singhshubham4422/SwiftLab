package com.example.invoiceapp.dto;

import com.example.invoiceapp.model.enums.DataMode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class DashboardSummaryDTO {
    private BigDecimal totalSales = BigDecimal.ZERO;
    private BigDecimal totalPurchases = BigDecimal.ZERO;
    private BigDecimal inventoryValue = BigDecimal.ZERO;
    private long lowStockCount = 0;
    private double outstandingInvoicesAmount = 0.0;
    private double paidInvoicesAmount = 0.0;
    private long overdueInvoicesCount = 0;
    private BigDecimal todaySales = BigDecimal.ZERO;
    private BigDecimal todayPurchases = BigDecimal.ZERO;
    private DataMode dataMode = DataMode.LOCAL_ONLY;
    private boolean online = false;
    private long pendingSyncCount = 0;
    private List<SaleDTO> recentSales = new ArrayList<>();
    private List<PurchaseDTO> recentPurchases = new ArrayList<>();
    private List<InvoiceDTO> recentInvoices = new ArrayList<>();

    public DashboardSummaryDTO() {}

    public BigDecimal getTotalSales() { return totalSales; }
    public void setTotalSales(BigDecimal totalSales) { this.totalSales = totalSales; }
    public BigDecimal getTotalPurchases() { return totalPurchases; }
    public void setTotalPurchases(BigDecimal totalPurchases) { this.totalPurchases = totalPurchases; }
    public BigDecimal getInventoryValue() { return inventoryValue; }
    public void setInventoryValue(BigDecimal inventoryValue) { this.inventoryValue = inventoryValue; }
    public long getLowStockCount() { return lowStockCount; }
    public void setLowStockCount(long lowStockCount) { this.lowStockCount = lowStockCount; }
    public double getOutstandingInvoicesAmount() { return outstandingInvoicesAmount; }
    public void setOutstandingInvoicesAmount(double outstandingInvoicesAmount) { this.outstandingInvoicesAmount = outstandingInvoicesAmount; }
    public double getPaidInvoicesAmount() { return paidInvoicesAmount; }
    public void setPaidInvoicesAmount(double paidInvoicesAmount) { this.paidInvoicesAmount = paidInvoicesAmount; }
    public long getOverdueInvoicesCount() { return overdueInvoicesCount; }
    public void setOverdueInvoicesCount(long overdueInvoicesCount) { this.overdueInvoicesCount = overdueInvoicesCount; }
    public BigDecimal getTodaySales() { return todaySales; }
    public void setTodaySales(BigDecimal todaySales) { this.todaySales = todaySales; }
    public BigDecimal getTodayPurchases() { return todayPurchases; }
    public void setTodayPurchases(BigDecimal todayPurchases) { this.todayPurchases = todayPurchases; }
    public DataMode getDataMode() { return dataMode; }
    public void setDataMode(DataMode dataMode) { this.dataMode = dataMode; }
    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }
    public long getPendingSyncCount() { return pendingSyncCount; }
    public void setPendingSyncCount(long pendingSyncCount) { this.pendingSyncCount = pendingSyncCount; }
    public List<SaleDTO> getRecentSales() { return recentSales; }
    public void setRecentSales(List<SaleDTO> recentSales) { this.recentSales = recentSales; }
    public List<PurchaseDTO> getRecentPurchases() { return recentPurchases; }
    public void setRecentPurchases(List<PurchaseDTO> recentPurchases) { this.recentPurchases = recentPurchases; }
    public List<InvoiceDTO> getRecentInvoices() { return recentInvoices; }
    public void setRecentInvoices(List<InvoiceDTO> recentInvoices) { this.recentInvoices = recentInvoices; }
}
