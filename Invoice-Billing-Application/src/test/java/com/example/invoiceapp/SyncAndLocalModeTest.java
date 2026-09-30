package com.example.invoiceapp;

import com.example.invoiceapp.dto.SyncBatchRequest;
import com.example.invoiceapp.dto.SyncBatchResponse;
import com.example.invoiceapp.dto.SyncQueueItemDTO;
import com.example.invoiceapp.model.*;
import com.example.invoiceapp.model.enums.DataMode;
import com.example.invoiceapp.model.enums.SyncOperation;
import com.example.invoiceapp.model.enums.SyncStatus;
import com.example.invoiceapp.service.AppRuntimeService;
import com.example.invoiceapp.service.SyncQueueService;
import com.example.invoiceapp.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class SyncAndLocalModeTest {

    @Autowired
    private UserService userService;

    @Autowired
    private AppRuntimeService appRuntimeService;

    @Autowired
    private SyncQueueService syncQueueService;

    @Test
    @DisplayName("Local Only Mode blocks sync queueing; explicit confirmation required for mode switch; idempotency check")
    void testSyncQueueAndDataModes() {
        User user = userService.register("sync_admin@test.com", "pass", "Sync Admin", "Sync Labs", "$", "USD", "", "", "");
        Long orgId = user.getOrganizationId();

        // 1. Initial Mode should be LOCAL_ONLY
        appRuntimeService.switchToLocalOnly();
        assertTrue(appRuntimeService.isLocalOnly());
        assertFalse(appRuntimeService.isCloudSync());
        assertEquals(DataMode.LOCAL_ONLY, appRuntimeService.getDataMode());

        // In LOCAL_ONLY mode, enqueue operations must NOT create queue items
        syncQueueService.enqueue(orgId, "Product", "PUB-101", SyncOperation.CREATE, "{\"name\":\"Widget\"}", "KEY-TEST-001");
        List<SyncQueueItemDTO> localQueue = syncQueueService.listQueue(orgId);
        assertTrue(localQueue.isEmpty(), "Local only mode must never enqueue sync operations");

        // 2. Switching to CLOUD_SYNC without confirmation must fail
        assertThrows(IllegalArgumentException.class, () -> 
            appRuntimeService.switchToCloudSync(false),
            "Switching to CLOUD_SYNC without confirmation must throw IllegalArgumentException");

        // 3. Switch to CLOUD_SYNC with explicit confirmation
        appRuntimeService.switchToCloudSync(true);
        assertTrue(appRuntimeService.isCloudSync());
        assertFalse(appRuntimeService.isLocalOnly());
        assertEquals(DataMode.CLOUD_SYNC, appRuntimeService.getDataMode());

        // In CLOUD_SYNC mode, enqueue operations must create queue items
        syncQueueService.enqueue(orgId, "Product", "PUB-101", SyncOperation.CREATE, "{\"name\":\"Widget\"}", "KEY-TEST-002");
        List<SyncQueueItemDTO> cloudQueue = syncQueueService.listQueue(orgId);
        assertEquals(1, cloudQueue.size());
        assertEquals(SyncStatus.PENDING, cloudQueue.get(0).getStatus());

        // 4. Test Idempotency: receiving the same sync item twice
        SyncQueueItemDTO syncItem = new SyncQueueItemDTO();
        syncItem.setId(999L);
        syncItem.setEntityType("Sale");
        syncItem.setEntityPublicId("SALE-PUB-55");
        syncItem.setOperation(SyncOperation.CREATE);
        syncItem.setPayload("{\"amount\":100}");
        syncItem.setIdempotencyKey("KEY-IDEMPOTENT-XYZ");

        SyncBatchRequest batch = new SyncBatchRequest();
        batch.setDeviceId("DEVICE-ABC");
        batch.setItems(List.of(syncItem));

        // First push: processed
        SyncBatchResponse resp1 = syncQueueService.processIncomingBatch(orgId, batch);
        assertEquals(1, resp1.getProcessedCount());
        assertEquals(0, resp1.getFailedCount());
        assertTrue(resp1.getAppliedKeys().contains("KEY-IDEMPOTENT-XYZ"));

        // Second push of identical batch: must detect duplicate via AppliedSyncKey and not re-execute!
        SyncBatchResponse resp2 = syncQueueService.processIncomingBatch(orgId, batch);
        assertEquals(0, resp2.getProcessedCount(), "Identical sync operation must not be processed again");
        assertEquals(0, resp2.getFailedCount());
        assertTrue(resp2.getAppliedKeys().contains("KEY-IDEMPOTENT-XYZ"));
    }
}
