package com.example.invoiceapp.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "applied_sync_keys", uniqueConstraints = {
        @UniqueConstraint(name = "uk_applied_sync_key", columnNames = {"organization_id", "idempotency_key"})
})
public class AppliedSyncKey {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long organizationId;
    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;
    private Instant appliedAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrganizationId() { return organizationId; }
    public void setOrganizationId(Long organizationId) { this.organizationId = organizationId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public Instant getAppliedAt() { return appliedAt; }
    public void setAppliedAt(Instant appliedAt) { this.appliedAt = appliedAt; }
}
