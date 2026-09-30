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

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    private String fullName;
    private String organizationName;

    private Long organizationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role = UserRole.OWNER;

    private String supabaseAuthId;

    private boolean active = true;

    private Instant lastLoginAt;

    private ZonedDateTime createdAt;

    public User() {
        this.createdAt = ZonedDateTime.now();
    }

    public User(String email, String password, String fullName, String organizationName) {
        this.email = email;
        this.password = password;
        this.fullName = fullName;
        this.organizationName = organizationName;
        this.createdAt = ZonedDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getOrganizationName() { return organizationName; }
    public void setOrganizationName(String organizationName) { this.organizationName = organizationName; }

    public Long getOrganizationId() { return organizationId; }
    public void setOrganizationId(Long organizationId) { this.organizationId = organizationId; }

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
}
