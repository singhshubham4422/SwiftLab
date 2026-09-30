package com.example.invoiceapp.controller;

import com.example.invoiceapp.dto.SupplierDTO;
import com.example.invoiceapp.model.Supplier;
import com.example.invoiceapp.model.User;
import com.example.invoiceapp.service.SupplierService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/suppliers")
public class SupplierWebController {

    private final SupplierService supplierService;

    public SupplierWebController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    private Long getOrgId(HttpSession session) {
        User u = session != null ? (User) session.getAttribute("user") : null;
        return u != null ? u.getOrganizationId() : null;
    }

    @GetMapping
    public String list(HttpSession session, Model model) {
        Long orgId = getOrgId(session);
        if (orgId == null) return "redirect:/login";

        model.addAttribute("suppliers", supplierService.listByOrganization(orgId));
        return "suppliers/list";
    }

    @GetMapping("/new")
    public String newForm(HttpSession session, Model model) {
        Long orgId = getOrgId(session);
        if (orgId == null) return "redirect:/login";

        model.addAttribute("supplier", new SupplierDTO());
        model.addAttribute("pageTitle", "Add New Supplier");
        return "suppliers/form";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, HttpSession session, Model model) {
        Long orgId = getOrgId(session);
        if (orgId == null) return "redirect:/login";

        Supplier s = supplierService.get(id, orgId);
        model.addAttribute("supplier", supplierService.toDTO(s));
        model.addAttribute("pageTitle", "Edit Supplier - " + s.getName());
        return "suppliers/form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute SupplierDTO dto, HttpSession session, RedirectAttributes redirectAttributes) {
        Long orgId = getOrgId(session);
        if (orgId == null) return "redirect:/login";

        try {
            supplierService.save(orgId, dto);
            redirectAttributes.addFlashAttribute("message", "Supplier '" + dto.getName() + "' saved successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error saving supplier: " + e.getMessage());
        }
        return "redirect:/suppliers";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        Long orgId = getOrgId(session);
        if (orgId == null) return "redirect:/login";

        supplierService.delete(id, orgId);
        redirectAttributes.addFlashAttribute("message", "Supplier deleted successfully.");
        return "redirect:/suppliers";
    }
}
