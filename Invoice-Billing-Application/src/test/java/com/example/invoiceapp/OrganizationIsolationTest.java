package com.example.invoiceapp;

import com.example.invoiceapp.dto.CustomerDTO;
import com.example.invoiceapp.dto.ProductDTO;
import com.example.invoiceapp.dto.SupplierDTO;
import com.example.invoiceapp.model.*;
import com.example.invoiceapp.service.CustomerService;
import com.example.invoiceapp.service.InvoiceService;
import com.example.invoiceapp.service.ProductService;
import com.example.invoiceapp.service.SupplierService;
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
public class OrganizationIsolationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private ProductService productService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private SupplierService supplierService;

    @Autowired
    private InvoiceService invoiceService;

    @Test
    @DisplayName("Strict multi-tenant isolation across products, customers, suppliers, and invoices")
    void testTenantIsolation() {
        // Setup Tenant 1 (Acme)
        User user1 = userService.register("user_acme@test.com", "pass", "Acme User", "Acme Corporation", "$", "USD", "", "", "");
        Long org1 = user1.getOrganizationId();

        // Setup Tenant 2 (Beta)
        User user2 = userService.register("user_beta@test.com", "pass", "Beta User", "Beta Enterprises", "$", "USD", "", "", "");
        Long org2 = user2.getOrganizationId();

        assertNotEquals(org1, org2, "Organizations must have unique IDs");

        // Org 1 creates Product
        ProductDTO prod1 = new ProductDTO();
        prod1.setSku("ACME-01");
        prod1.setName("Acme Widget");
        prod1.setSellingPrice(BigDecimal.valueOf(100.0));
        Product savedProd1 = productService.saveProduct(org1, prod1);

        // Org 1 creates Customer
        CustomerDTO cust1 = new CustomerDTO();
        cust1.setName("Acme Customer");
        cust1.setEmail("cust@acme.com");
        Customer savedCust1 = customerService.save(org1, cust1);

        // Org 1 creates Supplier
        SupplierDTO supp1 = new SupplierDTO();
        supp1.setName("Acme Supplier");
        Supplier savedSupp1 = supplierService.save(org1, supp1);

        // Org 1 creates Invoice
        Invoice inv1 = new Invoice();
        inv1.setOrganizationId(org1);
        inv1.setUserId(user1.getId());
        inv1.setCustomerName("Acme Customer");
        inv1.setInvoiceNumber("INV-ACME-001");
        inv1.addItem(new InvoiceItem("Consulting", 1, 500.0));
        inv1 = invoiceService.save(inv1);

        // --- VERIFY TENANT 2 CANNOT SEE OR ACCESS ORG 1 DATA ---

        // 1. Products isolation
        List<ProductDTO> org2Products = productService.listProducts(org2);
        assertTrue(org2Products.isEmpty(), "Org 2 product list must be empty");
        final Long prod1Id = savedProd1.getId();
        assertThrows(IllegalArgumentException.class, () -> productService.get(prod1Id, org2),
                "Org 2 querying Org 1 product must throw IllegalArgumentException");

        // 2. Customers isolation
        List<CustomerDTO> org2Customers = customerService.listByOrganization(org2);
        assertTrue(org2Customers.isEmpty(), "Org 2 customer list must be empty");
        final Long cust1Id = savedCust1.getId();
        assertThrows(IllegalArgumentException.class, () -> customerService.get(cust1Id, org2),
                "Org 2 querying Org 1 customer must throw IllegalArgumentException");

        // 3. Suppliers isolation
        List<SupplierDTO> org2Suppliers = supplierService.listByOrganization(org2);
        assertTrue(org2Suppliers.isEmpty(), "Org 2 supplier list must be empty");
        final Long supp1Id = savedSupp1.getId();
        assertThrows(IllegalArgumentException.class, () -> supplierService.get(supp1Id, org2),
                "Org 2 querying Org 1 supplier must throw IllegalArgumentException");

        // 4. Invoices isolation
        List<Invoice> org2Invoices = invoiceService.listByOrganization(org2);
        assertTrue(org2Invoices.isEmpty(), "Org 2 invoice list must be empty");
        final Long inv1Id = inv1.getId();
        assertThrows(RuntimeException.class, () -> invoiceService.getForOrganization(inv1Id, org2),
                "Org 2 accessing Org 1 invoice must throw RuntimeException");
    }
}
