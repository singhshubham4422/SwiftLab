package com.example.invoiceapp.model;

import com.example.invoiceapp.model.enums.SyncOperation;
import com.example.invoiceapp.model.enums.SyncStatus;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "sync_queue")
public class SyncQueueItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long organizationId;
    private String deviceId;
    private String entityType;
    private String entityPublicId;
    @Enumerated(EnumType.STRING)
    private SyncOperation operation;
    @Lob
    private String payload;
    private String idempotencyKey;
    private Instant createdAt = Instant.now();
    private int retryCount = 0;
    @Enumerated(EnumType.STRING)
    private SyncStatus status = SyncStatus.PENDING;
    private Instant lastAttemptAt;
    @Column(length = 2000)
    private String errorMessage;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrganizationId() { return organizationId; }
    public void setOrganizationId(Long organizationId) { this.organizationId = organizationId; }
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public String getEntityPublicId() { return entityPublicId; }
    public void setEntityPublicId(String entityPublicId) { this.entityPublicId = entityPublicId; }
    public SyncOperation getOperation() { return operation; }
    public void setOperation(SyncOperation operation) { this.operation = operation; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }
    public SyncStatus getStatus() { return status; }
    public void setStatus(SyncStatus status) { this.status = status; }
    public Instant getLastAttemptAt() { return lastAttemptAt; }
    public void setLastAttemptAt(Instant lastAttemptAt) { this.lastAttemptAt = lastAttemptAt; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
