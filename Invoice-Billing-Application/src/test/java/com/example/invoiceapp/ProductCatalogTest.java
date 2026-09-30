package com.example.invoiceapp;

import com.example.invoiceapp.dto.ProductDTO;
import com.example.invoiceapp.model.*;
import com.example.invoiceapp.service.ProductService;
import com.example.invoiceapp.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class ProductCatalogTest {

    @Autowired
    private UserService userService;

    @Autowired
    private ProductService productService;

    @Test
    @DisplayName("Product CRUD, SKU uniqueness, and soft delete verification")
    void testProductLifecycleAndSoftDelete() {
        User user = userService.register("catalog_mgr@test.com", "pass", "Catalog Manager", "Catalog Corp", "$", "USD", "", "", "");
        Long orgId = user.getOrganizationId();

        // 1. Create Category, Brand, Unit
        Category savedCat = productService.saveCategory(orgId, "Hardware", "Computer accessories");
        Brand savedBrand = productService.saveBrand(orgId, "SwiftBrand");
        Unit savedUnit = productService.saveUnit(orgId, "Pieces", "PCS");

        // 2. Create Product
        ProductDTO prod = new ProductDTO();
        prod.setSku("HW-001");
        prod.setName("Wireless Mouse");
        prod.setCategoryId(savedCat.getId());
        prod.setBrandId(savedBrand.getId());
        prod.setUnitId(savedUnit.getId());
        prod.setPurchasePrice(BigDecimal.valueOf(15.0));
        prod.setSellingPrice(BigDecimal.valueOf(29.99));
        prod.setMinimumStock(BigDecimal.valueOf(5.0));

        Product savedProd = productService.saveProduct(orgId, prod);
        assertNotNull(savedProd.getId());
        assertEquals("HW-001", savedProd.getSku());

        // 3. Prevent duplicate SKU in same organization
        ProductDTO dupSku = new ProductDTO();
        dupSku.setSku("HW-001");
        dupSku.setName("Another Mouse");
        assertThrows(IllegalArgumentException.class, () -> productService.saveProduct(orgId, dupSku),
                "Duplicate SKU in same org must throw exception");

        // 4. Update Product
        ProductDTO updateDto = new ProductDTO();
        updateDto.setId(savedProd.getId());
        updateDto.setSku("HW-001");
        updateDto.setName("Wireless Mouse V2");
        updateDto.setSellingPrice(BigDecimal.valueOf(34.99));
        Product updated = productService.saveProduct(orgId, updateDto);
        assertEquals(BigDecimal.valueOf(34.99), updated.getSellingPrice());

        // 5. Soft Delete Product
        productService.deleteProduct(savedProd.getId(), orgId);

        // Standard list should not return soft-deleted product
        List<ProductDTO> activeProducts = productService.listProducts(orgId);
        assertTrue(activeProducts.isEmpty(), "Soft deleted product must not appear in active product list");
    }
}
