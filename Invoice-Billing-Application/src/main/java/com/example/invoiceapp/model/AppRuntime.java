package com.example.invoiceapp.model;

import com.example.invoiceapp.model.enums.DataMode;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "app_runtime")
public class AppRuntime {
    @Id
    private Long id = 1L;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DataMode dataMode = DataMode.LOCAL_ONLY;

    private boolean cloudOptInConfirmed = false;
    private Instant lastSyncAt;
    private boolean lastOnline = false;
    private String displayDeviceId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public DataMode getDataMode() { return dataMode; }
    public void setDataMode(DataMode dataMode) { this.dataMode = dataMode; }
    public boolean isCloudOptInConfirmed() { return cloudOptInConfirmed; }
    public void setCloudOptInConfirmed(boolean cloudOptInConfirmed) { this.cloudOptInConfirmed = cloudOptInConfirmed; }
    public Instant getLastSyncAt() { return lastSyncAt; }
    public void setLastSyncAt(Instant lastSyncAt) { this.lastSyncAt = lastSyncAt; }
    public boolean isLastOnline() { return lastOnline; }
    public void setLastOnline(boolean lastOnline) { this.lastOnline = lastOnline; }
    public String getDisplayDeviceId() { return displayDeviceId; }
    public void setDisplayDeviceId(String displayDeviceId) { this.displayDeviceId = displayDeviceId; }
}
