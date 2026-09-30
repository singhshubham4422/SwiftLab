package com.example.invoiceapp.dto;

import java.math.BigDecimal;

public class CustomerDTO {
    private Long id;
    private String publicId;
    private String name;
    private String phone;
    private String email;
    private String address;
    private String taxNumber;
    private BigDecimal creditLimit = BigDecimal.ZERO;
    private String notes;

    public CustomerDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPublicId() { return publicId; }
    public void setPublicId(String publicId) { this.publicId = publicId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getTaxNumber() { return taxNumber; }
    public void setTaxNumber(String taxNumber) { this.taxNumber = taxNumber; }
    public BigDecimal getCreditLimit() { return creditLimit; }
    public void setCreditLimit(BigDecimal creditLimit) { this.creditLimit = creditLimit; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
