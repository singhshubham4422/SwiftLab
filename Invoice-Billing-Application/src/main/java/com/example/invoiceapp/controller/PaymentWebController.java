package com.example.invoiceapp.controller;

import com.example.invoiceapp.dto.PaymentCreateRequest;
import com.example.invoiceapp.model.Invoice;
import com.example.invoiceapp.model.User;
import com.example.invoiceapp.service.InvoiceService;
import com.example.invoiceapp.service.PaymentService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequestMapping("/payments")
public class PaymentWebController {

    private final PaymentService paymentService;
    private final InvoiceService invoiceService;

    public PaymentWebController(PaymentService paymentService, InvoiceService invoiceService) {
        this.paymentService = paymentService;
        this.invoiceService = invoiceService;
    }

    private Long getOrgId(HttpSession session) {
        User u = session != null ? (User) session.getAttribute("user") : null;
        return u != null ? u.getOrganizationId() : null;
    }

    @GetMapping
    public String list(HttpSession session, Model model) {
        Long orgId = getOrgId(session);
        if (orgId == null) return "redirect:/login";

        model.addAttribute("payments", paymentService.listByOrganization(orgId));
        return "payments/list";
    }

    @GetMapping("/new")
    public String newForm(@RequestParam(required = false) Long invoiceId,
                          HttpSession session,
                          Model model) {
        Long orgId = getOrgId(session);
        if (orgId == null) return "redirect:/login";

        PaymentCreateRequest req = new PaymentCreateRequest();
        if (invoiceId != null) {
            req.setInvoiceId(invoiceId);
            invoiceService.findByIdAndOrganization(invoiceId, orgId).ifPresent(inv -> {
                req.setAmount(BigDecimal.valueOf(inv.getOutstanding()));
            });
        }

        model.addAttribute("paymentRequest", req);
        model.addAttribute("invoices", invoiceService.listByOrganization(orgId));
        model.addAttribute("pageTitle", "Record Payment");
        return "payments/form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute PaymentCreateRequest req,
                       HttpSession session,
                       RedirectAttributes redirectAttributes) {
        Long orgId = getOrgId(session);
        if (orgId == null) return "redirect:/login";

        try {
            paymentService.recordPayment(orgId, req);
            redirectAttributes.addFlashAttribute("message", "Payment recorded successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to record payment: " + e.getMessage());
        }
        return "redirect:/payments";
    }
}
