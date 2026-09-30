package com.example.invoiceapp.service;

import com.example.invoiceapp.model.CompanySettings;
import com.example.invoiceapp.model.Invoice;
import com.example.invoiceapp.model.InvoiceItem;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

@Component
public class PdfInvoiceExporter {

    public byte[] exportPdf(Invoice invoice, CompanySettings settings) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document doc = new Document(pdf, PageSize.A4);
        doc.setMargins(30, 36, 30, 36);

        PdfFont fontBold;
        PdfFont fontRegular;

        // Use Windows Arial if available for full Unicode currency symbol support (e.g. ₹)
        File arialFile = new File("C:/Windows/Fonts/arial.ttf");
        File arialBdFile = new File("C:/Windows/Fonts/arialbd.ttf");
        String curr = (settings != null && settings.getCurrencySymbol() != null) ? settings.getCurrencySymbol() : "₹";

        if (arialFile.exists() && arialBdFile.exists()) {
            fontBold = PdfFontFactory.createFont(arialBdFile.getAbsolutePath(), PdfEncodings.IDENTITY_H);
            fontRegular = PdfFontFactory.createFont(arialFile.getAbsolutePath(), PdfEncodings.IDENTITY_H);
        } else {
            fontBold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            fontRegular = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            if ("₹".equals(curr)) {
                curr = "Rs. ";
            }
        }

        DeviceRgb primaryColor = new DeviceRgb(30, 41, 59);     // Slate 800
        DeviceRgb accentColor = new DeviceRgb(37, 99, 235);     // Blue 600
        DeviceRgb headerBg = new DeviceRgb(241, 245, 249);      // Slate 100
        DeviceRgb lightBorder = new DeviceRgb(226, 232, 240);   // Slate 200

        String orgName = (settings != null && settings.getOrganizationName() != null && !settings.getOrganizationName().trim().isEmpty())
                ? settings.getOrganizationName().trim()
                : "INVOICE BILLING";

        String tagline = (settings != null && settings.getTagline() != null && !settings.getTagline().trim().isEmpty())
                ? settings.getTagline().trim()
                : "Modern Enterprise Invoice Management";

        String contactLine = "";
        if (settings != null) {
            StringBuilder sb = new StringBuilder();
            if (settings.getEmail() != null && !settings.getEmail().isEmpty()) {
                sb.append("Email: ").append(settings.getEmail());
            }
            if (settings.getPhone() != null && !settings.getPhone().isEmpty()) {
                if (sb.length() > 0) sb.append(" | ");
                sb.append("Tel: ").append(settings.getPhone());
            }
            contactLine = sb.toString();
        }

        // --- Top Bar: Brand & Invoice Badge ---
        Table topTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .useAllAvailableWidth()
                .setBorder(Border.NO_BORDER);

        Cell brandCell = new Cell().setBorder(Border.NO_BORDER);
        brandCell.add(new Paragraph(orgName.toUpperCase())
                .setFont(fontBold).setFontSize(20).setFontColor(accentColor));
        brandCell.add(new Paragraph(tagline + (contactLine.isEmpty() ? "" : "\n" + contactLine))
                .setFont(fontRegular).setFontSize(9).setFontColor(ColorConstants.GRAY));
        topTable.addCell(brandCell);

        Cell titleCell = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT);
        titleCell.add(new Paragraph("INVOICE")
                .setFont(fontBold).setFontSize(24).setFontColor(primaryColor));
        titleCell.add(new Paragraph("# " + invoice.getInvoiceNumber())
                .setFont(fontBold).setFontSize(11).setFontColor(accentColor));
        titleCell.add(new Paragraph("Status: " + invoice.getStatus())
                .setFont(fontBold).setFontSize(10)
                .setFontColor("PAID".equalsIgnoreCase(invoice.getStatus()) ? new DeviceRgb(22, 163, 74) : new DeviceRgb(217, 119, 6)));
        topTable.addCell(titleCell);

        doc.add(topTable);
        doc.add(new Paragraph("\n"));

        // --- Bill To & Details Section ---
        Table infoTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .useAllAvailableWidth()
                .setBorder(Border.NO_BORDER)
                .setBackgroundColor(headerBg);

        Cell billTo = new Cell().setBorder(Border.NO_BORDER).setPadding(8);
        billTo.add(new Paragraph("BILLED TO:").setFont(fontBold).setFontSize(10).setFontColor(ColorConstants.GRAY));
        billTo.add(new Paragraph(invoice.getCustomerName() != null ? invoice.getCustomerName() : "Valued Customer")
                .setFont(fontBold).setFontSize(12).setFontColor(primaryColor));
        if (invoice.getCustomerEmail() != null && !invoice.getCustomerEmail().isEmpty()) {
            billTo.add(new Paragraph("Email: " + invoice.getCustomerEmail()).setFont(fontRegular).setFontSize(9));
        }
        if (invoice.getCustomerPhone() != null && !invoice.getCustomerPhone().isEmpty()) {
            billTo.add(new Paragraph("Phone: " + invoice.getCustomerPhone()).setFont(fontRegular).setFontSize(9));
        }
        if (invoice.getCustomerAddress() != null && !invoice.getCustomerAddress().isEmpty()) {
            billTo.add(new Paragraph("Address: " + invoice.getCustomerAddress()).setFont(fontRegular).setFontSize(9));
        }
        infoTable.addCell(billTo);

        Cell metaCell = new Cell().setBorder(Border.NO_BORDER).setPadding(8).setTextAlignment(TextAlignment.RIGHT);
        metaCell.add(new Paragraph("INVOICE DETAILS").setFont(fontBold).setFontSize(10).setFontColor(ColorConstants.GRAY));
        metaCell.add(new Paragraph("Invoice Date: " + (invoice.getIssueDate() != null ? invoice.getIssueDate().format(DateTimeFormatter.ISO_DATE) : "-"))
                .setFont(fontRegular).setFontSize(9));
        metaCell.add(new Paragraph("Due Date: " + (invoice.getDueDate() != null ? invoice.getDueDate().format(DateTimeFormatter.ISO_DATE) : "-"))
                .setFont(fontRegular).setFontSize(9));
        if (invoice.hasPaymentTerms()) {
            metaCell.add(new Paragraph("Terms: " + invoice.getPaymentTerms()).setFont(fontRegular).setFontSize(9));
        }
        infoTable.addCell(metaCell);

        doc.add(infoTable);
        doc.add(new Paragraph("\n"));

        // --- Line Items Table ---
        Table table = new Table(UnitValue.createPercentArray(new float[]{5, 2, 2, 2}))
                .useAllAvailableWidth()
                .setBorder(new SolidBorder(lightBorder, 1));

        String[] headers = {"Item Description", "Quantity", "Unit Price (" + curr + ")", "Total (" + curr + ")"};
        for (int i = 0; i < headers.length; i++) {
            Cell hCell = new Cell().add(new Paragraph(headers[i]).setFont(fontBold).setFontSize(10).setFontColor(ColorConstants.WHITE));
            hCell.setBackgroundColor(primaryColor);
            hCell.setPadding(8);
            if (i > 0) hCell.setTextAlignment(TextAlignment.RIGHT);
            table.addHeaderCell(hCell);
        }

        if (invoice.getItems() != null && !invoice.getItems().isEmpty()) {
            boolean isOdd = false;
            for (InvoiceItem item : invoice.getItems()) {
                DeviceRgb rowBg = isOdd ? headerBg : new DeviceRgb(255, 255, 255);
                isOdd = !isOdd;

                Cell c1 = new Cell().add(new Paragraph(item.getDescription()).setFont(fontRegular).setFontSize(9)).setBackgroundColor(rowBg).setPadding(7);
                Cell c2 = new Cell().add(new Paragraph(String.valueOf(item.getQuantity())).setFont(fontRegular).setFontSize(9)).setBackgroundColor(rowBg).setTextAlignment(TextAlignment.RIGHT).setPadding(7);
                Cell c3 = new Cell().add(new Paragraph(curr + String.format("%.2f", item.getUnitPrice())).setFont(fontRegular).setFontSize(9)).setBackgroundColor(rowBg).setTextAlignment(TextAlignment.RIGHT).setPadding(7);
                Cell c4 = new Cell().add(new Paragraph(curr + String.format("%.2f", item.getTotal())).setFont(fontBold).setFontSize(9)).setBackgroundColor(rowBg).setTextAlignment(TextAlignment.RIGHT).setPadding(7);

                table.addCell(c1);
                table.addCell(c2);
                table.addCell(c3);
                table.addCell(c4);
            }
        } else {
            Cell empty = new Cell(1, 4).add(new Paragraph("No items recorded.").setFont(fontRegular).setFontSize(9)).setTextAlignment(TextAlignment.CENTER).setPadding(10);
            table.addCell(empty);
        }

        doc.add(table);
        doc.add(new Paragraph("\n"));

        // --- Totals Summary Block ---
        Table summaryTable = new Table(UnitValue.createPercentArray(new float[]{6, 4}))
                .useAllAvailableWidth()
                .setBorder(Border.NO_BORDER);

        Cell notesCell = new Cell().setBorder(Border.NO_BORDER).setPadding(5);
        if (invoice.getNotes() != null && !invoice.getNotes().trim().isEmpty()) {
            notesCell.add(new Paragraph("NOTES & INSTRUCTIONS:").setFont(fontBold).setFontSize(9).setFontColor(ColorConstants.GRAY));
            notesCell.add(new Paragraph(invoice.getNotes()).setFont(fontRegular).setFontSize(9));
        } else {
            notesCell.add(new Paragraph("Thank you for your business!").setFont(fontBold).setFontSize(10).setFontColor(accentColor));
            notesCell.add(new Paragraph("Please make payments within the due date to ensure smooth services.").setFont(fontRegular).setFontSize(8).setFontColor(ColorConstants.GRAY));
        }
        summaryTable.addCell(notesCell);

        Table rightTotals = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .useAllAvailableWidth()
                .setBorder(Border.NO_BORDER);

        addTotalRow(rightTotals, "Subtotal:", curr + String.format("%.2f", invoice.getSubtotal()), fontRegular, 9, false);
        if (invoice.getDiscount() > 0) {
            addTotalRow(rightTotals, "Discount:", "-" + curr + String.format("%.2f", invoice.getDiscount()), fontRegular, 9, false);
        }
        addTotalRow(rightTotals, "Tax (" + String.format("%.1f", invoice.getTaxRate()) + "%):", curr + String.format("%.2f", invoice.getTaxAmount()), fontRegular, 9, false);
        addTotalRow(rightTotals, "Grand Total:", curr + String.format("%.2f", invoice.getTotal()), fontBold, 13, true);

        Cell totalsCell = new Cell().setBorder(Border.NO_BORDER).add(rightTotals);
        summaryTable.addCell(totalsCell);

        doc.add(summaryTable);

        // Dynamic Footer aligned with Organisation Name & Timezone
        String footerText = (settings != null) 
                ? settings.getFooterNote(invoice.getGeneratedAt()) 
                : "Invoice generated on " + invoice.getFormattedGeneratedAt() + " • Generated by Swift Invoicing System • Offline Encrypted Record";
        Paragraph footer = new Paragraph("\n" + footerText)
                .setFont(fontRegular).setFontSize(8).setFontColor(ColorConstants.GRAY)
                .setTextAlignment(TextAlignment.CENTER);
        doc.add(footer);

        doc.close();
        return baos.toByteArray();
    }

    private void addTotalRow(Table table, String label, String value, PdfFont font, float size, boolean highlight) {
        Cell lCell = new Cell().setBorder(Border.NO_BORDER).setPadding(4)
                .setTextAlignment(TextAlignment.RIGHT)
                .add(new Paragraph(label).setFont(font).setFontSize(size));
        Cell vCell = new Cell().setBorder(Border.NO_BORDER).setPadding(4)
                .setTextAlignment(TextAlignment.RIGHT)
                .add(new Paragraph(value).setFont(font).setFontSize(size));
        if (highlight) {
            lCell.setFontColor(new DeviceRgb(37, 99, 235));
            vCell.setFontColor(new DeviceRgb(37, 99, 235));
            lCell.setBorderTop(new SolidBorder(new DeviceRgb(37, 99, 235), 1.5f));
            vCell.setBorderTop(new SolidBorder(new DeviceRgb(37, 99, 235), 1.5f));
        }
        table.addCell(lCell);
        table.addCell(vCell);
    }
}
