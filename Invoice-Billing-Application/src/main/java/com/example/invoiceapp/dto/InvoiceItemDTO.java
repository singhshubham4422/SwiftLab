package com.example.invoiceapp.dto;

public class InvoiceItemDTO {
    private Long id;
    private String description;
    private int quantity = 1;
    private double unitPrice = 0.0;
    private double total = 0.0;

    public InvoiceItemDTO() {}

    public InvoiceItemDTO(String description, int quantity, double unitPrice) {
        this.description = description;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.total = quantity * unitPrice;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }
    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }
}
