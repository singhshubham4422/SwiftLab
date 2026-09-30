package com.example.invoiceapp.service;

import com.example.invoiceapp.config.AppProperties;
import com.example.invoiceapp.model.Device;
import com.example.invoiceapp.model.enums.DevicePlatform;
import com.example.invoiceapp.repository.DeviceRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class DeviceIdentityService {

    private static final Path DEVICE_FILE = Path.of("data", "device-id.txt");

    private final DeviceRepository deviceRepository;
    private final AppProperties appProperties;
    private volatile String cachedId;

    public DeviceIdentityService(DeviceRepository deviceRepository, AppProperties appProperties) {
        this.deviceRepository = deviceRepository;
        this.appProperties = appProperties;
    }

    public synchronized String localDeviceId() {
        if (cachedId != null) {
            return cachedId;
        }
        try {
            Files.createDirectories(DEVICE_FILE.getParent());
            if (Files.exists(DEVICE_FILE)) {
                String existing = Files.readString(DEVICE_FILE).trim();
                if (!existing.isBlank()) {
                    cachedId = existing;
                    return cachedId;
                }
            }
            String generated = "WIN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            Files.writeString(DEVICE_FILE, generated);
            cachedId = generated;
            return cachedId;
        } catch (IOException e) {
            cachedId = "WIN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            return cachedId;
        }
    }

    public Device registerForOrganization(Long organizationId) {
        String id = localDeviceId();
        Device device = deviceRepository.findByDeviceId(id).orElseGet(Device::new);
        device.setDeviceId(id);
        device.setOrganizationId(organizationId);
        device.setDeviceName(appProperties.getDevice().getName());
        device.setPlatform(DevicePlatform.WINDOWS);
        device.setActive(true);
        device.setLastSeenAt(Instant.now());
        return deviceRepository.save(device);
    }

    public void touch(Long organizationId) {
        if (organizationId == null) {
            return;
        }
        registerForOrganization(organizationId);
    }

    public List<Device> list(Long organizationId) {
        return deviceRepository.findByOrganizationIdOrderByLastSeenAtDesc(organizationId);
    }
}
