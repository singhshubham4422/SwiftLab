package com.example.invoiceapp.dto;

public class UnitDTO {
    private Long id;
    private String publicId;
    private String name;
    private String abbreviation;

    public UnitDTO() {}

    public UnitDTO(Long id, String publicId, String name, String abbreviation) {
        this.id = id;
        this.publicId = publicId;
        this.name = name;
        this.abbreviation = abbreviation;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPublicId() { return publicId; }
    public void setPublicId(String publicId) { this.publicId = publicId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAbbreviation() { return abbreviation; }
    public void setAbbreviation(String abbreviation) { this.abbreviation = abbreviation; }
}
