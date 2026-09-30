package com.example.invoiceapp.dto;

import com.example.invoiceapp.model.enums.UserRole;

public class UserDTO {
    private Long id;
    private String email;
    private String fullName;
    private String organizationName;
    private Long organizationId;
    private UserRole role;
    private boolean active;

    public UserDTO() {}

    public UserDTO(Long id, String email, String fullName, String organizationName, Long organizationId, UserRole role, boolean active) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.organizationName = organizationName;
        this.organizationId = organizationId;
        this.role = role;
        this.active = active;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getOrganizationName() { return organizationName; }
    public void setOrganizationName(String organizationName) { this.organizationName = organizationName; }
    public Long getOrganizationId() { return organizationId; }
    public void setOrganizationId(Long organizationId) { this.organizationId = organizationId; }
    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
