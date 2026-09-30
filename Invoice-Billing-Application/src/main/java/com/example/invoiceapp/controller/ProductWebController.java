package com.example.invoiceapp.controller;

import com.example.invoiceapp.dto.ProductDTO;
import com.example.invoiceapp.model.Product;
import com.example.invoiceapp.model.User;
import com.example.invoiceapp.service.ProductService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/products")
public class ProductWebController {

    private final ProductService productService;

    public ProductWebController(ProductService productService) {
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

        model.addAttribute("products", productService.listProducts(orgId));
        model.addAttribute("categories", productService.listCategories(orgId));
        model.addAttribute("brands", productService.listBrands(orgId));
        model.addAttribute("units", productService.listUnits(orgId));
        return "products/list";
    }

    @GetMapping("/new")
    public String newForm(HttpSession session, Model model) {
        Long orgId = getOrgId(session);
        if (orgId == null) return "redirect:/login";

        model.addAttribute("product", new ProductDTO());
        model.addAttribute("categories", productService.listCategories(orgId));
        model.addAttribute("brands", productService.listBrands(orgId));
        model.addAttribute("units", productService.listUnits(orgId));
        model.addAttribute("pageTitle", "Add New Product");
        return "products/form";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, HttpSession session, Model model) {
        Long orgId = getOrgId(session);
        if (orgId == null) return "redirect:/login";

        Product p = productService.get(id, orgId);
        model.addAttribute("product", productService.toDTO(p));
        model.addAttribute("categories", productService.listCategories(orgId));
        model.addAttribute("brands", productService.listBrands(orgId));
        model.addAttribute("units", productService.listUnits(orgId));
        model.addAttribute("pageTitle", "Edit Product - " + p.getName());
        return "products/form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute ProductDTO dto, HttpSession session, RedirectAttributes redirectAttributes) {
        Long orgId = getOrgId(session);
        if (orgId == null) return "redirect:/login";

        try {
            productService.saveProduct(orgId, dto);
            redirectAttributes.addFlashAttribute("message", "Product '" + dto.getName() + "' saved successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error saving product: " + e.getMessage());
        }
        return "redirect:/products";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        Long orgId = getOrgId(session);
        if (orgId == null) return "redirect:/login";

        productService.deleteProduct(id, orgId);
        redirectAttributes.addFlashAttribute("message", "Product deleted successfully.");
        return "redirect:/products";
    }

    @PostMapping("/category/save")
    public String saveCategory(@RequestParam String name,
                               @RequestParam(required = false) String description,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        Long orgId = getOrgId(session);
        if (orgId == null) return "redirect:/login";

        productService.saveCategory(orgId, name, description);
        redirectAttributes.addFlashAttribute("message", "Category '" + name + "' added!");
        return "redirect:/products";
    }

    @PostMapping("/brand/save")
    public String saveBrand(@RequestParam String name, HttpSession session, RedirectAttributes redirectAttributes) {
        Long orgId = getOrgId(session);
        if (orgId == null) return "redirect:/login";

        productService.saveBrand(orgId, name);
        redirectAttributes.addFlashAttribute("message", "Brand '" + name + "' added!");
        return "redirect:/products";
    }

    @PostMapping("/unit/save")
    public String saveUnit(@RequestParam String name,
                           @RequestParam(required = false) String abbreviation,
                           HttpSession session,
                           RedirectAttributes redirectAttributes) {
        Long orgId = getOrgId(session);
        if (orgId == null) return "redirect:/login";

        productService.saveUnit(orgId, name, abbreviation);
        redirectAttributes.addFlashAttribute("message", "Unit '" + name + "' added!");
        return "redirect:/products";
    }
}
