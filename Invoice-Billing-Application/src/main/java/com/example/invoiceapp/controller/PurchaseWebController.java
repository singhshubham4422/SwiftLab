package com.example.invoiceapp.controller;

import com.example.invoiceapp.dto.PurchaseCreateRequest;
import com.example.invoiceapp.model.Purchase;
import com.example.invoiceapp.model.User;
import com.example.invoiceapp.service.ProductService;
import com.example.invoiceapp.service.PurchaseService;
import com.example.invoiceapp.service.SupplierService;
import com.example.invoiceapp.service.WarehouseService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.ArrayList;

@Controller
@RequestMapping("/purchases")
public class PurchaseWebController {

    private final PurchaseService purchaseService;
    private final SupplierService supplierService;
    private final WarehouseService warehouseService;
    private final ProductService productService;

    public PurchaseWebController(PurchaseService purchaseService,
                                 SupplierService supplierService,
                                 WarehouseService warehouseService,
                                 ProductService productService) {
        this.purchaseService = purchaseService;
        this.supplierService = supplierService;
        this.warehouseService = warehouseService;
        this.productService = productService;
    }

    private Long getOrgId(HttpSession session) {
        User u = session != null ? (User) session.getAttribute("user") : null;
        return u != null ? u.getOrganizationId() : null;
    }

    @GetMapping
    public String list(HttpSession session, Model model) {
        Long orgId = getOrgId(session);
        if (orgId == null) return "redirect:/login";

        model.addAttribute("purchases", purchaseService.listByOrganization(orgId));
        return "purchases/list";
    }

    @GetMapping("/new")
    public String newForm(HttpSession session, Model model) {
        Long orgId = getOrgId(session);
        if (orgId == null) return "redirect:/login";

        PurchaseCreateRequest req = new PurchaseCreateRequest();
        req.setItems(new ArrayList<>());
        req.getItems().add(new PurchaseCreateRequest.ItemRequest(null, "", BigDecimal.ONE, BigDecimal.ZERO));

        model.addAttribute("purchaseRequest", req);
        model.addAttribute("suppliers", supplierService.listByOrganization(orgId));
        model.addAttribute("warehouses", warehouseService.listByOrganization(orgId));
        model.addAttribute("products", productService.listProducts(orgId));
        model.addAttribute("pageTitle", "New Purchase Order");
        return "purchases/form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute PurchaseCreateRequest req,
                       HttpSession session,
                       RedirectAttributes redirectAttributes) {
        Long orgId = getOrgId(session);
        if (orgId == null) return "redirect:/login";

        try {
            if (req.getItems() != null) {
                req.getItems().removeIf(i -> i.getProductId() == null);
            }
            Purchase saved = purchaseService.createPurchase(orgId, req);
            redirectAttributes.addFlashAttribute("message", "Purchase #" + saved.getPurchaseNumber() + " created successfully!");
            return "redirect:/purchases/" + saved.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to create purchase: " + e.getMessage());
            return "redirect:/purchases/new";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, HttpSession session, Model model) {
        Long orgId = getOrgId(session);
        if (orgId == null) return "redirect:/login";

        Purchase p = purchaseService.get(id, orgId);
        model.addAttribute("purchase", p);
        model.addAttribute("supplier", p.getSupplierId() != null ? supplierService.findById(p.getSupplierId(), orgId).orElse(null) : null);
        model.addAttribute("warehouse", p.getWarehouseId() != null ? warehouseService.findById(p.getWarehouseId(), orgId).orElse(null) : null);
        return "purchases/detail";
    }

    @PostMapping("/{id}/receive")
    public String receive(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        Long orgId = getOrgId(session);
        if (orgId == null) return "redirect:/login";

        try {
            Purchase received = purchaseService.receiveGoods(id, orgId);
            redirectAttributes.addFlashAttribute("message", "Goods received for PO #" + received.getPurchaseNumber() + ". Inventory has been updated!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error receiving goods: " + e.getMessage());
        }
        return "redirect:/purchases/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        Long orgId = getOrgId(session);
        if (orgId == null) return "redirect:/login";

        purchaseService.delete(id, orgId);
        redirectAttributes.addFlashAttribute("message", "Purchase deleted.");
        return "redirect:/purchases";
    }
}
