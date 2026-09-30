package com.example.invoiceapp;

import com.example.invoiceapp.dto.ProductDTO;
import com.example.invoiceapp.dto.PurchaseCreateRequest;
import com.example.invoiceapp.dto.SupplierDTO;
import com.example.invoiceapp.model.*;
import com.example.invoiceapp.model.enums.PurchaseStatus;
import com.example.invoiceapp.service.*;
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
public class PurchaseWorkflowTest {

    @Autowired
    private UserService userService;

    @Autowired
    private ProductService productService;

    @Autowired
    private SupplierService supplierService;

    @Autowired
    private WarehouseService warehouseService;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private PurchaseService purchaseService;

    @Test
    @DisplayName("Purchase Workflow: Order creation -> Receive goods -> Automatic stock replenishment & StockMovement")
    void testPurchaseWorkflow() {
        User user = userService.register("buyer_user@test.com", "pass", "Buyer User", "Supply Chain Inc", "$", "USD", "", "", "");
        Long orgId = user.getOrganizationId();

        Warehouse wh = warehouseService.ensureDefaultWarehouse(orgId);

        SupplierDTO supp = new SupplierDTO();
        supp.setName("Mega Supplier");
        Supplier savedSupp = supplierService.save(orgId, supp);

        ProductDTO prod = new ProductDTO();
        prod.setSku("PO-ITEM-01");
        prod.setName("Monitor 27 inch");
        prod.setPurchasePrice(BigDecimal.valueOf(150.0));
        prod.setSellingPrice(BigDecimal.valueOf(250.0));
        Product savedProd = productService.saveProduct(orgId, prod);

        // Pre-check stock is 0
        assertEquals(BigDecimal.ZERO, inventoryService.getAvailableStock(orgId, wh.getId(), savedProd.getId()));

        // 1. Create Purchase Order
        PurchaseCreateRequest req = new PurchaseCreateRequest();
        req.setSupplierId(savedSupp.getId());
        req.setWarehouseId(wh.getId());
        req.setReceiveNow(false);
        req.setNotes("Bulk monitor shipment");

        PurchaseCreateRequest.ItemRequest itemReq = new PurchaseCreateRequest.ItemRequest(
                savedProd.getId(),
                "Monitor 27 inch",
                BigDecimal.valueOf(30.0),
                BigDecimal.valueOf(150.0)
        );
        req.setItems(List.of(itemReq));

        Purchase po = purchaseService.createPurchase(orgId, req);
        assertNotNull(po.getId());
        assertEquals(PurchaseStatus.ORDERED, po.getStatus());
        assertEquals(0, BigDecimal.valueOf(4500.0).compareTo(po.getTotal()));

        // Stock should still be 0 until received
        assertEquals(BigDecimal.ZERO, inventoryService.getAvailableStock(orgId, wh.getId(), savedProd.getId()));

        // 2. Receive Goods
        Purchase received = purchaseService.receiveGoods(po.getId(), orgId);
        assertEquals(PurchaseStatus.RECEIVED, received.getStatus());

        // Stock must now be exactly 30.0!
        assertEquals(0, BigDecimal.valueOf(30.0).compareTo(inventoryService.getAvailableStock(orgId, wh.getId(), savedProd.getId())));
    }
}
