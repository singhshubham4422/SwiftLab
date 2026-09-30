package com.example.invoiceapp.dto;

public class CategoryDTO {
    private Long id;
    private String publicId;
    private String name;
    private String description;

    public CategoryDTO() {}

    public CategoryDTO(Long id, String publicId, String name, String description) {
        this.id = id;
        this.publicId = publicId;
        this.name = name;
        this.description = description;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPublicId() { return publicId; }
    public void setPublicId(String publicId) { this.publicId = publicId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
