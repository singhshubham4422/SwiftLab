package com.example.invoiceapp.service;

import com.example.invoiceapp.dto.PaymentCreateRequest;
import com.example.invoiceapp.dto.PaymentDTO;
import com.example.invoiceapp.model.Invoice;
import com.example.invoiceapp.model.Payment;
import com.example.invoiceapp.model.enums.SyncOperation;
import com.example.invoiceapp.repository.InvoiceRepository;
import com.example.invoiceapp.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepo;
    private final InvoiceRepository invoiceRepo;
    private final SyncQueueService syncQueueService;

    public PaymentService(PaymentRepository paymentRepo,
                          InvoiceRepository invoiceRepo,
                          SyncQueueService syncQueueService) {
        this.paymentRepo = paymentRepo;
        this.invoiceRepo = invoiceRepo;
        this.syncQueueService = syncQueueService;
    }

    public List<PaymentDTO> listByOrganization(Long organizationId) {
        if (organizationId == null) return List.of();
        List<Payment> payments = paymentRepo.findByOrganizationIdAndDeletedFalseOrderByIdDesc(organizationId);
        Map<Long, String> invoiceNumbers = invoiceRepo.findByOrganizationIdAndDeletedFalseOrderByIdDesc(organizationId)
                .stream().collect(Collectors.toMap(Invoice::getId, Invoice::getInvoiceNumber, (a, b) -> a));

        return payments.stream().map(p -> toDTO(p, invoiceNumbers.getOrDefault(p.getInvoiceId(), "N/A")))
                .collect(Collectors.toList());
    }

    public List<PaymentDTO> listByInvoice(Long invoiceId) {
        if (invoiceId == null) return List.of();
        Invoice invoice = invoiceRepo.findById(invoiceId).orElse(null);
        String invNum = invoice != null ? invoice.getInvoiceNumber() : "N/A";
        return paymentRepo.findByInvoiceIdAndDeletedFalse(invoiceId).stream()
                .map(p -> toDTO(p, invNum))
                .collect(Collectors.toList());
    }

    @Transactional
    public Payment recordPayment(Long organizationId, PaymentCreateRequest req) {
        if (organizationId == null) throw new IllegalArgumentException("Organization ID is required");
        Invoice invoice = invoiceRepo.findByIdAndOrganizationId(req.getInvoiceId(), organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found in organization"));

        Payment payment = new Payment();
        payment.setOrganizationId(organizationId);
        payment.setInvoiceId(invoice.getId());
        payment.setMethod(req.getMethod());
        payment.setAmount(req.getAmount() != null ? req.getAmount() : BigDecimal.ZERO);
        payment.setPaidOn(req.getPaidOn() != null ? req.getPaidOn() : LocalDate.now());
        payment.setReference(req.getReference());
        payment.setNotes(req.getNotes());
        payment.setCreatedAt(Instant.now());
        payment.setUpdatedAt(Instant.now());

        Payment savedPayment = paymentRepo.save(payment);

        // Update invoice balance and status
        double currentPaid = invoice.getAmountPaid();
        double addedAmount = req.getAmount().doubleValue();
        invoice.setAmountPaid(currentPaid + addedAmount);
        invoice.refreshPaymentStatus();
        invoiceRepo.save(invoice);

        // Enqueue sync if Cloud Sync is active
        syncQueueService.enqueue(
                organizationId,
                "Payment",
                savedPayment.getPublicId(),
                SyncOperation.CREATE,
                toDTO(savedPayment, invoice.getInvoiceNumber()),
                null
        );

        return savedPayment;
    }

    public PaymentDTO toDTO(Payment p, String invoiceNumber) {
        if (p == null) return null;
        PaymentDTO dto = new PaymentDTO();
        dto.setId(p.getId());
        dto.setPublicId(p.getPublicId());
        dto.setInvoiceId(p.getInvoiceId());
        dto.setInvoiceNumber(invoiceNumber);
        dto.setMethod(p.getMethod());
        dto.setAmount(p.getAmount());
        dto.setPaidOn(p.getPaidOn());
        dto.setReference(p.getReference());
        dto.setNotes(p.getNotes());
        return dto;
    }
}
