package com.example.invoiceapp.dto;

import com.example.invoiceapp.model.enums.DevicePlatform;

import java.time.Instant;

public class DeviceDTO {
    private Long id;
    private String deviceId;
    private String deviceName;
    private DevicePlatform platform;
    private Instant lastSeenAt;
    private boolean active;
    private Instant createdAt;

    public DeviceDTO() {}

    public DeviceDTO(Long id, String deviceId, String deviceName, DevicePlatform platform, Instant lastSeenAt, boolean active, Instant createdAt) {
        this.id = id;
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.platform = platform;
        this.lastSeenAt = lastSeenAt;
        this.active = active;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }
    public DevicePlatform getPlatform() { return platform; }
    public void setPlatform(DevicePlatform platform) { this.platform = platform; }
    public Instant getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(Instant lastSeenAt) { this.lastSeenAt = lastSeenAt; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
