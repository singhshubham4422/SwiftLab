package com.example.invoiceapp.controller.api;

import com.example.invoiceapp.dto.StockAdjustmentRequest;
import com.example.invoiceapp.dto.StockDTO;
import com.example.invoiceapp.dto.StockMovementDTO;
import com.example.invoiceapp.model.StockMovement;
import com.example.invoiceapp.security.AccessPolicy;
import com.example.invoiceapp.security.AppUserPrincipal;
import com.example.invoiceapp.service.AuditLogService;
import com.example.invoiceapp.service.DeviceIdentityService;
import com.example.invoiceapp.service.InventoryService;
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
@RequestMapping("/api")
@Tag(name = "Inventory & Movements", description = "Movement-based inventory management and tracking APIs")
public class InventoryApiController {

    private final InventoryService inventoryService;
    private final AuditLogService auditLogService;
    private final DeviceIdentityService deviceIdentityService;

    public InventoryApiController(InventoryService inventoryService,
                                  AuditLogService auditLogService,
                                  DeviceIdentityService deviceIdentityService) {
        this.inventoryService = inventoryService;
        this.auditLogService = auditLogService;
        this.deviceIdentityService = deviceIdentityService;
    }

    @GetMapping("/inventory")
    @Operation(summary = "List current stock levels by warehouse and product")
    public ResponseEntity<List<StockDTO>> getInventory(@AuthenticationPrincipal AppUserPrincipal principal) {
        if (principal == null || principal.getOrganizationId() == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(inventoryService.listStock(principal.getOrganizationId()));
    }

    @PostMapping("/inventory/adjust")
    @Operation(summary = "Record manual stock adjustment / stock in / stock out")
    public ResponseEntity<?> adjustStock(@AuthenticationPrincipal AppUserPrincipal principal,
                                         @Valid @RequestBody StockAdjustmentRequest req) {
        if (principal == null || principal.getOrganizationId() == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!AccessPolicy.canManageInventory(principal.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Insufficient permissions to adjust stock"));
        }

        try {
            StockMovement movement = inventoryService.adjustStock(principal.getOrganizationId(), req);
            auditLogService.record(
                    principal.getOrganizationId(),
                    principal.getUserId(),
                    deviceIdentityService.localDeviceId(),
                    "ADJUST_STOCK",
                    "StockMovement",
                    movement.getId().toString(),
                    "Adjusted product #" + req.getProductId() + " (" + req.getMovementType() + "): " + req.getQuantity()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(movement);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/stock-movements")
    @Operation(summary = "List auditable stock movements")
    public ResponseEntity<List<StockMovementDTO>> getStockMovements(@AuthenticationPrincipal AppUserPrincipal principal) {
        if (principal == null || principal.getOrganizationId() == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(inventoryService.listMovements(principal.getOrganizationId()));
    }
}
