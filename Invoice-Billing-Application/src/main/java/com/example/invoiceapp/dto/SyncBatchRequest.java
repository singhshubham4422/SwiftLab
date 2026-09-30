package com.example.invoiceapp.dto;

import java.util.ArrayList;
import java.util.List;

public class SyncBatchRequest {
    private String deviceId;
    private List<SyncQueueItemDTO> items = new ArrayList<>();

    public SyncBatchRequest() {}

    public SyncBatchRequest(String deviceId, List<SyncQueueItemDTO> items) {
        this.deviceId = deviceId;
        this.items = items;
    }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public List<SyncQueueItemDTO> getItems() { return items; }
    public void setItems(List<SyncQueueItemDTO> items) { this.items = items; }
}
