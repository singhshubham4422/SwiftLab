package com.example.invoiceapp.controller.api;

import com.example.invoiceapp.dto.PurchaseCreateRequest;
import com.example.invoiceapp.dto.PurchaseDTO;
import com.example.invoiceapp.model.Purchase;
import com.example.invoiceapp.security.AccessPolicy;
import com.example.invoiceapp.security.AppUserPrincipal;
import com.example.invoiceapp.service.AuditLogService;
import com.example.invoiceapp.service.DeviceIdentityService;
import com.example.invoiceapp.service.PurchaseService;
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
@RequestMapping("/api/purchases")
@Tag(name = "Purchases", description = "Purchase orders and goods receipt APIs")
public class PurchaseApiController {

    private final PurchaseService purchaseService;
    private final AuditLogService auditLogService;
    private final DeviceIdentityService deviceIdentityService;

    public PurchaseApiController(PurchaseService purchaseService,
                                 AuditLogService auditLogService,
                                 DeviceIdentityService deviceIdentityService) {
        this.purchaseService = purchaseService;
        this.auditLogService = auditLogService;
        this.deviceIdentityService = deviceIdentityService;
    }

    @GetMapping
    @Operation(summary = "List all purchases for organization")
    public ResponseEntity<List<PurchaseDTO>> list(@AuthenticationPrincipal AppUserPrincipal principal) {
        if (principal == null || principal.getOrganizationId() == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(purchaseService.listByOrganization(principal.getOrganizationId()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get purchase by ID")
    public ResponseEntity<?> get(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable Long id) {
        if (principal == null || principal.getOrganizationId() == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Purchase p = purchaseService.get(id, principal.getOrganizationId());
        return ResponseEntity.ok(p);
    }

    @PostMapping
    @Operation(summary = "Create purchase order")
    public ResponseEntity<?> create(@AuthenticationPrincipal AppUserPrincipal principal,
                                    @Valid @RequestBody PurchaseCreateRequest req) {
        if (principal == null || principal.getOrganizationId() == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!AccessPolicy.canPurchases(principal.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Insufficient permissions for purchases"));
        }

        try {
            Purchase saved = purchaseService.createPurchase(principal.getOrganizationId(), req);
            auditLogService.record(
                    principal.getOrganizationId(),
                    principal.getUserId(),
                    deviceIdentityService.localDeviceId(),
                    "CREATE_PURCHASE",
                    "Purchase",
                    saved.getId().toString(),
                    "Created PO #" + saved.getPurchaseNumber() + " (Total: " + saved.getTotal() + ")"
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/receive")
    @Operation(summary = "Receive goods and increment inventory")
    public ResponseEntity<?> receive(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable Long id) {
        if (principal == null || principal.getOrganizationId() == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!AccessPolicy.canPurchases(principal.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Insufficient permissions"));
        }

        try {
            Purchase received = purchaseService.receiveGoods(id, principal.getOrganizationId());
            auditLogService.record(
                    principal.getOrganizationId(),
                    principal.getUserId(),
                    deviceIdentityService.localDeviceId(),
                    "RECEIVE_PURCHASE",
                    "Purchase",
                    received.getId().toString(),
                    "Goods received for PO #" + received.getPurchaseNumber()
            );
            return ResponseEntity.ok(received);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete purchase")
    public ResponseEntity<?> delete(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable Long id) {
        if (principal == null || principal.getOrganizationId() == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!AccessPolicy.canPurchases(principal.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Insufficient permissions"));
        }

        purchaseService.delete(id, principal.getOrganizationId());
        return ResponseEntity.ok(Map.of("message", "Purchase deleted successfully"));
    }
}
