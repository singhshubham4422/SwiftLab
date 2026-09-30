package com.example.invoiceapp;

import com.example.invoiceapp.dto.CustomerDTO;
import com.example.invoiceapp.dto.ProductDTO;
import com.example.invoiceapp.dto.SaleCreateRequest;
import com.example.invoiceapp.dto.StockAdjustmentRequest;
import com.example.invoiceapp.model.*;
import com.example.invoiceapp.model.enums.PaymentMethod;
import com.example.invoiceapp.model.enums.SaleStatus;
import com.example.invoiceapp.model.enums.StockMovementType;
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
public class SaleWorkflowTest {

    @Autowired
    private UserService userService;

    @Autowired
    private ProductService productService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private WarehouseService warehouseService;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private SaleService saleService;

    @Autowired
    private InvoiceService invoiceService;

    @Test
    @DisplayName("Sale POS Workflow: Stock validation -> Sale & Items -> Stock deduction -> Auto Invoice -> Payment")
    void testSalePosWorkflow() {
        User user = userService.register("pos_cashier@test.com", "pass", "POS Cashier", "Retail Hub", "$", "USD", "", "", "");
        Long orgId = user.getOrganizationId();

        Warehouse wh = warehouseService.ensureDefaultWarehouse(orgId);

        CustomerDTO cust = new CustomerDTO();
        cust.setName("VIP Shopper");
        Customer savedCust = customerService.save(orgId, cust);

        ProductDTO prod = new ProductDTO();
        prod.setSku("POS-ITEM-99");
        prod.setName("Gaming Headset");
        prod.setSellingPrice(BigDecimal.valueOf(100.0));
        Product savedProd = productService.saveProduct(orgId, prod);

        // Seed stock with 10 units
        StockAdjustmentRequest adj = new StockAdjustmentRequest();
        adj.setWarehouseId(wh.getId());
        adj.setProductId(savedProd.getId());
        adj.setMovementType(StockMovementType.STOCK_IN);
        adj.setQuantity(BigDecimal.valueOf(10.0));
        inventoryService.adjustStock(orgId, adj);
        assertEquals(0, BigDecimal.valueOf(10.0).compareTo(inventoryService.getAvailableStock(orgId, wh.getId(), savedProd.getId())));

        // 1. Attempt sale exceeding available stock (e.g. 15 units) -> Must fail and rollback
        SaleCreateRequest invalidReq = new SaleCreateRequest();
        invalidReq.setCustomerId(savedCust.getId());
        invalidReq.setWarehouseId(wh.getId());
        invalidReq.setItems(List.of(new SaleCreateRequest.ItemRequest(savedProd.getId(), "Gaming Headset", BigDecimal.valueOf(15.0), BigDecimal.valueOf(100.0))));
        assertThrows(IllegalStateException.class, () -> saleService.createSale(orgId, user.getId(), invalidReq),
                "Sale exceeding stock must fail");

        // Stock must remain intact at 10.0
        assertEquals(0, BigDecimal.valueOf(10.0).compareTo(inventoryService.getAvailableStock(orgId, wh.getId(), savedProd.getId())));

        // 2. Execute valid sale of 3 units with cash payment
        SaleCreateRequest validReq = new SaleCreateRequest();
        validReq.setCustomerId(savedCust.getId());
        validReq.setWarehouseId(wh.getId());
        validReq.setCompleteNow(true);
        validReq.setMarkPaid(true);
        validReq.setPaymentMethod(PaymentMethod.CASH);
        validReq.setNotes("Front desk cash sale");
        validReq.setItems(List.of(new SaleCreateRequest.ItemRequest(savedProd.getId(), "Gaming Headset", BigDecimal.valueOf(3.0), BigDecimal.valueOf(100.0))));

        Sale sale = saleService.createSale(orgId, user.getId(), validReq);
        assertNotNull(sale.getId());
        assertEquals(SaleStatus.COMPLETED, sale.getStatus());
        assertEquals(0, BigDecimal.valueOf(300.0).compareTo(sale.getTotal()));
        assertNotNull(sale.getInvoiceId(), "Sale must automatically generate an Invoice");

        // Stock must now be decremented: 10 - 3 = 7.0!
        assertEquals(0, BigDecimal.valueOf(7.0).compareTo(inventoryService.getAvailableStock(orgId, wh.getId(), savedProd.getId())));

        // Verify generated invoice
        Invoice inv = invoiceService.getForOrganization(sale.getInvoiceId(), orgId);
        assertNotNull(inv);
        assertEquals(300.0, inv.getTotal());
        assertEquals("PAID", inv.getStatus());
        assertEquals(300.0, inv.getAmountPaid());
    }
}
