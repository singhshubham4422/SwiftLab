package com.example.invoiceapp.model;

import com.example.invoiceapp.model.enums.UserRole;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.ZonedDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    private String fullName;
    private String organizationName;

    private Long organizationId;

    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role = UserRole.OWNER;

    private String supabaseAuthId;

    private boolean active = true;

    private Instant lastLoginAt;

    private ZonedDateTime createdAt;

    private Instant updatedAt;

    public User() {
        this.createdAt = ZonedDateTime.now();
        this.updatedAt = Instant.now();
    }

    public User(String email, String password, String fullName, String organizationName) {
        this.email = email;
        this.username = email;
        this.password = password;
        this.fullName = fullName;
        this.organizationName = organizationName;
        this.createdAt = ZonedDateTime.now();
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username != null ? username : email; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) {
        this.email = email;
        if (this.username == null || this.username.isBlank()) {
            this.username = email;
        }
    }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getOrganizationName() { return organizationName; }
    public void setOrganizationName(String organizationName) { this.organizationName = organizationName; }

    public Long getOrganizationId() { return organizationId; }
    public void setOrganizationId(Long organizationId) { this.organizationId = organizationId; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }

    public String getSupabaseAuthId() { return supabaseAuthId; }
    public void setSupabaseAuthId(String supabaseAuthId) { this.supabaseAuthId = supabaseAuthId; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Instant getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(Instant lastLoginAt) { this.lastLoginAt = lastLoginAt; }

    public ZonedDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(ZonedDateTime createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
