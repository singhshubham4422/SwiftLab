package com.example.invoiceapp.dto;

import com.example.invoiceapp.model.enums.DataMode;

import java.time.Instant;

public class SyncStatusDTO {
    private DataMode dataMode;
    private boolean online;
    private long pendingQueueCount;
    private Instant lastSyncAt;
    private String localDeviceId;

    public SyncStatusDTO() {}

    public SyncStatusDTO(DataMode dataMode, boolean online, long pendingQueueCount, Instant lastSyncAt, String localDeviceId) {
        this.dataMode = dataMode;
        this.online = online;
        this.pendingQueueCount = pendingQueueCount;
        this.lastSyncAt = lastSyncAt;
        this.localDeviceId = localDeviceId;
    }

    public DataMode getDataMode() { return dataMode; }
    public void setDataMode(DataMode dataMode) { this.dataMode = dataMode; }
    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }
    public long getPendingQueueCount() { return pendingQueueCount; }
    public void setPendingQueueCount(long pendingQueueCount) { this.pendingQueueCount = pendingQueueCount; }
    public Instant getLastSyncAt() { return lastSyncAt; }
    public void setLastSyncAt(Instant lastSyncAt) { this.lastSyncAt = lastSyncAt; }
    public String getLocalDeviceId() { return localDeviceId; }
    public void setLocalDeviceId(String localDeviceId) { this.localDeviceId = localDeviceId; }
}
