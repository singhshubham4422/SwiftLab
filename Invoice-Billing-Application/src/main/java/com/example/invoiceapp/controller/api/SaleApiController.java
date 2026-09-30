package com.example.invoiceapp.controller.api;

import com.example.invoiceapp.dto.SaleCreateRequest;
import com.example.invoiceapp.dto.SaleDTO;
import com.example.invoiceapp.model.Sale;
import com.example.invoiceapp.security.AccessPolicy;
import com.example.invoiceapp.security.AppUserPrincipal;
import com.example.invoiceapp.service.AuditLogService;
import com.example.invoiceapp.service.DeviceIdentityService;
import com.example.invoiceapp.service.SaleService;
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
@RequestMapping("/api/sales")
@Tag(name = "Sales", description = "Point of sale, stock depletion, and auto-invoicing APIs")
public class SaleApiController {

    private final SaleService saleService;
    private final AuditLogService auditLogService;
    private final DeviceIdentityService deviceIdentityService;

    public SaleApiController(SaleService saleService,
                             AuditLogService auditLogService,
                             DeviceIdentityService deviceIdentityService) {
        this.saleService = saleService;
        this.auditLogService = auditLogService;
        this.deviceIdentityService = deviceIdentityService;
    }

    @GetMapping
    @Operation(summary = "List all sales in organization")
    public ResponseEntity<List<SaleDTO>> list(@AuthenticationPrincipal AppUserPrincipal principal) {
        if (principal == null || principal.getOrganizationId() == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(saleService.listByOrganization(principal.getOrganizationId()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get sale details by ID")
    public ResponseEntity<?> get(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable Long id) {
        if (principal == null || principal.getOrganizationId() == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Sale s = saleService.get(id, principal.getOrganizationId());
        return ResponseEntity.ok(s);
    }

    @PostMapping
    @Operation(summary = "Create sale, deplete stock, generate invoice, and optional payment")
    public ResponseEntity<?> create(@AuthenticationPrincipal AppUserPrincipal principal,
                                    @Valid @RequestBody SaleCreateRequest req) {
        if (principal == null || principal.getOrganizationId() == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!AccessPolicy.canSales(principal.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Insufficient permissions for sales"));
        }

        try {
            Sale saved = saleService.createSale(principal.getOrganizationId(), principal.getUserId(), req);
            auditLogService.record(
                    principal.getOrganizationId(),
                    principal.getUserId(),
                    deviceIdentityService.localDeviceId(),
                    "CREATE_SALE",
                    "Sale",
                    saved.getId().toString(),
                    "Created Sale #" + saved.getSaleNumber() + " (Total: " + saved.getTotal() + ")"
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete sale")
    public ResponseEntity<?> delete(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable Long id) {
        if (principal == null || principal.getOrganizationId() == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        saleService.delete(id, principal.getOrganizationId());
        return ResponseEntity.ok(Map.of("message", "Sale deleted successfully"));
    }
}
