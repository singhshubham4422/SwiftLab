package com.example.invoiceapp.controller.api;

import com.example.invoiceapp.dto.PaymentCreateRequest;
import com.example.invoiceapp.dto.PaymentDTO;
import com.example.invoiceapp.model.Payment;
import com.example.invoiceapp.security.AccessPolicy;
import com.example.invoiceapp.security.AppUserPrincipal;
import com.example.invoiceapp.service.AuditLogService;
import com.example.invoiceapp.service.DeviceIdentityService;
import com.example.invoiceapp.service.PaymentService;
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
@RequestMapping("/api/payments")
@Tag(name = "Payments", description = "Invoice payment recording and settlement APIs")
public class PaymentApiController {

    private final PaymentService paymentService;
    private final AuditLogService auditLogService;
    private final DeviceIdentityService deviceIdentityService;

    public PaymentApiController(PaymentService paymentService,
                                AuditLogService auditLogService,
                                DeviceIdentityService deviceIdentityService) {
        this.paymentService = paymentService;
        this.auditLogService = auditLogService;
        this.deviceIdentityService = deviceIdentityService;
    }

    @GetMapping
    @Operation(summary = "List all payments for organization")
    public ResponseEntity<List<PaymentDTO>> list(@AuthenticationPrincipal AppUserPrincipal principal) {
        if (principal == null || principal.getOrganizationId() == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(paymentService.listByOrganization(principal.getOrganizationId()));
    }

    @GetMapping("/by-invoice/{invoiceId}")
    @Operation(summary = "List payments recorded against an invoice")
    public ResponseEntity<List<PaymentDTO>> listByInvoice(@PathVariable Long invoiceId) {
        return ResponseEntity.ok(paymentService.listByInvoice(invoiceId));
    }

    @PostMapping
    @Operation(summary = "Record payment against invoice")
    public ResponseEntity<?> create(@AuthenticationPrincipal AppUserPrincipal principal,
                                    @Valid @RequestBody PaymentCreateRequest req) {
        if (principal == null || principal.getOrganizationId() == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!AccessPolicy.canInvoices(principal.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Insufficient permissions"));
        }

        try {
            Payment saved = paymentService.recordPayment(principal.getOrganizationId(), req);
            auditLogService.record(
                    principal.getOrganizationId(),
                    principal.getUserId(),
                    deviceIdentityService.localDeviceId(),
                    "RECORD_PAYMENT",
                    "Payment",
                    saved.getId().toString(),
                    "Recorded payment of " + saved.getAmount() + " via " + saved.getMethod()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.toDTO(saved, ""));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
