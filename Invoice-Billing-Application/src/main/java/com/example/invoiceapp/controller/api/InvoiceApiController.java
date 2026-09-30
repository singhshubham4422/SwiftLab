package com.example.invoiceapp.controller.api;

import com.example.invoiceapp.dto.InvoiceCreateRequest;
import com.example.invoiceapp.dto.InvoiceDTO;
import com.example.invoiceapp.model.Invoice;
import com.example.invoiceapp.security.AccessPolicy;
import com.example.invoiceapp.security.AppUserPrincipal;
import com.example.invoiceapp.service.AuditLogService;
import com.example.invoiceapp.service.DeviceIdentityService;
import com.example.invoiceapp.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/invoices")
@Tag(name = "Invoices", description = "Invoice lifecycle, status, PDF, and Excel export APIs")
public class InvoiceApiController {

    private final InvoiceService invoiceService;
    private final AuditLogService auditLogService;
    private final DeviceIdentityService deviceIdentityService;

    public InvoiceApiController(InvoiceService invoiceService,
                                AuditLogService auditLogService,
                                DeviceIdentityService deviceIdentityService) {
        this.invoiceService = invoiceService;
        this.auditLogService = auditLogService;
        this.deviceIdentityService = deviceIdentityService;
    }

    @GetMapping
    @Operation(summary = "List all invoices in organization")
    public ResponseEntity<List<InvoiceDTO>> list(@AuthenticationPrincipal AppUserPrincipal principal) {
        if (principal == null || principal.getOrganizationId() == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(invoiceService.listDTOByOrganization(principal.getOrganizationId()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get invoice by ID")
    public ResponseEntity<?> get(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable Long id) {
        if (principal == null || principal.getOrganizationId() == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Invoice inv = invoiceService.getForOrganization(id, principal.getOrganizationId());
        return ResponseEntity.ok(invoiceService.toDTO(inv));
    }

    @PostMapping
    @Operation(summary = "Create an invoice")
    public ResponseEntity<?> create(@AuthenticationPrincipal AppUserPrincipal principal,
                                    @RequestBody InvoiceCreateRequest req) {
        if (principal == null || principal.getOrganizationId() == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!AccessPolicy.canInvoices(principal.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Insufficient permissions"));
        }

        Invoice saved = invoiceService.createInvoice(principal.getOrganizationId(), principal.getUserId(), req);
        auditLogService.record(
                principal.getOrganizationId(),
                principal.getUserId(),
                deviceIdentityService.localDeviceId(),
                "CREATE_INVOICE",
                "Invoice",
                saved.getId().toString(),
                "Created invoice #" + saved.getInvoiceNumber() + " (Total: " + saved.getTotal() + ")"
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(invoiceService.toDTO(saved));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update invoice status")
    public ResponseEntity<?> updateStatus(@AuthenticationPrincipal AppUserPrincipal principal,
                                          @PathVariable Long id,
                                          @RequestBody Map<String, String> body) {
        if (principal == null || principal.getOrganizationId() == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Invoice inv = invoiceService.getForOrganization(id, principal.getOrganizationId());
        String status = body.get("status");
        if (status == null || status.isBlank()) return ResponseEntity.badRequest().body(Map.of("error", "status is required"));

        invoiceService.updateStatus(inv.getId(), status.toUpperCase());
        auditLogService.record(
                principal.getOrganizationId(),
                principal.getUserId(),
                deviceIdentityService.localDeviceId(),
                "UPDATE_INVOICE_STATUS",
                "Invoice",
                inv.getId().toString(),
                "Status updated to: " + status
        );
        return ResponseEntity.ok(Map.of("message", "Status updated to " + status));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete invoice")
    public ResponseEntity<?> delete(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable Long id) {
        if (principal == null || principal.getOrganizationId() == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Invoice inv = invoiceService.getForOrganization(id, principal.getOrganizationId());
        invoiceService.delete(inv.getId());
        auditLogService.record(
                principal.getOrganizationId(),
                principal.getUserId(),
                deviceIdentityService.localDeviceId(),
                "DELETE_INVOICE",
                "Invoice",
                id.toString(),
                "Soft deleted invoice #" + inv.getInvoiceNumber()
        );
        return ResponseEntity.ok(Map.of("message", "Invoice deleted successfully"));
    }

    @GetMapping("/{id}/pdf")
    @Operation(summary = "Download invoice PDF")
    public ResponseEntity<ByteArrayResource> downloadPdf(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable Long id) {
        if (principal == null || principal.getOrganizationId() == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            Invoice inv = invoiceService.getForOrganization(id, principal.getOrganizationId());
            byte[] data = invoiceService.exportPdf(inv);
            ByteArrayResource resource = new ByteArrayResource(data);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"invoice-" + inv.getInvoiceNumber() + ".pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(data.length)
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}/excel")
    @Operation(summary = "Download invoice Excel spreadsheet")
    public ResponseEntity<ByteArrayResource> downloadExcel(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable Long id) {
        if (principal == null || principal.getOrganizationId() == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            Invoice inv = invoiceService.getForOrganization(id, principal.getOrganizationId());
            byte[] data = invoiceService.exportExcel(inv);
            ByteArrayResource resource = new ByteArrayResource(data);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"invoice-" + inv.getInvoiceNumber() + ".xlsx\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .contentLength(data.length)
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
