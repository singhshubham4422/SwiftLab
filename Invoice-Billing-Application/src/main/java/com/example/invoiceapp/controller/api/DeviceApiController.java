package com.example.invoiceapp.controller.api;

import com.example.invoiceapp.dto.DeviceDTO;
import com.example.invoiceapp.model.Device;
import com.example.invoiceapp.security.AppUserPrincipal;
import com.example.invoiceapp.service.DeviceIdentityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/devices")
@Tag(name = "Devices", description = "Device registration and multi-device organization tracking APIs")
public class DeviceApiController {

    private final DeviceIdentityService deviceIdentityService;

    public DeviceApiController(DeviceIdentityService deviceIdentityService) {
        this.deviceIdentityService = deviceIdentityService;
    }

    @GetMapping
    @Operation(summary = "List all devices registered to organization")
    public ResponseEntity<List<DeviceDTO>> list(@AuthenticationPrincipal AppUserPrincipal principal) {
        if (principal == null || principal.getOrganizationId() == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        List<Device> devices = deviceIdentityService.list(principal.getOrganizationId());
        return ResponseEntity.ok(devices.stream().map(d -> new DeviceDTO(
                d.getId(),
                d.getDeviceId(),
                d.getDeviceName(),
                d.getPlatform(),
                d.getLastSeenAt(),
                d.isActive(),
                d.getCreatedAt()
        )).collect(Collectors.toList()));
    }

    @PostMapping("/register")
    @Operation(summary = "Register or touch local device for current organization")
    public ResponseEntity<DeviceDTO> register(@AuthenticationPrincipal AppUserPrincipal principal) {
        if (principal == null || principal.getOrganizationId() == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Device d = deviceIdentityService.registerForOrganization(principal.getOrganizationId());
        return ResponseEntity.ok(new DeviceDTO(
                d.getId(),
                d.getDeviceId(),
                d.getDeviceName(),
                d.getPlatform(),
                d.getLastSeenAt(),
                d.isActive(),
                d.getCreatedAt()
        ));
    }
}
