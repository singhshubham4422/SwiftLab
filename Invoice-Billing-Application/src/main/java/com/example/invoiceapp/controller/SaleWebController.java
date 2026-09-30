package com.example.invoiceapp.controller;

import com.example.invoiceapp.dto.SaleCreateRequest;
import com.example.invoiceapp.model.Sale;
import com.example.invoiceapp.model.User;
import com.example.invoiceapp.service.CustomerService;
import com.example.invoiceapp.service.ProductService;
import com.example.invoiceapp.service.SaleService;
import com.example.invoiceapp.service.WarehouseService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.ArrayList;

@Controller
@RequestMapping("/sales")
public class SaleWebController {

    private final SaleService saleService;
    private final CustomerService customerService;
    private final WarehouseService warehouseService;
    private final ProductService productService;

    public SaleWebController(SaleService saleService,
                             CustomerService customerService,
                             WarehouseService warehouseService,
                             ProductService productService) {
        this.saleService = saleService;
        this.customerService = customerService;
        this.warehouseService = warehouseService;
        this.productService = productService;
    }

    private User getSessionUser(HttpSession session) {
        return session != null ? (User) session.getAttribute("user") : null;
    }

    private Long getOrgId(HttpSession session) {
        User u = getSessionUser(session);
        return u != null ? u.getOrganizationId() : null;
    }

    @GetMapping
    public String list(HttpSession session, Model model) {
        Long orgId = getOrgId(session);
        if (orgId == null) return "redirect:/login";

        model.addAttribute("sales", saleService.listByOrganization(orgId));
        return "sales/list";
    }

    @GetMapping("/new")
    public String newForm(HttpSession session, Model model) {
        Long orgId = getOrgId(session);
        if (orgId == null) return "redirect:/login";

        SaleCreateRequest req = new SaleCreateRequest();
        req.setItems(new ArrayList<>());
        req.getItems().add(new SaleCreateRequest.ItemRequest(null, "", BigDecimal.ONE, BigDecimal.ZERO));

        model.addAttribute("saleRequest", req);
        model.addAttribute("customers", customerService.listByOrganization(orgId));
        model.addAttribute("warehouses", warehouseService.listByOrganization(orgId));
        model.addAttribute("products", productService.listProducts(orgId));
        model.addAttribute("pageTitle", "New Sale Order / POS");
        return "sales/form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute SaleCreateRequest req,
                       HttpSession session,
                       RedirectAttributes redirectAttributes) {
        User user = getSessionUser(session);
        if (user == null || user.getOrganizationId() == null) return "redirect:/login";

        try {
            if (req.getItems() != null) {
                req.getItems().removeIf(i -> i.getProductId() == null);
            }
            Sale saved = saleService.createSale(user.getOrganizationId(), user.getId(), req);
            redirectAttributes.addFlashAttribute("message", "Sale #" + saved.getSaleNumber() + " completed! Linked Invoice generated.");
            return "redirect:/sales/" + saved.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to process sale: " + e.getMessage());
            return "redirect:/sales/new";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, HttpSession session, Model model) {
        Long orgId = getOrgId(session);
        if (orgId == null) return "redirect:/login";

        Sale s = saleService.get(id, orgId);
        model.addAttribute("sale", s);
        model.addAttribute("customer", s.getCustomerId() != null ? customerService.findById(s.getCustomerId(), orgId).orElse(null) : null);
        model.addAttribute("warehouse", s.getWarehouseId() != null ? warehouseService.findById(s.getWarehouseId(), orgId).orElse(null) : null);
        return "sales/detail";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        Long orgId = getOrgId(session);
        if (orgId == null) return "redirect:/login";

        saleService.delete(id, orgId);
        redirectAttributes.addFlashAttribute("message", "Sale deleted.");
        return "redirect:/sales";
    }
}
