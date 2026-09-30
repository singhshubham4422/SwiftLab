package com.example.invoiceapp.dto;

import com.example.invoiceapp.model.enums.SyncOperation;
import com.example.invoiceapp.model.enums.SyncStatus;

import java.time.Instant;

public class SyncQueueItemDTO {
    private Long id;
    private String deviceId;
    private String entityType;
    private String entityPublicId;
    private SyncOperation operation;
    private String payload;
    private String idempotencyKey;
    private Instant createdAt;
    private int retryCount;
    private SyncStatus status;
    private Instant lastAttemptAt;
    private String errorMessage;

    public SyncQueueItemDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
