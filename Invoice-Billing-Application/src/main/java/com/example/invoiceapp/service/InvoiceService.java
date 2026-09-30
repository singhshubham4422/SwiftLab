package com.example.invoiceapp.service;

import com.example.invoiceapp.dto.InvoiceCreateRequest;
import com.example.invoiceapp.dto.InvoiceDTO;
import com.example.invoiceapp.dto.InvoiceItemDTO;
import com.example.invoiceapp.model.CompanySettings;
import com.example.invoiceapp.model.Customer;
import com.example.invoiceapp.model.Invoice;
import com.example.invoiceapp.model.InvoiceItem;
import com.example.invoiceapp.model.enums.SyncOperation;
import com.example.invoiceapp.repository.CustomerRepository;
import com.example.invoiceapp.repository.InvoiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class InvoiceService {

    private final InvoiceRepository repo;
    private final CustomerRepository customerRepo;
    private final PdfInvoiceExporter pdfExporter;
    private final ExcelInvoiceExporter excelExporter;
    private final CompanySettingsService settingsService;
    private final SyncQueueService syncQueueService;

    public InvoiceService(InvoiceRepository repo,
                          CustomerRepository customerRepo,
                          PdfInvoiceExporter pdfExporter,
                          ExcelInvoiceExporter excelExporter,
                          CompanySettingsService settingsService,
                          SyncQueueService syncQueueService) {
        this.repo = repo;
        this.customerRepo = customerRepo;
        this.pdfExporter = pdfExporter;
        this.excelExporter = excelExporter;
        this.settingsService = settingsService;
        this.syncQueueService = syncQueueService;
    }

    public List<Invoice> listAll() {
        return repo.findAll();
    }

    public List<Invoice> listByOrganization(Long organizationId) {
        if (organizationId == null) return List.of();
        return repo.findByOrganizationIdAndDeletedFalseOrderByIdDesc(organizationId);
    }

    public List<InvoiceDTO> listDTOByOrganization(Long organizationId) {
        return listByOrganization(organizationId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<Invoice> listByUserId(Long userId) {
        if (userId == null) {
            return repo.findAll();
        }
        return repo.findByUserIdOrderByIdDesc(userId);
    }

    public Optional<Invoice> findById(Long id) {
        return repo.findById(id).filter(i -> !i.isDeleted());
    }

    public Optional<Invoice> findByIdAndOrganization(Long id, Long organizationId) {
        if (id == null || organizationId == null) return Optional.empty();
        return repo.findByIdAndOrganizationId(id, organizationId).filter(i -> !i.isDeleted());
    }

    public Invoice get(Long id) {
        return findById(id).orElseThrow(() -> new IllegalArgumentException("Invoice with id " + id + " not found"));
    }

    public Invoice getForOrganization(Long id, Long organizationId) {
        return findByIdAndOrganization(id, organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found or not owned by organization"));
    }

    @Transactional
    public Invoice createInvoice(Long organizationId, Long userId, InvoiceCreateRequest req) {
        Invoice invoice = new Invoice();
        invoice.setOrganizationId(organizationId);
        invoice.setUserId(userId);
        invoice.setInvoiceNumber(generateNextInvoiceNumber(organizationId));
        invoice.setIssueDate(req.getIssueDate() != null ? req.getIssueDate() : LocalDate.now());
        invoice.setDueDate(req.getDueDate() != null ? req.getDueDate() : invoice.getIssueDate().plusDays(15));
        invoice.setPaymentTerms(req.getPaymentTerms());
        invoice.setTaxRate(req.getTaxRate());
        invoice.setDiscount(req.getDiscount());
        invoice.setNotes(req.getNotes());
        invoice.setGeneratedAt(ZonedDateTime.now());
        invoice.setCreatedAt(Instant.now());
        invoice.setUpdatedAt(Instant.now());

        if (req.getCustomerId() != null) {
            customerRepo.findByIdAndOrganizationId(req.getCustomerId(), organizationId).ifPresent(c -> {
                invoice.setCustomerId(c.getId());
                invoice.setCustomerName(c.getName());
                invoice.setCustomerEmail(c.getEmail());
                invoice.setCustomerPhone(c.getPhone());
                invoice.setCustomerAddress(c.getAddress());
            });
        } else {
            invoice.setCustomerName(req.getCustomerName());
            invoice.setCustomerEmail(req.getCustomerEmail());
            invoice.setCustomerPhone(req.getCustomerPhone());
            invoice.setCustomerAddress(req.getCustomerAddress());
        }

        if (req.getItems() != null) {
            for (InvoiceItemDTO itemDto : req.getItems()) {
                if (itemDto.getDescription() != null && !itemDto.getDescription().isBlank()) {
                    invoice.addItem(new InvoiceItem(itemDto.getDescription(), itemDto.getQuantity(), itemDto.getUnitPrice()));
                }
            }
        }

        invoice.refreshPaymentStatus();
        Invoice saved = repo.save(invoice);

        syncQueueService.enqueue(
                organizationId,
                "Invoice",
                saved.getPublicId(),
                SyncOperation.CREATE,
                toDTO(saved),
                null
        );

        return saved;
    }

    @Transactional
    public Invoice save(Invoice invoice) {
        if (invoice.getInvoiceNumber() == null || invoice.getInvoiceNumber().trim().isEmpty()) {
            invoice.setInvoiceNumber(generateNextInvoiceNumber(invoice.getOrganizationId()));
        }
        if (invoice.getIssueDate() == null) {
            invoice.setIssueDate(LocalDate.now());
        }
        if (invoice.getDueDate() == null) {
            invoice.setDueDate(invoice.getIssueDate().plusDays(15));
        }
        if (invoice.getGeneratedAt() == null) {
            invoice.setGeneratedAt(ZonedDateTime.now());
        }
        if (invoice.getItems() != null) {
            invoice.getItems().removeIf(item -> item.getDescription() == null || item.getDescription().trim().isEmpty());
        }

        invoice.refreshPaymentStatus();
        Invoice saved = repo.save(invoice);

        if (saved.getOrganizationId() != null) {
            syncQueueService.enqueue(
                    saved.getOrganizationId(),
                    "Invoice",
                    saved.getPublicId(),
                    SyncOperation.UPDATE,
                    toDTO(saved),
                    null
            );
        }

        return saved;
    }

    @Transactional
    public void delete(Long id) {
        Invoice inv = get(id);
        inv.markDeleted();
        repo.save(inv);

        if (inv.getOrganizationId() != null) {
            syncQueueService.enqueue(
                    inv.getOrganizationId(),
                    "Invoice",
                    inv.getPublicId(),
                    SyncOperation.DELETE,
                    null,
                    null
            );
        }
    }

    @Transactional
    public void updateStatus(Long id, String status) {
        Invoice invoice = get(id);
        invoice.setStatus(status);
        repo.save(invoice);

        if (invoice.getOrganizationId() != null) {
            syncQueueService.enqueue(
                    invoice.getOrganizationId(),
                    "Invoice",
                    invoice.getPublicId(),
                    SyncOperation.UPDATE,
                    toDTO(invoice),
                    null
            );
        }
    }

    public String generateNextInvoiceNumber(Long organizationId) {
        long count = organizationId != null ? repo.countByOrganizationId(organizationId) + 1 : repo.count() + 1;
        return String.format("INV-%04d", count);
    }

    public String generateNextInvoiceNumber() {
        return generateNextInvoiceNumber(null);
    }

    public byte[] exportPdf(Invoice invoice) throws IOException {
        CompanySettings settings = invoice.getUserId() != null 
                ? settingsService.getSettingsForUser(invoice.getUserId()) 
                : settingsService.getSettings();
        return pdfExporter.exportPdf(invoice, settings);
    }

    public byte[] exportExcel(Invoice invoice) throws IOException {
        CompanySettings settings = invoice.getUserId() != null 
                ? settingsService.getSettingsForUser(invoice.getUserId()) 
                : settingsService.getSettings();
        return excelExporter.exportExcel(invoice, settings);
    }

    public byte[] generateSampleExcelTemplate(Long userId) throws IOException {
        CompanySettings settings = userId != null 
                ? settingsService.getSettingsForUser(userId) 
                : settingsService.getSettings();
        return excelExporter.generateSampleExcelTemplate(settings);
    }

    @Transactional
    public Invoice importFromExcel(MultipartFile file, Long userId, Long organizationId) throws IOException {
        Invoice invoice = excelExporter.parseExcel(file);
        if (userId != null) {
            invoice.setUserId(userId);
        }
        if (organizationId != null) {
            invoice.setOrganizationId(organizationId);
        }
        return save(invoice);
    }

    @Transactional
    public Invoice importFromExcel(MultipartFile file, Long userId) throws IOException {
        return importFromExcel(file, userId, null);
    }

    public InvoiceDTO toDTO(Invoice inv) {
        if (inv == null) return null;
        InvoiceDTO dto = new InvoiceDTO();
        dto.setId(inv.getId());
        dto.setPublicId(inv.getPublicId());
        dto.setInvoiceNumber(inv.getInvoiceNumber());
        dto.setUserId(inv.getUserId());
        dto.setCustomerId(inv.getCustomerId());
        dto.setCustomerName(inv.getCustomerName());
        dto.setCustomerEmail(inv.getCustomerEmail());
        dto.setCustomerPhone(inv.getCustomerPhone());
        dto.setCustomerAddress(inv.getCustomerAddress());
        dto.setIssueDate(inv.getIssueDate());
        dto.setDueDate(inv.getDueDate());
        dto.setPaymentTerms(inv.getPaymentTerms());
        dto.setStatus(inv.getStatus());
        dto.setTaxRate(inv.getTaxRate());
        dto.setDiscount(inv.getDiscount());
        dto.setSubtotal(inv.getSubtotal());
        dto.setTaxAmount(inv.getTaxAmount());
        dto.setTotal(inv.getTotal());
        dto.setAmountPaid(inv.getAmountPaid());
        dto.setOutstanding(inv.getOutstanding());
        dto.setNotes(inv.getNotes());
        dto.setSaleId(inv.getSaleId());
        dto.setSynced(inv.isSynced());

        if (inv.getItems() != null) {
            List<InvoiceItemDTO> itemDTOs = new ArrayList<>();
            for (InvoiceItem item : inv.getItems()) {
                itemDTOs.add(new InvoiceItemDTO(item.getDescription(), item.getQuantity(), item.getUnitPrice()));
            }
            dto.setItems(itemDTOs);
        }
        return dto;
    }
}
