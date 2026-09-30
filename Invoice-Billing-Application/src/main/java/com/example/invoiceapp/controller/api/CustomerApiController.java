package com.example.invoiceapp.controller.api;

import com.example.invoiceapp.dto.CustomerDTO;
import com.example.invoiceapp.model.Customer;
import com.example.invoiceapp.security.AppUserPrincipal;
import com.example.invoiceapp.service.AuditLogService;
import com.example.invoiceapp.service.CustomerService;
import com.example.invoiceapp.service.DeviceIdentityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customers")
@Tag(name = "Customers", description = "Customer CRM and credit management APIs")
public class CustomerApiController {

    private final CustomerService customerService;
    private final AuditLogService auditLogService;
    private final DeviceIdentityService deviceIdentityService;

    public CustomerApiController(CustomerService customerService,
                                 AuditLogService auditLogService,
                                 DeviceIdentityService deviceIdentityService) {
        this.customerService = customerService;
        this.auditLogService = auditLogService;
        this.deviceIdentityService = deviceIdentityService;
    }

    @GetMapping
    @Operation(summary = "List all customers in organization")
    public ResponseEntity<List<CustomerDTO>> list(@AuthenticationPrincipal AppUserPrincipal principal) {
        if (principal == null || principal.getOrganizationId() == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(customerService.listByOrganization(principal.getOrganizationId()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get customer by ID")
    public ResponseEntity<?> get(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable Long id) {
        if (principal == null || principal.getOrganizationId() == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Customer c = customerService.get(id, principal.getOrganizationId());
        return ResponseEntity.ok(customerService.toDTO(c));
    }

    @PostMapping
    @Operation(summary = "Create customer")
    public ResponseEntity<?> create(@AuthenticationPrincipal AppUserPrincipal principal, @RequestBody CustomerDTO dto) {
        if (principal == null || principal.getOrganizationId() == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        dto.setId(null);
        Customer saved = customerService.save(principal.getOrganizationId(), dto);
        auditLogService.record(
                principal.getOrganizationId(),
                principal.getUserId(),
                deviceIdentityService.localDeviceId(),
                "CREATE_CUSTOMER",
                "Customer",
                saved.getId().toString(),
                "Created customer: " + saved.getName()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.toDTO(saved));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update customer")
    public ResponseEntity<?> update(@AuthenticationPrincipal AppUserPrincipal principal,
                                    @PathVariable Long id,
                                    @RequestBody CustomerDTO dto) {
        if (principal == null || principal.getOrganizationId() == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        dto.setId(id);
        Customer saved = customerService.save(principal.getOrganizationId(), dto);
        auditLogService.record(
                principal.getOrganizationId(),
                principal.getUserId(),
                deviceIdentityService.localDeviceId(),
                "UPDATE_CUSTOMER",
                "Customer",
                saved.getId().toString(),
                "Updated customer: " + saved.getName()
        );
        return ResponseEntity.ok(customerService.toDTO(saved));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete customer")
    public ResponseEntity<?> delete(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable Long id) {
        if (principal == null || principal.getOrganizationId() == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        customerService.delete(id, principal.getOrganizationId());
        auditLogService.record(
                principal.getOrganizationId(),
                principal.getUserId(),
                deviceIdentityService.localDeviceId(),
                "DELETE_CUSTOMER",
                "Customer",
                id.toString(),
                "Soft deleted customer #" + id
        );
        return ResponseEntity.ok(Map.of("message", "Customer deleted successfully"));
    }
}
