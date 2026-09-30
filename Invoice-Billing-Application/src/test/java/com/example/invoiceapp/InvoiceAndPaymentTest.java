package com.example.invoiceapp;

import com.example.invoiceapp.dto.PaymentCreateRequest;
import com.example.invoiceapp.model.CompanySettings;
import com.example.invoiceapp.model.Invoice;
import com.example.invoiceapp.model.InvoiceItem;
import com.example.invoiceapp.model.User;
import com.example.invoiceapp.model.enums.PaymentMethod;
import com.example.invoiceapp.service.CompanySettingsService;
import com.example.invoiceapp.service.InvoiceService;
import com.example.invoiceapp.service.PaymentService;
import com.example.invoiceapp.service.PdfInvoiceExporter;
import com.example.invoiceapp.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class InvoiceAndPaymentTest {

    @Autowired
    private UserService userService;

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private CompanySettingsService settingsService;

    @Autowired
    private PdfInvoiceExporter pdfInvoiceExporter;

    @Test
    @DisplayName("Invoice calculations, partial payments, status progression, and PDF generation")
    void testInvoiceCalculationsAndPayments() throws Exception {
        User user = userService.register("billing_user@test.com", "pass", "Billing Manager", "Accounting LLC", "$", "USD", "", "", "");
        Long orgId = user.getOrganizationId();

        // 1. Create Invoice with 2 items and 10% tax
        Invoice inv = new Invoice();
        inv.setOrganizationId(orgId);
        inv.setUserId(user.getId());
        inv.setInvoiceNumber("INV-TEST-9001");
        inv.setCustomerName("Enterprise Client");
        inv.setCustomerEmail("client@enterprise.com");
        inv.setTaxRate(10.0);
        inv.setDiscount(50.0); // $50 discount
        inv.addItem(new InvoiceItem("Web App Development", 1, 1000.0));
        inv.addItem(new InvoiceItem("Server Setup", 1, 500.0));

        // Subtotal = 1500, Tax = (1500 - 50) * 0.10 = 145.0, Total = 1450 + 145 = 1595.0
        Invoice saved = invoiceService.save(inv);
        assertEquals(1500.0, saved.getSubtotal());
        assertEquals(145.0, saved.getTaxAmount());
        assertEquals(1595.0, saved.getTotal());
        assertEquals("PENDING", saved.getStatus());

        // 2. Partial Payment #1: $600
        PaymentCreateRequest p1 = new PaymentCreateRequest();
        p1.setInvoiceId(saved.getId());
        p1.setAmount(BigDecimal.valueOf(600.0));
        p1.setMethod(PaymentMethod.BANK_TRANSFER);
        p1.setReference("WIRE-001");
        paymentService.recordPayment(orgId, p1);

        Invoice updated1 = invoiceService.getForOrganization(saved.getId(), orgId);
        assertEquals(600.0, updated1.getAmountPaid());
        assertEquals("PARTIALLY_PAID", updated1.getStatus());

        // 3. Partial Payment #2: Remaining balance of $995.0
        PaymentCreateRequest p2 = new PaymentCreateRequest();
        p2.setInvoiceId(saved.getId());
        p2.setAmount(BigDecimal.valueOf(995.0));
        p2.setMethod(PaymentMethod.UPI);
        p2.setReference("UPI-002");
        paymentService.recordPayment(orgId, p2);

        Invoice updated2 = invoiceService.getForOrganization(saved.getId(), orgId);
        assertEquals(1595.0, updated2.getAmountPaid());
        assertEquals("PAID", updated2.getStatus());

        // 4. PDF Generation verification
        CompanySettings settings = settingsService.getSettingsForUser(user.getId());
        byte[] pdfBytes = pdfInvoiceExporter.exportPdf(saved, settings);
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 500, "PDF byte array should be non-empty");
    }
}
