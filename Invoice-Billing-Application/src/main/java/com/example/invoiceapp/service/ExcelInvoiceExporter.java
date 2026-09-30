package com.example.invoiceapp.service;

import com.example.invoiceapp.model.CompanySettings;
import com.example.invoiceapp.model.Invoice;
import com.example.invoiceapp.model.InvoiceItem;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class ExcelInvoiceExporter {

    public byte[] exportExcel(Invoice invoice, CompanySettings settings) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Invoice - " + invoice.getInvoiceNumber());

            String orgName = (settings != null && settings.getOrganizationName() != null && !settings.getOrganizationName().trim().isEmpty())
                    ? settings.getOrganizationName().trim()
                    : "INVOICE BILLING";
            String curr = (settings != null && settings.getCurrencySymbol() != null) ? settings.getCurrencySymbol() : "₹";

            CellStyle titleStyle = wb.createCellStyle();
            Font titleFont = wb.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            titleFont.setColor(IndexedColors.DARK_BLUE.getIndex());
            titleStyle.setFont(titleFont);

            CellStyle boldStyle = wb.createCellStyle();
            Font boldFont = wb.createFont();
            boldFont.setBold(true);
            boldStyle.setFont(boldFont);

            CellStyle tableHeaderStyle = wb.createCellStyle();
            Font thFont = wb.createFont();
            thFont.setBold(true);
            thFont.setColor(IndexedColors.WHITE.getIndex());
            tableHeaderStyle.setFont(thFont);
            tableHeaderStyle.setFillForegroundColor(IndexedColors.DARK_TEAL.getIndex());
            tableHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            tableHeaderStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle numberStyle = wb.createCellStyle();
            DataFormat df = wb.createDataFormat();
            numberStyle.setDataFormat(df.getFormat(curr + "#,##0.00"));

            int rowIdx = 0;

            // Title
            Row rTitle = sheet.createRow(rowIdx++);
            Cell cTitle = rTitle.createCell(0);
            cTitle.setCellValue(orgName.toUpperCase() + " - INVOICE STATEMENT");
            cTitle.setCellStyle(titleStyle);

            rowIdx++; // empty row

            // Metadata rows
            createKeyValueRow(sheet, rowIdx++, "Invoice Number:", invoice.getInvoiceNumber(), boldStyle);
            createKeyValueRow(sheet, rowIdx++, "Issue Date:", invoice.getIssueDate() != null ? invoice.getIssueDate().toString() : "", boldStyle);
            createKeyValueRow(sheet, rowIdx++, "Due Date:", invoice.getDueDate() != null ? invoice.getDueDate().toString() : "", boldStyle);
            if (invoice.hasPaymentTerms()) {
                createKeyValueRow(sheet, rowIdx++, "Payment Terms:", invoice.getPaymentTerms(), boldStyle);
            }
            createKeyValueRow(sheet, rowIdx++, "Status:", invoice.getStatus(), boldStyle);
            createKeyValueRow(sheet, rowIdx++, "Customer Name:", invoice.getCustomerName(), boldStyle);
            createKeyValueRow(sheet, rowIdx++, "Customer Email:", invoice.getCustomerEmail(), boldStyle);
            createKeyValueRow(sheet, rowIdx++, "Customer Phone:", invoice.getCustomerPhone(), boldStyle);
            createKeyValueRow(sheet, rowIdx++, "Customer Address:", invoice.getCustomerAddress(), boldStyle);

            rowIdx++; // empty row

            // Table Header
            Row thRow = sheet.createRow(rowIdx++);
            String[] cols = {"Item #", "Description", "Quantity", "Unit Price (" + curr + ")", "Total Price (" + curr + ")"};
            for (int i = 0; i < cols.length; i++) {
                Cell cell = thRow.createCell(i);
                cell.setCellValue(cols[i]);
                cell.setCellStyle(tableHeaderStyle);
            }

            // Items
            int itemNum = 1;
            if (invoice.getItems() != null) {
                for (InvoiceItem item : invoice.getItems()) {
                    Row r = sheet.createRow(rowIdx++);
                    r.createCell(0).setCellValue(itemNum++);
                    r.createCell(1).setCellValue(item.getDescription());
                    r.createCell(2).setCellValue(item.getQuantity());

                    Cell upCell = r.createCell(3);
                    upCell.setCellValue(item.getUnitPrice());
                    upCell.setCellStyle(numberStyle);

                    Cell totCell = r.createCell(4);
                    totCell.setCellValue(item.getTotal());
                    totCell.setCellStyle(numberStyle);
                }
            }

            rowIdx++; // blank

            // Subtotal, Tax, Discount, Total
            createSummaryRow(sheet, rowIdx++, "Subtotal (" + curr + "):", invoice.getSubtotal(), boldStyle, numberStyle);
            if (invoice.getDiscount() > 0) {
                createSummaryRow(sheet, rowIdx++, "Discount (" + curr + "):", -invoice.getDiscount(), boldStyle, numberStyle);
            }
            createSummaryRow(sheet, rowIdx++, "Tax (" + invoice.getTaxRate() + "%):", invoice.getTaxAmount(), boldStyle, numberStyle);
            createSummaryRow(sheet, rowIdx++, "Grand Total (" + curr + "):", invoice.getTotal(), boldStyle, numberStyle);

            if (invoice.getNotes() != null && !invoice.getNotes().isEmpty()) {
                rowIdx++;
                Row nRow = sheet.createRow(rowIdx++);
                Cell nc = nRow.createCell(0);
                nc.setCellValue("Notes: " + invoice.getNotes());
                nc.setCellStyle(boldStyle);
            }

            // Footer
            rowIdx++;
            Row footerRow = sheet.createRow(rowIdx);
            Cell fc = footerRow.createCell(0);
            fc.setCellValue(settings != null 
                    ? settings.getFooterNote(invoice.getGeneratedAt()) 
                    : "Invoice generated on " + invoice.getFormattedGeneratedAt() + " • Generated by Swift Invoicing System • Offline Encrypted Record");

            for (int i = 0; i < 5; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wb.write(baos);
            return baos.toByteArray();
        }
    }

    private void createKeyValueRow(Sheet sheet, int rowIdx, String key, String val, CellStyle keyStyle) {
        Row row = sheet.createRow(rowIdx);
        Cell c1 = row.createCell(0);
        c1.setCellValue(key);
        c1.setCellStyle(keyStyle);
        Cell c2 = row.createCell(1);
        c2.setCellValue(val != null ? val : "");
    }

    private void createSummaryRow(Sheet sheet, int rowIdx, String label, double amount, CellStyle boldStyle, CellStyle numStyle) {
        Row row = sheet.createRow(rowIdx);
        Cell cLabel = row.createCell(3);
        cLabel.setCellValue(label);
        cLabel.setCellStyle(boldStyle);

        Cell cVal = row.createCell(4);
        cVal.setCellValue(amount);
        cVal.setCellStyle(numStyle);
    }

    public byte[] generateSampleExcelTemplate(CompanySettings settings) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Invoice_Template");
            String curr = (settings != null && settings.getCurrencySymbol() != null) ? settings.getCurrencySymbol() : "₹";

            CellStyle headerStyle = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            font.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle bold = wb.createCellStyle();
            Font bf = wb.createFont();
            bf.setBold(true);
            bold.setFont(bf);

            Row r0 = sheet.createRow(0);
            Cell c0 = r0.createCell(0);
            c0.setCellValue("INVOICE IMPORT TEMPLATE");
            c0.setCellStyle(bold);

            createKeyValueRow(sheet, 2, "Invoice Number:", "INV-2026-999", bold);
            createKeyValueRow(sheet, 3, "Customer Name:", "Apex Global Solutions", bold);
            createKeyValueRow(sheet, 4, "Customer Email:", "billing@apexglobal.com", bold);
            createKeyValueRow(sheet, 5, "Customer Phone:", "+91 98765 43210", bold);
            createKeyValueRow(sheet, 6, "Customer Address:", "Sector 48, Gurugram, HR, India", bold);
            createKeyValueRow(sheet, 7, "Tax Rate (%):", "18.0", bold);
            createKeyValueRow(sheet, 8, "Discount (" + curr + "):", "50.0", bold);
            createKeyValueRow(sheet, 9, "Notes:", "Standard payment terms. Thank you!", bold);

            Row th = sheet.createRow(11);
            String[] headers = {"Item #", "Description", "Quantity", "Unit Price (" + curr + ")"};
            for (int i = 0; i < headers.length; i++) {
                Cell c = th.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            Object[][] samples = {
                    {1, "Cloud Architecture Consulting", 10, 1500.00},
                    {2, "Custom Software Implementation", 1, 12000.00},
                    {3, "Annual Maintenance & Support", 1, 3500.00}
            };

            int startRow = 12;
            for (Object[] itm : samples) {
                Row r = sheet.createRow(startRow++);
                r.createCell(0).setCellValue((Integer) itm[0]);
                r.createCell(1).setCellValue((String) itm[1]);
                r.createCell(2).setCellValue((Integer) itm[2]);
                r.createCell(3).setCellValue((Double) itm[3]);
            }

            for (int i = 0; i < 4; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wb.write(baos);
            return baos.toByteArray();
        }
    }

    public Invoice parseExcel(MultipartFile file) throws IOException {
        try (InputStream is = file.getInputStream(); Workbook wb = WorkbookFactory.create(is)) {
            Sheet sheet = wb.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();

            Invoice inv = new Invoice();
            inv.setInvoiceNumber("INV-" + System.currentTimeMillis() % 1000000);
            inv.setIssueDate(LocalDate.now());
            inv.setDueDate(LocalDate.now().plusDays(15));
            inv.setStatus("PENDING");

            for (int i = 0; i <= Math.min(sheet.getLastRowNum(), 15); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                Cell c0 = row.getCell(0);
                Cell c1 = row.getCell(1);
                if (c0 == null || c1 == null) continue;

                String key = formatter.formatCellValue(c0).trim().toLowerCase();
                String val = formatter.formatCellValue(c1).trim();

                if (key.contains("invoice number") || key.contains("invoice #")) {
                    if (!val.isEmpty()) inv.setInvoiceNumber(val);
                } else if (key.contains("customer name") || key.contains("customer:")) {
                    if (!val.isEmpty()) inv.setCustomerName(val);
                } else if (key.contains("email")) {
                    if (!val.isEmpty()) inv.setCustomerEmail(val);
                } else if (key.contains("phone")) {
                    if (!val.isEmpty()) inv.setCustomerPhone(val);
                } else if (key.contains("address")) {
                    if (!val.isEmpty()) inv.setCustomerAddress(val);
                } else if (key.contains("tax rate")) {
                    try { inv.setTaxRate(Double.parseDouble(val.replace("%", "").trim())); } catch (Exception ignored) {}
                } else if (key.contains("discount")) {
                    try { inv.setDiscount(Double.parseDouble(val.replaceAll("[^0-9.]", "").trim())); } catch (Exception ignored) {}
                } else if (key.contains("notes")) {
                    inv.setNotes(val);
                }
            }

            int itemHeaderRow = -1;
            int descCol = 1;
            int qtyCol = 2;
            int priceCol = 3;

            for (int i = 0; i <= sheet.getLastRowNum(); i++) {
                Row r = sheet.getRow(i);
                if (r == null) continue;
                for (int c = 0; c < r.getLastCellNum(); c++) {
                    String heading = formatter.formatCellValue(r.getCell(c)).trim().toLowerCase();
                    if (heading.contains("description")) {
                        itemHeaderRow = i;
                        descCol = c;
                        for (int nc = 0; nc < r.getLastCellNum(); nc++) {
                            String nh = formatter.formatCellValue(r.getCell(nc)).trim().toLowerCase();
                            if (nh.contains("qty") || nh.contains("quantity")) qtyCol = nc;
                            if (nh.contains("price") || nh.contains("rate") || nh.contains("cost")) priceCol = nc;
                        }
                        break;
                    }
                }
                if (itemHeaderRow != -1) break;
            }

            List<InvoiceItem> items = new ArrayList<>();
            if (itemHeaderRow != -1) {
                for (int i = itemHeaderRow + 1; i <= sheet.getLastRowNum(); i++) {
                    Row r = sheet.getRow(i);
                    if (r == null) continue;

                    String desc = formatter.formatCellValue(r.getCell(descCol)).trim();
                    if (desc.isEmpty() || desc.toLowerCase().contains("subtotal") || desc.toLowerCase().contains("total")) {
                        continue;
                    }

                    int qty = 1;
                    try {
                        String qStr = formatter.formatCellValue(r.getCell(qtyCol)).trim();
                        if (!qStr.isEmpty()) qty = (int) Double.parseDouble(qStr);
                    } catch (Exception ignored) {}

                    double price = 0.0;
                    try {
                        String pStr = formatter.formatCellValue(r.getCell(priceCol)).trim().replaceAll("[^0-9.]", "");
                        if (!pStr.isEmpty()) price = Double.parseDouble(pStr);
                    } catch (Exception ignored) {}

                    InvoiceItem itm = new InvoiceItem(desc, qty, price);
                    items.add(itm);
                }
            }

            if (items.isEmpty()) {
                items.add(new InvoiceItem("Imported Product / Service", 1, 100.00));
            }

            inv.setItems(items);
            return inv;
        }
    }
}
