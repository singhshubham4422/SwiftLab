package com.example.invoiceapp.dto;

public class WarehouseDTO {
    private Long id;
    private String publicId;
    private String name;
    private String location;
    private boolean defaultWarehouse;

    public WarehouseDTO() {}

    public WarehouseDTO(Long id, String publicId, String name, String location, boolean defaultWarehouse) {
        this.id = id;
        this.publicId = publicId;
        this.name = name;
        this.location = location;
        this.defaultWarehouse = defaultWarehouse;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPublicId() { return publicId; }
    public void setPublicId(String publicId) { this.publicId = publicId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public boolean isDefaultWarehouse() { return defaultWarehouse; }
    public void setDefaultWarehouse(boolean defaultWarehouse) { this.defaultWarehouse = defaultWarehouse; }
}
