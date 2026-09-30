package com.example.invoiceapp.controller.api;

import com.example.invoiceapp.dto.BrandDTO;
import com.example.invoiceapp.dto.CategoryDTO;
import com.example.invoiceapp.dto.ProductDTO;
import com.example.invoiceapp.dto.UnitDTO;
import com.example.invoiceapp.model.Brand;
import com.example.invoiceapp.model.Category;
import com.example.invoiceapp.model.Product;
import com.example.invoiceapp.model.Unit;
import com.example.invoiceapp.security.AccessPolicy;
import com.example.invoiceapp.security.AppUserPrincipal;
import com.example.invoiceapp.service.AuditLogService;
import com.example.invoiceapp.service.DeviceIdentityService;
import com.example.invoiceapp.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "Catalog & Products", description = "Product, category, brand, and unit management APIs")
public class ProductApiController {

    private final ProductService productService;
    private final AuditLogService auditLogService;
    private final DeviceIdentityService deviceIdentityService;

    public ProductApiController(ProductService productService,
                                AuditLogService auditLogService,
                                DeviceIdentityService deviceIdentityService) {
        this.productService = productService;
        this.auditLogService = auditLogService;
        this.deviceIdentityService = deviceIdentityService;
    }

    // Products
    @GetMapping("/products")
    @Operation(summary = "List all products in organization")
    public ResponseEntity<List<ProductDTO>> listProducts(@AuthenticationPrincipal AppUserPrincipal principal) {
        if (principal == null || principal.getOrganizationId() == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(productService.listProducts(principal.getOrganizationId()));
    }

    @GetMapping("/products/{id}")
    @Operation(summary = "Get product details by ID")
    public ResponseEntity<?> getProduct(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable Long id) {
        if (principal == null || principal.getOrganizationId() == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Product product = productService.get(id, principal.getOrganizationId());
        return ResponseEntity.ok(productService.toDTO(product));
    }

    @PostMapping("/products")
    @Operation(summary = "Create a new product")
    public ResponseEntity<?> createProduct(@AuthenticationPrincipal AppUserPrincipal principal, @RequestBody ProductDTO dto) {
        if (principal == null || principal.getOrganizationId() == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!AccessPolicy.canManageProducts(principal.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Insufficient permissions to manage products"));
        }

        try {
            dto.setId(null);
            Product saved = productService.saveProduct(principal.getOrganizationId(), dto);
            auditLogService.record(
                    principal.getOrganizationId(),
                    principal.getUserId(),
                    deviceIdentityService.localDeviceId(),
                    "CREATE_PRODUCT",
                    "Product",
                    saved.getId().toString(),
                    "Created product: " + saved.getName() + " (SKU: " + saved.getSku() + ")"
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(productService.toDTO(saved));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/products/{id}")
    @Operation(summary = "Update an existing product")
    public ResponseEntity<?> updateProduct(@AuthenticationPrincipal AppUserPrincipal principal,
                                           @PathVariable Long id,
                                           @RequestBody ProductDTO dto) {
        if (principal == null || principal.getOrganizationId() == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!AccessPolicy.canManageProducts(principal.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Insufficient permissions to manage products"));
        }

        try {
            dto.setId(id);
            Product saved = productService.saveProduct(principal.getOrganizationId(), dto);
            auditLogService.record(
                    principal.getOrganizationId(),
                    principal.getUserId(),
                    deviceIdentityService.localDeviceId(),
                    "UPDATE_PRODUCT",
                    "Product",
                    saved.getId().toString(),
                    "Updated product: " + saved.getName()
            );
            return ResponseEntity.ok(productService.toDTO(saved));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/products/{id}")
    @Operation(summary = "Soft delete a product")
    public ResponseEntity<?> deleteProduct(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable Long id) {
        if (principal == null || principal.getOrganizationId() == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!AccessPolicy.canManageProducts(principal.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Insufficient permissions to delete products"));
        }

        productService.deleteProduct(id, principal.getOrganizationId());
        auditLogService.record(
                principal.getOrganizationId(),
                principal.getUserId(),
                deviceIdentityService.localDeviceId(),
                "DELETE_PRODUCT",
                "Product",
                id.toString(),
                "Soft deleted product #" + id
        );
        return ResponseEntity.ok(Map.of("message", "Product deleted successfully"));
    }

    // Categories
    @GetMapping("/categories")
    @Operation(summary = "List all product categories")
    public ResponseEntity<List<CategoryDTO>> listCategories(@AuthenticationPrincipal AppUserPrincipal principal) {
        if (principal == null || principal.getOrganizationId() == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(productService.listCategories(principal.getOrganizationId()));
    }

    @PostMapping("/categories")
    @Operation(summary = "Create a product category")
    public ResponseEntity<?> createCategory(@AuthenticationPrincipal AppUserPrincipal principal, @RequestBody Map<String, String> body) {
        if (principal == null || principal.getOrganizationId() == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        String name = body.get("name");
        String desc = body.get("description");
        if (name == null || name.isBlank()) return ResponseEntity.badRequest().body(Map.of("error", "Category name is required"));

        Category saved = productService.saveCategory(principal.getOrganizationId(), name, desc);
        return ResponseEntity.status(HttpStatus.CREATED).body(new CategoryDTO(saved.getId(), saved.getPublicId(), saved.getName(), saved.getDescription()));
    }

    // Brands
    @GetMapping("/brands")
    @Operation(summary = "List all product brands")
    public ResponseEntity<List<BrandDTO>> listBrands(@AuthenticationPrincipal AppUserPrincipal principal) {
        if (principal == null || principal.getOrganizationId() == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(productService.listBrands(principal.getOrganizationId()));
    }

    @PostMapping("/brands")
    @Operation(summary = "Create a product brand")
    public ResponseEntity<?> createBrand(@AuthenticationPrincipal AppUserPrincipal principal, @RequestBody Map<String, String> body) {
        if (principal == null || principal.getOrganizationId() == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        String name = body.get("name");
        if (name == null || name.isBlank()) return ResponseEntity.badRequest().body(Map.of("error", "Brand name is required"));

        Brand saved = productService.saveBrand(principal.getOrganizationId(), name);
        return ResponseEntity.status(HttpStatus.CREATED).body(new BrandDTO(saved.getId(), saved.getPublicId(), saved.getName()));
    }

    // Units
    @GetMapping("/units")
    @Operation(summary = "List all product units")
    public ResponseEntity<List<UnitDTO>> listUnits(@AuthenticationPrincipal AppUserPrincipal principal) {
        if (principal == null || principal.getOrganizationId() == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(productService.listUnits(principal.getOrganizationId()));
    }

    @PostMapping("/units")
    @Operation(summary = "Create a product unit")
    public ResponseEntity<?> createUnit(@AuthenticationPrincipal AppUserPrincipal principal, @RequestBody Map<String, String> body) {
        if (principal == null || principal.getOrganizationId() == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        String name = body.get("name");
        String abbr = body.get("abbreviation");
        if (name == null || name.isBlank()) return ResponseEntity.badRequest().body(Map.of("error", "Unit name is required"));

        Unit saved = productService.saveUnit(principal.getOrganizationId(), name, abbr);
        return ResponseEntity.status(HttpStatus.CREATED).body(new UnitDTO(saved.getId(), saved.getPublicId(), saved.getName(), saved.getAbbreviation()));
    }
}
