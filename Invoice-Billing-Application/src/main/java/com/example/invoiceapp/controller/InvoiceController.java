package com.example.invoiceapp.controller;

import com.example.invoiceapp.model.CompanySettings;
import com.example.invoiceapp.model.Invoice;
import com.example.invoiceapp.model.InvoiceItem;
import com.example.invoiceapp.model.User;
import com.example.invoiceapp.service.CompanySettingsService;
import com.example.invoiceapp.service.InvoiceService;
import com.example.invoiceapp.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.stream.Collectors;

@Controller
public class InvoiceController {

    private final InvoiceService service;
    private final CompanySettingsService settingsService;
    private final UserService userService;

    public InvoiceController(InvoiceService service,
                             CompanySettingsService settingsService,
                             UserService userService) {
        this.service = service;
        this.settingsService = settingsService;
        this.userService = userService;
    }

    private User getSessionUser(HttpSession session) {
        if (session != null && session.getAttribute("user") != null) {
            return (User) session.getAttribute("user");
        }
        return null;
    }

    private Long getSessionOrgId(HttpSession session) {
        User u = getSessionUser(session);
        return u != null ? u.getOrganizationId() : null;
    }

    @ModelAttribute("settings")
    public CompanySettings populateSettings(HttpSession session) {
        User u = getSessionUser(session);
        return u != null ? settingsService.getSettingsForUser(u.getId()) : settingsService.getSettings();
    }

    @GetMapping("/")
    public String index(HttpSession session) {
        if (userService.countUsers() == 0) {
            return "redirect:/signup";
        }
        if (getSessionUser(session) == null) {
            return "redirect:/login";
        }
        return "redirect:/dashboard";
    }

    @GetMapping("/setup")
    public String setupRedirect() {
        return "redirect:/signup";
    }

    /* ---------- Settings Page ---------- */
    @GetMapping("/settings")
    public String settingsPage(HttpSession session, Model model) {
        User user = getSessionUser(session);
        if (user == null) return "redirect:/login";

        CompanySettings settings = settingsService.getSettingsForUser(user.getId());
        model.addAttribute("settingsForm", settings);
        return "settings";
    }

    @PostMapping("/settings/save")
    public String saveSettings(@ModelAttribute("settingsForm") CompanySettings form,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        User user = getSessionUser(session);
        if (user != null) {
            form.setUserId(user.getId());
            form.setOrganizationId(user.getOrganizationId());
        }
        settingsService.saveSettings(form);

        if (session != null && session.getAttribute("user") != null) {
            User u = (User) session.getAttribute("user");
            u.setFullName(form.getUserName());
            u.setOrganizationName(form.getOrganizationName());
            session.setAttribute("user", u);
        }

        redirectAttributes.addFlashAttribute("message", "Settings updated successfully.");
        return "redirect:/settings";
    }

    /* ---------- Invoices Dashboard / List ---------- */
    @GetMapping("/invoices")
    public String list(@RequestParam(value = "search", required = false) String search,
                       @RequestParam(value = "status", required = false) String status,
                       HttpSession session,
                       Model model) {
        Long orgId = getSessionOrgId(session);
        if (orgId == null) return "redirect:/login";

        List<Invoice> invoices = service.listByOrganization(orgId);

        if (search != null && !search.trim().isEmpty()) {
            String q = search.trim().toLowerCase();
            invoices = invoices.stream()
                    .filter(inv -> (inv.getInvoiceNumber() != null && inv.getInvoiceNumber().toLowerCase().contains(q))
                            || (inv.getCustomerName() != null && inv.getCustomerName().toLowerCase().contains(q)))
                    .collect(Collectors.toList());
        }

        if (status != null && !status.trim().isEmpty() && !status.equalsIgnoreCase("ALL")) {
            invoices = invoices.stream()
                    .filter(inv -> status.equalsIgnoreCase(inv.getStatus()))
                    .collect(Collectors.toList());
        }

        // Stats for current organization
        List<Invoice> all = service.listByOrganization(orgId);
        double totalRevenue = all.stream().mapToDouble(Invoice::getTotal).sum();
        double paidRevenue = all.stream().filter(i -> "PAID".equalsIgnoreCase(i.getStatus())).mapToDouble(Invoice::getTotal).sum();
        double pendingRevenue = all.stream().filter(i -> !"PAID".equalsIgnoreCase(i.getStatus())).mapToDouble(Invoice::getTotal).sum();
        long count = all.size();

        model.addAttribute("invoices", invoices);
        model.addAttribute("search", search);
        model.addAttribute("statusFilter", status);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("paidRevenue", paidRevenue);
        model.addAttribute("pendingRevenue", pendingRevenue);
        model.addAttribute("totalCount", count);

        return "list";
    }

    @GetMapping("/invoices/new")
    public String createForm(HttpSession session, Model model) {
        User user = getSessionUser(session);
        if (user == null) return "redirect:/login";

        CompanySettings currentSettings = settingsService.getSettingsForUser(user.getId());

        Invoice invoice = new Invoice();
        invoice.setOrganizationId(user.getOrganizationId());
        invoice.setUserId(user.getId());
        invoice.setInvoiceNumber(service.generateNextInvoiceNumber(user.getOrganizationId()));
        invoice.setTaxRate(currentSettings.getDefaultTaxRate());
        invoice.setPaymentTerms("Net 15 Days");
        invoice.addItem(new InvoiceItem("Services / Consulting", 1, 1000.0));

        model.addAttribute("invoice", invoice);
        model.addAttribute("pageTitle", "Create New Invoice");
        return "form";
    }

    @GetMapping("/invoices/edit/{id}")
    public String editForm(@PathVariable Long id, HttpSession session, Model model) {
        Long orgId = getSessionOrgId(session);
        if (orgId == null) return "redirect:/login";

        Invoice invoice = service.getForOrganization(id, orgId);
        if (invoice.getItems().isEmpty()) {
            invoice.addItem(new InvoiceItem("", 1, 0.0));
        }
        model.addAttribute("invoice", invoice);
        model.addAttribute("pageTitle", "Edit Invoice #" + invoice.getInvoiceNumber());
        return "form";
    }

    @PostMapping("/invoices/save")
    public String save(@ModelAttribute Invoice invoice,
                       HttpSession session,
                       RedirectAttributes redirectAttributes) {
        User user = getSessionUser(session);
        if (user == null) return "redirect:/login";

        invoice.setOrganizationId(user.getOrganizationId());
        invoice.setUserId(user.getId());

        Invoice saved = service.save(invoice);
        redirectAttributes.addFlashAttribute("message", "Invoice #" + saved.getInvoiceNumber() + " saved successfully!");
        return "redirect:/invoices/" + saved.getId();
    }

    @GetMapping("/invoices/{id}")
    public String detail(@PathVariable Long id, HttpSession session, Model model) {
        Long orgId = getSessionOrgId(session);
        if (orgId == null) return "redirect:/login";

        Invoice inv = service.getForOrganization(id, orgId);
        model.addAttribute("invoice", inv);
        return "detail";
    }

    @PostMapping("/invoices/{id}/status")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam String status,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        Long orgId = getSessionOrgId(session);
        if (orgId == null) return "redirect:/login";

        Invoice inv = service.getForOrganization(id, orgId);
        service.updateStatus(inv.getId(), status);
        redirectAttributes.addFlashAttribute("message", "Status updated to " + status);
        return "redirect:/invoices/" + id;
    }

    @PostMapping("/invoices/{id}/delete")
    public String delete(@PathVariable Long id,
                         HttpSession session,
                         RedirectAttributes redirectAttributes) {
        Long orgId = getSessionOrgId(session);
        if (orgId == null) return "redirect:/login";

        Invoice inv = service.getForOrganization(id, orgId);
        service.delete(inv.getId());
        redirectAttributes.addFlashAttribute("message", "Invoice deleted successfully.");
        return "redirect:/invoices";
    }

    /* ---------- PDF & Excel Export ---------- */
    @GetMapping("/invoices/{id}/pdf")
    public ResponseEntity<ByteArrayResource> downloadPdf(@PathVariable Long id, HttpSession session) {
        Long orgId = getSessionOrgId(session);
        if (orgId == null) return ResponseEntity.status(401).build();

        try {
            Invoice inv = service.getForOrganization(id, orgId);
            byte[] data = service.exportPdf(inv);
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

    @GetMapping("/invoices/{id}/excel")
    public ResponseEntity<ByteArrayResource> downloadExcel(@PathVariable Long id, HttpSession session) {
        Long orgId = getSessionOrgId(session);
        if (orgId == null) return ResponseEntity.status(401).build();

        try {
            Invoice inv = service.getForOrganization(id, orgId);
            byte[] data = service.exportExcel(inv);
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

    /* ---------- Excel Import & Template Download ---------- */
    @GetMapping("/invoices/import")
    public String importPage() {
        return "import";
    }

    @GetMapping("/invoices/sample-template")
    public ResponseEntity<ByteArrayResource> downloadTemplate(HttpSession session) {
        User user = getSessionUser(session);
        if (user == null) return ResponseEntity.status(401).build();

        try {
            byte[] data = service.generateSampleExcelTemplate(user.getId());
            ByteArrayResource resource = new ByteArrayResource(data);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"invoice_sample_template.xlsx\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .contentLength(data.length)
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/invoices/import")
    public String handleImport(@RequestParam("file") MultipartFile file,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        User user = getSessionUser(session);
        if (user == null) return "redirect:/login";

        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please select a valid Excel file (.xlsx) to import.");
            return "redirect:/invoices/import";
        }
        try {
            Invoice imported = service.importFromExcel(file, user.getId(), user.getOrganizationId());
            redirectAttributes.addFlashAttribute("message", "Excel imported successfully as Invoice #" + imported.getInvoiceNumber());
            return "redirect:/invoices/" + imported.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to import Excel file: " + e.getMessage());
            return "redirect:/invoices/import";
        }
    }
}
