package com.example.invoiceapp.dto;

import com.example.invoiceapp.model.enums.DataMode;

public class OrganizationDTO {
    private Long id;
    private String publicId;
    private String name;
    private boolean allowNegativeInventory;
    private DataMode preferredDataMode;

    public OrganizationDTO() {}

    public OrganizationDTO(Long id, String publicId, String name, boolean allowNegativeInventory, DataMode preferredDataMode) {
        this.id = id;
        this.publicId = publicId;
        this.name = name;
        this.allowNegativeInventory = allowNegativeInventory;
        this.preferredDataMode = preferredDataMode;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPublicId() { return publicId; }
    public void setPublicId(String publicId) { this.publicId = publicId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isAllowNegativeInventory() { return allowNegativeInventory; }
    public void setAllowNegativeInventory(boolean allowNegativeInventory) { this.allowNegativeInventory = allowNegativeInventory; }
    public DataMode getPreferredDataMode() { return preferredDataMode; }
    public void setPreferredDataMode(DataMode preferredDataMode) { this.preferredDataMode = preferredDataMode; }
}
