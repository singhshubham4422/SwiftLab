package com.example.invoiceapp.dto;

public class BrandDTO {
    private Long id;
    private String publicId;
    private String name;

    public BrandDTO() {}

    public BrandDTO(Long id, String publicId, String name) {
        this.id = id;
        this.publicId = publicId;
        this.name = name;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPublicId() { return publicId; }
    public void setPublicId(String publicId) { this.publicId = publicId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
