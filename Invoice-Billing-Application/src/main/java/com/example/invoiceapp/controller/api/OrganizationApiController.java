package com.example.invoiceapp.controller.api;

import com.example.invoiceapp.dto.OrganizationDTO;
import com.example.invoiceapp.model.Organization;
import com.example.invoiceapp.security.AccessPolicy;
import com.example.invoiceapp.security.AppUserPrincipal;
import com.example.invoiceapp.service.AuditLogService;
import com.example.invoiceapp.service.DeviceIdentityService;
import com.example.invoiceapp.service.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/organizations")
@Tag(name = "Organizations", description = "Organization management APIs")
public class OrganizationApiController {

    private final OrganizationService orgService;
    private final AuditLogService auditLogService;
    private final DeviceIdentityService deviceIdentityService;

    public OrganizationApiController(OrganizationService orgService,
                                     AuditLogService auditLogService,
                                     DeviceIdentityService deviceIdentityService) {
        this.orgService = orgService;
        this.auditLogService = auditLogService;
        this.deviceIdentityService = deviceIdentityService;
    }

    @GetMapping("/current")
    @Operation(summary = "Get current organization details")
    public ResponseEntity<?> getCurrent(@AuthenticationPrincipal AppUserPrincipal principal) {
        if (principal == null || principal.getOrganizationId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Organization org = orgService.get(principal.getOrganizationId());
        return ResponseEntity.ok(orgService.toDTO(org));
    }

    @PutMapping("/current")
    @Operation(summary = "Update current organization settings")
    public ResponseEntity<?> updateCurrent(@AuthenticationPrincipal AppUserPrincipal principal,
                                           @RequestBody Map<String, Object> body) {
        if (principal == null || principal.getOrganizationId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!AccessPolicy.canOrgSettings(principal.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Only OWNER and ADMIN can modify organization settings."));
        }

        String name = (String) body.get("name");
        Boolean allowNegative = (Boolean) body.get("allowNegativeInventory");

        Organization updated = orgService.updateOrganization(
                principal.getOrganizationId(),
                name,
                allowNegative != null ? allowNegative : false
        );

        auditLogService.record(
                principal.getOrganizationId(),
                principal.getUserId(),
                deviceIdentityService.localDeviceId(),
                "UPDATE_ORG_SETTINGS",
                "Organization",
                updated.getId().toString(),
                "Updated settings: name=" + name + ", allowNegative=" + allowNegative
        );

        return ResponseEntity.ok(orgService.toDTO(updated));
    }
}
