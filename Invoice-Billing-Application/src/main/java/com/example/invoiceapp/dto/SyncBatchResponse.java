package com.example.invoiceapp.dto;

import java.util.ArrayList;
import java.util.List;

public class SyncBatchResponse {
    private int processedCount;
    private int failedCount;
    private List<String> appliedKeys = new ArrayList<>();
    private List<String> errors = new ArrayList<>();

    public SyncBatchResponse() {}

    public SyncBatchResponse(int processedCount, int failedCount, List<String> appliedKeys, List<String> errors) {
        this.processedCount = processedCount;
        this.failedCount = failedCount;
        this.appliedKeys = appliedKeys;
        this.errors = errors;
    }

    public int getProcessedCount() { return processedCount; }
    public void setProcessedCount(int processedCount) { this.processedCount = processedCount; }
    public int getFailedCount() { return failedCount; }
    public void setFailedCount(int failedCount) { this.failedCount = failedCount; }
    public List<String> getAppliedKeys() { return appliedKeys; }
    public void setAppliedKeys(List<String> appliedKeys) { this.appliedKeys = appliedKeys; }
    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }
}
