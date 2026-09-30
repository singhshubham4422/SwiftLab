package com.example.invoiceapp.controller.api;

import com.example.invoiceapp.dto.ModeSwitchRequest;
import com.example.invoiceapp.dto.SyncBatchRequest;
import com.example.invoiceapp.dto.SyncBatchResponse;
import com.example.invoiceapp.dto.SyncQueueItemDTO;
import com.example.invoiceapp.dto.SyncStatusDTO;
import com.example.invoiceapp.model.enums.DataMode;
import com.example.invoiceapp.security.AppUserPrincipal;
import com.example.invoiceapp.service.AppRuntimeService;
import com.example.invoiceapp.service.AuditLogService;
import com.example.invoiceapp.service.DeviceIdentityService;
import com.example.invoiceapp.service.SyncQueueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sync")
@Tag(name = "Synchronization & Mode", description = "Dual data modes (LOCAL_ONLY / CLOUD_SYNC) and synchronization APIs")
public class SyncApiController {

    private final SyncQueueService syncQueueService;
    private final AppRuntimeService appRuntimeService;
    private final AuditLogService auditLogService;
    private final DeviceIdentityService deviceIdentityService;

    public SyncApiController(SyncQueueService syncQueueService,
                             AppRuntimeService appRuntimeService,
                             AuditLogService auditLogService,
                             DeviceIdentityService deviceIdentityService) {
        this.syncQueueService = syncQueueService;
        this.appRuntimeService = appRuntimeService;
        this.auditLogService = auditLogService;
        this.deviceIdentityService = deviceIdentityService;
    }

    @GetMapping("/status")
    @Operation(summary = "Get current application data mode and synchronization status")
    public ResponseEntity<SyncStatusDTO> getStatus() {
        return ResponseEntity.ok(appRuntimeService.getSyncStatus());
    }

    @PostMapping("/mode")
    @Operation(summary = "Safely switch data mode (requires explicit confirmation to enable Cloud Sync)")
    public ResponseEntity<?> switchMode(@AuthenticationPrincipal AppUserPrincipal principal,
                                        @Valid @RequestBody ModeSwitchRequest req) {
        Long orgId = principal != null ? principal.getOrganizationId() : null;
        Long userId = principal != null ? principal.getUserId() : null;

        try {
            if (req.getTargetMode() == DataMode.CLOUD_SYNC) {
                appRuntimeService.switchToCloudSync(req.isConfirm());
                auditLogService.record(
                        orgId,
                        userId,
                        deviceIdentityService.localDeviceId(),
                        "SWITCH_MODE_CLOUD_SYNC",
                        "AppRuntime",
                        "1",
                        "Operator confirmed switch to CLOUD_SYNC mode"
                );
            } else {
                appRuntimeService.switchToLocalOnly();
                auditLogService.record(
                        orgId,
                        userId,
                        deviceIdentityService.localDeviceId(),
                        "SWITCH_MODE_LOCAL_ONLY",
                        "AppRuntime",
                        "1",
                        "Switched to LOCAL_ONLY mode (cloud uploads disabled)"
                );
            }
            return ResponseEntity.ok(appRuntimeService.getSyncStatus());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/queue")
    @Operation(summary = "List sync queue items")
    public ResponseEntity<List<SyncQueueItemDTO>> getQueue(@AuthenticationPrincipal AppUserPrincipal principal) {
        if (principal == null || principal.getOrganizationId() == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(syncQueueService.listQueue(principal.getOrganizationId()));
    }

    @PostMapping("/batch")
    @Operation(summary = "Inbound batch synchronization endpoint (for cloud receiver or peer sync)")
    public ResponseEntity<SyncBatchResponse> receiveBatch(@AuthenticationPrincipal AppUserPrincipal principal,
                                                          @RequestBody SyncBatchRequest batch) {
        Long orgId = principal != null ? principal.getOrganizationId() : null;
        if (orgId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        SyncBatchResponse response = syncQueueService.processIncomingBatch(orgId, batch);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/trigger")
    @Operation(summary = "Manually trigger background sync processing")
    public ResponseEntity<?> triggerSync() {
        if (appRuntimeService.isLocalOnly()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Cannot process sync queue while in LOCAL_ONLY mode."));
        }
        int processed = syncQueueService.processPendingQueue();
        return ResponseEntity.ok(Map.of("processedCount", processed, "status", appRuntimeService.getSyncStatus()));
    }
}
