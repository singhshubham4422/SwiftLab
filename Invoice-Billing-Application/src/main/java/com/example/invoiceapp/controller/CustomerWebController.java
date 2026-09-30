package com.example.invoiceapp.controller;

import com.example.invoiceapp.dto.CustomerDTO;
import com.example.invoiceapp.model.Customer;
import com.example.invoiceapp.model.User;
import com.example.invoiceapp.service.CustomerService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/customers")
public class CustomerWebController {

    private final CustomerService customerService;

    public CustomerWebController(CustomerService customerService) {
        this.customerService = customerService;
    }

    private Long getOrgId(HttpSession session) {
        User u = session != null ? (User) session.getAttribute("user") : null;
        return u != null ? u.getOrganizationId() : null;
    }

    @GetMapping
    public String list(HttpSession session, Model model) {
        Long orgId = getOrgId(session);
        if (orgId == null) return "redirect:/login";

        model.addAttribute("customers", customerService.listByOrganization(orgId));
        return "customers/list";
    }

    @GetMapping("/new")
    public String newForm(HttpSession session, Model model) {
        Long orgId = getOrgId(session);
        if (orgId == null) return "redirect:/login";

        model.addAttribute("customer", new CustomerDTO());
        model.addAttribute("pageTitle", "Add New Customer");
        return "customers/form";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, HttpSession session, Model model) {
        Long orgId = getOrgId(session);
        if (orgId == null) return "redirect:/login";

        Customer c = customerService.get(id, orgId);
        model.addAttribute("customer", customerService.toDTO(c));
        model.addAttribute("pageTitle", "Edit Customer - " + c.getName());
        return "customers/form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute CustomerDTO dto, HttpSession session, RedirectAttributes redirectAttributes) {
        Long orgId = getOrgId(session);
        if (orgId == null) return "redirect:/login";

        try {
            customerService.save(orgId, dto);
            redirectAttributes.addFlashAttribute("message", "Customer '" + dto.getName() + "' saved successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error saving customer: " + e.getMessage());
        }
        return "redirect:/customers";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        Long orgId = getOrgId(session);
        if (orgId == null) return "redirect:/login";

        customerService.delete(id, orgId);
        redirectAttributes.addFlashAttribute("message", "Customer deleted successfully.");
        return "redirect:/customers";
    }
}
