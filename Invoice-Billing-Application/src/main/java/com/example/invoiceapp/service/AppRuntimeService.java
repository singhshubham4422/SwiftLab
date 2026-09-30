package com.example.invoiceapp.service;

import com.example.invoiceapp.dto.SyncStatusDTO;
import com.example.invoiceapp.model.AppRuntime;
import com.example.invoiceapp.model.enums.DataMode;
import com.example.invoiceapp.model.enums.SyncStatus;
import com.example.invoiceapp.repository.AppRuntimeRepository;
import com.example.invoiceapp.repository.SyncQueueRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class AppRuntimeService {

    private final AppRuntimeRepository runtimeRepo;
    private final SyncQueueRepository syncQueueRepo;
    private final DeviceIdentityService deviceIdentityService;

    public AppRuntimeService(AppRuntimeRepository runtimeRepo,
                             SyncQueueRepository syncQueueRepo,
                             DeviceIdentityService deviceIdentityService) {
        this.runtimeRepo = runtimeRepo;
        this.syncQueueRepo = syncQueueRepo;
        this.deviceIdentityService = deviceIdentityService;
    }

    public synchronized AppRuntime getRuntime() {
        return runtimeRepo.findById(1L).orElseGet(() -> {
            AppRuntime rt = new AppRuntime();
            rt.setId(1L);
            rt.setDataMode(DataMode.LOCAL_ONLY);
            rt.setCloudOptInConfirmed(false);
            rt.setDisplayDeviceId(deviceIdentityService.localDeviceId());
            rt.setLastOnline(false);
            return runtimeRepo.save(rt);
        });
    }

    public DataMode getDataMode() {
        return getRuntime().getDataMode();
    }

    public boolean isLocalOnly() {
        return getDataMode() == DataMode.LOCAL_ONLY;
    }

    public boolean isCloudSync() {
        return getDataMode() == DataMode.CLOUD_SYNC;
    }

    @Transactional
    public AppRuntime switchToCloudSync(boolean confirmed) {
        if (!confirmed) {
            throw new IllegalArgumentException("Confirmation is strictly required to enable Cloud Synchronization mode.");
        }
        AppRuntime rt = getRuntime();
        rt.setDataMode(DataMode.CLOUD_SYNC);
        rt.setCloudOptInConfirmed(true);
        return runtimeRepo.save(rt);
    }

    @Transactional
    public AppRuntime switchToLocalOnly() {
        AppRuntime rt = getRuntime();
        rt.setDataMode(DataMode.LOCAL_ONLY);
        return runtimeRepo.save(rt);
    }

    @Transactional
    public void recordOnlineStatus(boolean online) {
        AppRuntime rt = getRuntime();
        rt.setLastOnline(online);
        runtimeRepo.save(rt);
    }

    @Transactional
    public void recordSyncCompletion() {
        AppRuntime rt = getRuntime();
        rt.setLastSyncAt(Instant.now());
        runtimeRepo.save(rt);
    }

    public SyncStatusDTO getSyncStatus() {
        AppRuntime rt = getRuntime();
        long pending = syncQueueRepo.countByStatus(SyncStatus.PENDING);
        return new SyncStatusDTO(
                rt.getDataMode(),
                rt.isLastOnline(),
                pending,
                rt.getLastSyncAt(),
                deviceIdentityService.localDeviceId()
        );
    }
}
