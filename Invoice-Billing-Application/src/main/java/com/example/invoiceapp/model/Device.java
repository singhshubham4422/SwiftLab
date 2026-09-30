package com.example.invoiceapp.model;

import com.example.invoiceapp.model.enums.DevicePlatform;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "devices")
public class Device {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 64)
    private String deviceId;
    private Long organizationId;
    private String deviceName;
    @Enumerated(EnumType.STRING)
    private DevicePlatform platform = DevicePlatform.WINDOWS;
    private Instant lastSeenAt;
    private boolean active = true;
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public Long getOrganizationId() { return organizationId; }
    public void setOrganizationId(Long organizationId) { this.organizationId = organizationId; }
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
