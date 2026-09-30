package com.example.invoiceapp.controller.api;

import com.example.invoiceapp.dto.WarehouseDTO;
import com.example.invoiceapp.model.Warehouse;
import com.example.invoiceapp.security.AccessPolicy;
import com.example.invoiceapp.security.AppUserPrincipal;
import com.example.invoiceapp.service.WarehouseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/warehouses")
@Tag(name = "Warehouses", description = "Warehouse storage location management APIs")
public class WarehouseApiController {

    private final WarehouseService warehouseService;

    public WarehouseApiController(WarehouseService warehouseService) {
        this.warehouseService = warehouseService;
    }

    @GetMapping
    @Operation(summary = "List all warehouses for the organization")
    public ResponseEntity<List<WarehouseDTO>> list(@AuthenticationPrincipal AppUserPrincipal principal) {
        if (principal == null || principal.getOrganizationId() == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(warehouseService.listByOrganization(principal.getOrganizationId()));
    }

    @PostMapping
    @Operation(summary = "Create a new warehouse")
    public ResponseEntity<?> create(@AuthenticationPrincipal AppUserPrincipal principal,
                                    @RequestBody Map<String, Object> body) {
        if (principal == null || principal.getOrganizationId() == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!AccessPolicy.canManageInventory(principal.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Insufficient permissions"));
        }

        String name = (String) body.get("name");
        String location = (String) body.get("location");
        Boolean isDefault = (Boolean) body.get("defaultWarehouse");

        if (name == null || name.isBlank()) return ResponseEntity.badRequest().body(Map.of("error", "Warehouse name is required"));

        Warehouse saved = warehouseService.create(principal.getOrganizationId(), name, location, isDefault != null && isDefault);
        return ResponseEntity.status(HttpStatus.CREATED).body(warehouseService.toDTO(saved));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete warehouse")
    public ResponseEntity<?> delete(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable Long id) {
        if (principal == null || principal.getOrganizationId() == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!AccessPolicy.canManageInventory(principal.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Insufficient permissions"));
        }

        warehouseService.delete(id, principal.getOrganizationId());
        return ResponseEntity.ok(Map.of("message", "Warehouse deleted successfully"));
    }
}
