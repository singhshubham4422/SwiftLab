package com.example.invoiceapp.service;

import com.example.invoiceapp.config.AppProperties;
import com.example.invoiceapp.dto.SyncBatchRequest;
import com.example.invoiceapp.dto.SyncBatchResponse;
import com.example.invoiceapp.dto.SyncQueueItemDTO;
import com.example.invoiceapp.model.AppliedSyncKey;
import com.example.invoiceapp.model.SyncQueueItem;
import com.example.invoiceapp.model.enums.SyncOperation;
import com.example.invoiceapp.model.enums.SyncStatus;
import com.example.invoiceapp.repository.AppliedSyncKeyRepository;
import com.example.invoiceapp.repository.SyncQueueRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SyncQueueService {

    private static final Logger log = LoggerFactory.getLogger(SyncQueueService.class);

    private final SyncQueueRepository syncQueueRepo;
    private final AppliedSyncKeyRepository appliedSyncKeyRepo;
    private final AppRuntimeService appRuntimeService;
    private final DeviceIdentityService deviceIdentityService;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public SyncQueueService(SyncQueueRepository syncQueueRepo,
                            AppliedSyncKeyRepository appliedSyncKeyRepo,
                            @Lazy AppRuntimeService appRuntimeService,
                            DeviceIdentityService deviceIdentityService,
                            AppProperties appProperties) {
        this.syncQueueRepo = syncQueueRepo;
        this.appliedSyncKeyRepo = appliedSyncKeyRepo;
        this.appRuntimeService = appRuntimeService;
        this.deviceIdentityService = deviceIdentityService;
        this.appProperties = appProperties;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Transactional
    public void enqueue(Long organizationId,
                        String entityType,
                        String entityPublicId,
                        SyncOperation operation,
                        Object payloadObject,
                        String idempotencyKey) {
        // Enforce Local-Only constraint: Never queue sync items in LOCAL_ONLY mode
        if (appRuntimeService.isLocalOnly()) {
            return;
        }

        try {
            String deviceId = deviceIdentityService.localDeviceId();
            String key = (idempotencyKey != null && !idempotencyKey.isBlank())
                    ? idempotencyKey
                    : deviceId + ":" + entityType + ":" + entityPublicId + ":" + operation.name() + ":" + Instant.now().toEpochMilli();

            // Prevent duplicate enqueuing
            if (syncQueueRepo.findByIdempotencyKey(key).isPresent()) {
                return;
            }

            SyncQueueItem item = new SyncQueueItem();
            item.setOrganizationId(organizationId);
            item.setDeviceId(deviceId);
            item.setEntityType(entityType);
            item.setEntityPublicId(entityPublicId);
            item.setOperation(operation);
            item.setPayload(payloadObject != null ? objectMapper.writeValueAsString(payloadObject) : "{}");
            item.setIdempotencyKey(key);
            item.setCreatedAt(Instant.now());
            item.setStatus(SyncStatus.PENDING);
            item.setRetryCount(0);

            syncQueueRepo.save(item);
        } catch (Exception e) {
            log.error("Failed to enqueue sync item for {} {}: {}", entityType, entityPublicId, e.getMessage());
        }
    }

    public List<SyncQueueItemDTO> listQueue(Long organizationId) {
        if (organizationId == null) return List.of();
        return syncQueueRepo.findByOrganizationIdOrderByCreatedAtDesc(organizationId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Scheduled(fixedDelay = 30000)
    public void scheduledSyncProcessor() {
        if (appRuntimeService.isLocalOnly()) {
            return;
        }
        processPendingQueue();
    }

    @Transactional
    public synchronized int processPendingQueue() {
        if (appRuntimeService.isLocalOnly()) {
            return 0;
        }

        String cloudApiUrl = appProperties.getCloud().getApiUrl();
        if (cloudApiUrl == null || cloudApiUrl.isBlank()) {
            return 0;
        }

        List<SyncQueueItem> pending = syncQueueRepo.findByStatusInOrderByCreatedAtAsc(List.of(SyncStatus.PENDING, SyncStatus.FAILED));
        if (pending.isEmpty()) {
            return 0;
        }

        int processed = 0;
        String deviceId = deviceIdentityService.localDeviceId();

        try {
            List<SyncQueueItemDTO> dtoList = pending.stream().map(this::toDTO).collect(Collectors.toList());
            SyncBatchRequest batchRequest = new SyncBatchRequest(deviceId, dtoList);
            String jsonBody = objectMapper.writeValueAsString(batchRequest);

            String endpoint = cloudApiUrl.replaceAll("/+$", "") + "/api/sync/batch";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .header("X-Device-Id", deviceId)
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                appRuntimeService.recordOnlineStatus(true);
                appRuntimeService.recordSyncCompletion();

                for (SyncQueueItem item : pending) {
                    item.setStatus(SyncStatus.SYNCED);
                    item.setLastAttemptAt(Instant.now());
                    item.setErrorMessage(null);
                    syncQueueRepo.save(item);
                    processed++;
                }
                log.info("Successfully synced {} items to cloud API.", processed);
            } else {
                appRuntimeService.recordOnlineStatus(false);
                for (SyncQueueItem item : pending) {
                    item.setStatus(SyncStatus.FAILED);
                    item.setRetryCount(item.getRetryCount() + 1);
                    item.setLastAttemptAt(Instant.now());
                    item.setErrorMessage("HTTP " + response.statusCode() + ": " + response.body());
                    syncQueueRepo.save(item);
                }
            }
        } catch (Exception e) {
            appRuntimeService.recordOnlineStatus(false);
            log.info("Cloud sync offline or unreachable: {}. Local queue preserved.", e.getMessage());
            for (SyncQueueItem item : pending) {
                item.setLastAttemptAt(Instant.now());
                item.setErrorMessage("Network error: " + e.getMessage());
                syncQueueRepo.save(item);
            }
        }

        return processed;
    }

    @Transactional
    public SyncBatchResponse processIncomingBatch(Long organizationId, SyncBatchRequest batch) {
        if (batch == null || batch.getItems() == null) {
            return new SyncBatchResponse(0, 0, List.of(), List.of());
        }

        int processed = 0;
        int failed = 0;
        List<String> appliedKeys = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (SyncQueueItemDTO item : batch.getItems()) {
            String key = item.getIdempotencyKey();
            if (key != null && appliedSyncKeyRepo.existsByOrganizationIdAndIdempotencyKey(organizationId, key)) {
                // Already applied idempotently
                appliedKeys.add(key);
                continue;
            }

            try {
                // Record the applied idempotency key to prevent duplicate replays
                if (key != null && !key.isBlank()) {
                    AppliedSyncKey applied = new AppliedSyncKey();
                    applied.setOrganizationId(organizationId);
                    applied.setIdempotencyKey(key);
                    applied.setAppliedAt(Instant.now());
                    appliedSyncKeyRepo.save(applied);
                    appliedKeys.add(key);
                }
                processed++;
            } catch (Exception e) {
                failed++;
                errors.add("Item " + item.getIdempotencyKey() + ": " + e.getMessage());
            }
        }

        return new SyncBatchResponse(processed, failed, appliedKeys, errors);
    }

    public SyncQueueItemDTO toDTO(SyncQueueItem item) {
        if (item == null) return null;
        SyncQueueItemDTO dto = new SyncQueueItemDTO();
        dto.setId(item.getId());
        dto.setDeviceId(item.getDeviceId());
        dto.setEntityType(item.getEntityType());
        dto.setEntityPublicId(item.getEntityPublicId());
        dto.setOperation(item.getOperation());
        dto.setPayload(item.getPayload());
        dto.setIdempotencyKey(item.getIdempotencyKey());
        dto.setCreatedAt(item.getCreatedAt());
        dto.setRetryCount(item.getRetryCount());
        dto.setStatus(item.getStatus());
        dto.setLastAttemptAt(item.getLastAttemptAt());
        dto.setErrorMessage(item.getErrorMessage());
        return dto;
    }
}
