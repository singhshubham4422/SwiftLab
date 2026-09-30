package com.example.invoiceapp.controller.api;

import com.example.invoiceapp.dto.SupplierDTO;
import com.example.invoiceapp.model.Supplier;
import com.example.invoiceapp.security.AppUserPrincipal;
import com.example.invoiceapp.service.AuditLogService;
import com.example.invoiceapp.service.DeviceIdentityService;
import com.example.invoiceapp.service.SupplierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/suppliers")
@Tag(name = "Suppliers", description = "Supplier vendor management APIs")
public class SupplierApiController {

    private final SupplierService supplierService;
    private final AuditLogService auditLogService;
    private final DeviceIdentityService deviceIdentityService;

    public SupplierApiController(SupplierService supplierService,
                                 AuditLogService auditLogService,
                                 DeviceIdentityService deviceIdentityService) {
        this.supplierService = supplierService;
        this.auditLogService = auditLogService;
        this.deviceIdentityService = deviceIdentityService;
    }

    @GetMapping
    @Operation(summary = "List all suppliers in organization")
    public ResponseEntity<List<SupplierDTO>> list(@AuthenticationPrincipal AppUserPrincipal principal) {
        if (principal == null || principal.getOrganizationId() == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(supplierService.listByOrganization(principal.getOrganizationId()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get supplier by ID")
    public ResponseEntity<?> get(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable Long id) {
        if (principal == null || principal.getOrganizationId() == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Supplier s = supplierService.get(id, principal.getOrganizationId());
        return ResponseEntity.ok(supplierService.toDTO(s));
    }

    @PostMapping
    @Operation(summary = "Create supplier")
    public ResponseEntity<?> create(@AuthenticationPrincipal AppUserPrincipal principal, @RequestBody SupplierDTO dto) {
        if (principal == null || principal.getOrganizationId() == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        dto.setId(null);
        Supplier saved = supplierService.save(principal.getOrganizationId(), dto);
        auditLogService.record(
                principal.getOrganizationId(),
                principal.getUserId(),
                deviceIdentityService.localDeviceId(),
                "CREATE_SUPPLIER",
                "Supplier",
                saved.getId().toString(),
                "Created supplier: " + saved.getName()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(supplierService.toDTO(saved));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update supplier")
    public ResponseEntity<?> update(@AuthenticationPrincipal AppUserPrincipal principal,
                                    @PathVariable Long id,
                                    @RequestBody SupplierDTO dto) {
        if (principal == null || principal.getOrganizationId() == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        dto.setId(id);
        Supplier saved = supplierService.save(principal.getOrganizationId(), dto);
        auditLogService.record(
                principal.getOrganizationId(),
                principal.getUserId(),
                deviceIdentityService.localDeviceId(),
                "UPDATE_SUPPLIER",
                "Supplier",
                saved.getId().toString(),
                "Updated supplier: " + saved.getName()
        );
        return ResponseEntity.ok(supplierService.toDTO(saved));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete supplier")
    public ResponseEntity<?> delete(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable Long id) {
        if (principal == null || principal.getOrganizationId() == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        supplierService.delete(id, principal.getOrganizationId());
        auditLogService.record(
                principal.getOrganizationId(),
                principal.getUserId(),
                deviceIdentityService.localDeviceId(),
                "DELETE_SUPPLIER",
                "Supplier",
                id.toString(),
                "Soft deleted supplier #" + id
        );
        return ResponseEntity.ok(Map.of("message", "Supplier deleted successfully"));
    }
}
