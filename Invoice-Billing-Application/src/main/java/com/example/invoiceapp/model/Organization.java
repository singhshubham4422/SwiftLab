package com.example.invoiceapp.model;

import com.example.invoiceapp.model.enums.DataMode;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "organizations")
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 36)
    private String publicId = UUID.randomUUID().toString();

    @Column(nullable = false)
    private String name;

    private boolean allowNegativeInventory = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DataMode preferredDataMode = DataMode.LOCAL_ONLY;

    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

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

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
