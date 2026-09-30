package com.example.invoiceapp.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "customers")
public class Customer extends SyncedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;
    private String phone;
    private String email;
    @Column(length = 2000)
    private String address;
    private String taxNumber;
    private BigDecimal creditLimit = BigDecimal.ZERO;
    @Column(length = 2000)
    private String notes;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
