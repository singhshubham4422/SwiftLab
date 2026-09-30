package com.example.invoiceapp.service;

import com.example.invoiceapp.model.CompanySettings;
import com.example.invoiceapp.model.Invoice;
import com.example.invoiceapp.repository.InvoiceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class SupabaseSyncService {

    private static final Logger log = LoggerFactory.getLogger(SupabaseSyncService.class);

    private final InvoiceRepository invoiceRepository;
    private final CompanySettingsService settingsService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public SupabaseSyncService(InvoiceRepository invoiceRepository,
                               CompanySettingsService settingsService) {
        this.invoiceRepository = invoiceRepository;
        this.settingsService = settingsService;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(4))
                .build();
    }

    public boolean syncInvoice(Invoice invoice, CompanySettings settings) {
        if (settings == null || !settings.isSupabaseConfigured()) {
            return false;
        }

        try {
            String baseUrl = settings.getSupabaseUrl().trim();
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }
            URI targetUri = URI.create(baseUrl + "/rest/v1/invoices?on_conflict=invoice_number");

            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("invoice_number", invoice.getInvoiceNumber());
            payload.put("issue_date", invoice.getIssueDate() != null ? invoice.getIssueDate().toString() : null);
            payload.put("due_date", invoice.getDueDate() != null ? invoice.getDueDate().toString() : null);
            payload.put("payment_terms", invoice.getPaymentTerms());
            payload.put("customer_name", invoice.getCustomerName());
            payload.put("customer_email", invoice.getCustomerEmail());
            payload.put("customer_phone", invoice.getCustomerPhone());
            payload.put("customer_address", invoice.getCustomerAddress());
            payload.put("status", invoice.getStatus());
            payload.put("tax_rate", invoice.getTaxRate());
            payload.put("discount", invoice.getDiscount());
            payload.put("subtotal", invoice.getSubtotal());
            payload.put("total", invoice.getTotal());
            payload.put("notes", invoice.getNotes());
            payload.put("generated_at", invoice.getFormattedGeneratedAt());
            if (invoice.getUserId() != null) {
                payload.put("user_id", invoice.getUserId());
            }

            // Line items as JSON
            ArrayNode itemsNode = payload.putArray("items");
            if (invoice.getItems() != null) {
                for (var item : invoice.getItems()) {
                    ObjectNode itemObj = itemsNode.addObject();
                    itemObj.put("description", item.getDescription());
                    itemObj.put("quantity", item.getQuantity());
                    itemObj.put("unit_price", item.getUnitPrice());
                    itemObj.put("total", item.getTotal());
                }
            }

            String jsonBody = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(targetUri)
                    .timeout(Duration.ofSeconds(6))
                    .header("apikey", settings.getSupabaseKey().trim())
                    .header("Authorization", "Bearer " + settings.getSupabaseKey().trim())
                    .header("Content-Type", "application/json")
                    .header("Prefer", "resolution=merge-duplicates,return=minimal")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("Invoice #{} successfully synced to Supabase (HTTP {})", invoice.getInvoiceNumber(), response.statusCode());
                invoice.setSynced(true);
                invoiceRepository.save(invoice);
                return true;
            } else {
                log.warn("Supabase responded with status {} while syncing invoice #{}: {}", response.statusCode(), invoice.getInvoiceNumber(), response.body());
                return false;
            }
        } catch (Exception e) {
            log.info("Supabase sync offline/unreachable for invoice #{}: {}. Preserved in local H2 database.", invoice.getInvoiceNumber(), e.getMessage());
            return false;
        }
    }

    @Async
    public CompletableFuture<Boolean> syncInvoiceAsync(Invoice invoice, CompanySettings settings) {
        boolean success = syncInvoice(invoice, settings);
        return CompletableFuture.completedFuture(success);
    }

    public int syncAllUnsynced(CompanySettings settings) {
        if (settings == null || !settings.isSupabaseConfigured()) {
            return 0;
        }

        List<Invoice> unsynced = settings.getUserId() != null 
                ? invoiceRepository.findByUserIdAndSyncedFalse(settings.getUserId())
                : invoiceRepository.findBySyncedFalse();

        int count = 0;
        for (Invoice inv : unsynced) {
            if (syncInvoice(inv, settings)) {
                count++;
            }
        }
        return count;
    }

    // Legacy direct sync method; sync is now orchestrated via SyncQueueService
    public void scheduledSync() {
        CompanySettings settings = settingsService.getSettings();
        if (settings.isSupabaseConfigured() && settings.isAutoSync()) {
            List<Invoice> unsynced = invoiceRepository.findBySyncedFalse();
            if (!unsynced.isEmpty()) {
                log.info("Attempting background sync of {} pending offline invoices to Supabase...", unsynced.size());
                int synced = syncAllUnsynced(settings);
                if (synced > 0) {
                    log.info("Background sync completed: {} invoices synced to Supabase.", synced);
                }
            }
        }
    }

    public boolean testConnection(String url, String key) {
        if (url == null || url.trim().isEmpty() || key == null || key.trim().isEmpty()) {
            return false;
        }
        try {
            String baseUrl = url.trim();
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }
            URI targetUri = URI.create(baseUrl + "/rest/v1/");
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(targetUri)
                    .timeout(Duration.ofSeconds(4))
                    .header("apikey", key.trim())
                    .header("Authorization", "Bearer " + key.trim())
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() >= 200 && response.statusCode() < 400;
        } catch (Exception e) {
            log.warn("Supabase test connection failed: {}", e.getMessage());
            return false;
        }
    }

    public String getSupabaseTableSql() {
        return """
            -- Run this in your Supabase SQL Editor to create the invoices table:
            create table if not exists public.invoices (
                id bigint generated by default as identity primary key,
                invoice_number text not null unique,
                issue_date date,
                due_date date,
                payment_terms text,
                customer_name text,
                customer_email text,
                customer_phone text,
                customer_address text,
                status text default 'PENDING',
                tax_rate numeric,
                discount numeric default 0,
                subtotal numeric,
                total numeric,
                notes text,
                items jsonb default '[]'::jsonb,
                generated_at text,
                user_id bigint,
                created_at timestamp with time zone default timezone('utc'::text, now()) not null
            );

            -- Enable RLS and create an open access policy for application API key:
            alter table public.invoices enable row level security;
            create policy "Allow all operations with api key" on public.invoices
                for all using (true) with check (true);
            """;
    }
}
