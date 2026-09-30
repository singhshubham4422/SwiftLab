package com.example.invoiceapp;

import com.example.invoiceapp.dto.ProductDTO;
import com.example.invoiceapp.dto.StockAdjustmentRequest;
import com.example.invoiceapp.dto.StockMovementDTO;
import com.example.invoiceapp.model.Product;
import com.example.invoiceapp.model.User;
import com.example.invoiceapp.model.Warehouse;
import com.example.invoiceapp.model.enums.StockMovementType;
import com.example.invoiceapp.service.InventoryService;
import com.example.invoiceapp.service.ProductService;
import com.example.invoiceapp.service.UserService;
import com.example.invoiceapp.service.WarehouseService;
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
public class InventoryEngineTest {

    @Autowired
    private UserService userService;

    @Autowired
    private ProductService productService;

    @Autowired
    private WarehouseService warehouseService;

    @Autowired
    private InventoryService inventoryService;

    @Test
    @DisplayName("Movement-based inventory: Stock In, Sale, Purchase, Return, Adjustment with ledger audit")
    void testMovementBasedInventory() {
        User user = userService.register("inv_user@test.com", "pass", "Logistics Mgr", "Logistics Org", "$", "USD", "", "", "");
        Long orgId = user.getOrganizationId();

        // Warehouse & Product
        Warehouse wh = warehouseService.ensureDefaultWarehouse(orgId);
        ProductDTO prod = new ProductDTO();
        prod.setSku("ITEM-LEDGER-01");
        prod.setName("Mechanical Keyboard");
        prod.setSellingPrice(BigDecimal.valueOf(80.0));
        Product savedProd = productService.saveProduct(orgId, prod);
        Long prodId = savedProd.getId();

        // Initial check: stock is 0
        assertEquals(BigDecimal.ZERO, inventoryService.getAvailableStock(orgId, wh.getId(), prodId));

        // 1. Opening Stock +100 (STOCK_IN)
        StockAdjustmentRequest req1 = new StockAdjustmentRequest();
        req1.setWarehouseId(wh.getId());
        req1.setProductId(prodId);
        req1.setMovementType(StockMovementType.STOCK_IN);
        req1.setQuantity(BigDecimal.valueOf(100.0));
        req1.setNotes("Opening initial stock");
        inventoryService.adjustStock(orgId, req1);
        assertEquals(0, BigDecimal.valueOf(100.0).compareTo(inventoryService.getAvailableStock(orgId, wh.getId(), prodId)));

        // 2. Sale -5
        StockAdjustmentRequest req2 = new StockAdjustmentRequest();
        req2.setWarehouseId(wh.getId());
        req2.setProductId(prodId);
        req2.setMovementType(StockMovementType.SALE);
        req2.setQuantity(BigDecimal.valueOf(5.0));
        inventoryService.adjustStock(orgId, req2);
        assertEquals(0, BigDecimal.valueOf(95.0).compareTo(inventoryService.getAvailableStock(orgId, wh.getId(), prodId)));

        // 3. Purchase +20
        StockAdjustmentRequest req3 = new StockAdjustmentRequest();
        req3.setWarehouseId(wh.getId());
        req3.setProductId(prodId);
        req3.setMovementType(StockMovementType.PURCHASE);
        req3.setQuantity(BigDecimal.valueOf(20.0));
        inventoryService.adjustStock(orgId, req3);
        assertEquals(0, BigDecimal.valueOf(115.0).compareTo(inventoryService.getAvailableStock(orgId, wh.getId(), prodId)));

        // 4. Return +2
        StockAdjustmentRequest req4 = new StockAdjustmentRequest();
        req4.setWarehouseId(wh.getId());
        req4.setProductId(prodId);
        req4.setMovementType(StockMovementType.RETURN);
        req4.setQuantity(BigDecimal.valueOf(2.0));
        inventoryService.adjustStock(orgId, req4);
        assertEquals(0, BigDecimal.valueOf(117.0).compareTo(inventoryService.getAvailableStock(orgId, wh.getId(), prodId)));

        // 5. Stock Out -1 (Adjustment reduction)
        StockAdjustmentRequest req5 = new StockAdjustmentRequest();
        req5.setWarehouseId(wh.getId());
        req5.setProductId(prodId);
        req5.setMovementType(StockMovementType.STOCK_OUT);
        req5.setQuantity(BigDecimal.valueOf(1.0));
        inventoryService.adjustStock(orgId, req5);
        
        // Final stock should be 116.0!
        assertEquals(0, BigDecimal.valueOf(116.0).compareTo(inventoryService.getAvailableStock(orgId, wh.getId(), prodId)));

        // Verify movement ledger entries
        List<StockMovementDTO> movements = inventoryService.listMovements(orgId);
        assertEquals(5, movements.size());

        // 6. Test negative stock prevention: attempting to deduct 200 units must fail
        StockAdjustmentRequest invalidReq = new StockAdjustmentRequest();
        invalidReq.setWarehouseId(wh.getId());
        invalidReq.setProductId(prodId);
        invalidReq.setMovementType(StockMovementType.STOCK_OUT);
        invalidReq.setQuantity(BigDecimal.valueOf(200.0));
        assertThrows(IllegalStateException.class, () -> inventoryService.adjustStock(orgId, invalidReq),
                "Deducting more stock than available must throw IllegalStateException");
    }
}
