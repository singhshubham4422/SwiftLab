package com.example.invoiceapp.dto;

public class SupplierDTO {
    private Long id;
    private String publicId;
    private String name;
    private String phone;
    private String email;
    private String address;
    private String taxNumber;
    private String notes;

    public SupplierDTO() {}

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
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
