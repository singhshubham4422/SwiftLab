package com.example.invoiceapp.model;

import jakarta.persistence.*;

@Entity
@Table(name = "brands")
public class Brand extends SyncedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
