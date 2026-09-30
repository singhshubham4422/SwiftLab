package com.example.invoiceapp.model;

import jakarta.persistence.*;

@Entity
@Table(name = "warehouses")
public class Warehouse extends SyncedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;
    private String location;
    private boolean defaultWarehouse = false;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public boolean isDefaultWarehouse() { return defaultWarehouse; }
    public void setDefaultWarehouse(boolean defaultWarehouse) { this.defaultWarehouse = defaultWarehouse; }
}
