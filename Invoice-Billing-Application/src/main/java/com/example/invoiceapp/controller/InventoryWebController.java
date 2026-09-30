package com.example.invoiceapp.controller;

import com.example.invoiceapp.dto.StockAdjustmentRequest;
import com.example.invoiceapp.model.User;
import com.example.invoiceapp.service.InventoryService;
import com.example.invoiceapp.service.ProductService;
import com.example.invoiceapp.service.WarehouseService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/inventory")
public class InventoryWebController {

    private final InventoryService inventoryService;
    private final ProductService productService;
    private final WarehouseService warehouseService;

    public InventoryWebController(InventoryService inventoryService,
                                  ProductService productService,
                                  WarehouseService warehouseService) {
        this.inventoryService = inventoryService;
        this.productService = productService;
        this.warehouseService = warehouseService;
    }

    private Long getOrgId(HttpSession session) {
        User u = session != null ? (User) session.getAttribute("user") : null;
        return u != null ? u.getOrganizationId() : null;
    }

    @GetMapping
    public String list(HttpSession session, Model model) {
        Long orgId = getOrgId(session);
        if (orgId == null) return "redirect:/login";

        model.addAttribute("stocks", inventoryService.listStock(orgId));
        model.addAttribute("warehouses", warehouseService.listByOrganization(orgId));
        model.addAttribute("products", productService.listProducts(orgId));
        model.addAttribute("adjustmentRequest", new StockAdjustmentRequest());
        return "inventory/list";
    }

    @PostMapping("/adjust")
    public String adjust(@ModelAttribute StockAdjustmentRequest req,
                         HttpSession session,
                         RedirectAttributes redirectAttributes) {
        Long orgId = getOrgId(session);
        if (orgId == null) return "redirect:/login";

        try {
            inventoryService.adjustStock(orgId, req);
            redirectAttributes.addFlashAttribute("message", "Stock updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update stock: " + e.getMessage());
        }
        return "redirect:/inventory";
    }

    @GetMapping("/movements")
    public String movements(HttpSession session, Model model) {
        Long orgId = getOrgId(session);
        if (orgId == null) return "redirect:/login";

        model.addAttribute("movements", inventoryService.listMovements(orgId));
        return "inventory/movements";
    }
}
