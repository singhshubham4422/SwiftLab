package com.example.invoiceapp.model;

import jakarta.persistence.*;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "company_settings")
public class CompanySettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private Long organizationId;

    private String userName;
    @Column(name = "company_name")
    private String companyName;
    @Column(name = "organization_name")
    private String organizationName;
    private String tagline = "Modern Enterprise Invoice Management";
    private String email;
    private String phone;
    private String address;

    private String currencySymbol = "₹";
    private String currencyCode = "INR";

    private double defaultTaxRate = 18.0;

    @Column(nullable = true)
    private Boolean configured = false;

    private String supabaseUrl = "";
    private String supabaseKey = "";

    @Column(nullable = true)
    private Boolean autoSync = false;

    public CompanySettings() {}

    public String getAppTitle() {
        if (organizationName != null && !organizationName.trim().isEmpty()) {
            return organizationName.trim() + " Inventory & Billing";
        }
        if (userName != null && !userName.trim().isEmpty()) {
            return userName.trim() + " Inventory & Billing";
        }
        return "SwiftLab Inventory & Billing";
    }

    public String getFooterNote(ZonedDateTime time) {
        String org = (organizationName != null && !organizationName.trim().isEmpty())
                ? organizationName.trim()
                : "SwiftLab";

        ZonedDateTime stamp = (time != null) ? time : ZonedDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");
        String formattedDate = stamp.format(formatter);

        return "Generated on " + formattedDate + " • " + org + " Inventory & Billing • Local-first records";
    }

    public String getFooterNote() {
        return getFooterNote(ZonedDateTime.now());
    }

    public boolean isSupabaseConfigured() {
        return supabaseUrl != null && !supabaseUrl.trim().isEmpty() &&
               supabaseKey != null && !supabaseKey.trim().isEmpty();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getOrganizationId() { return organizationId; }
    public void setOrganizationId(Long organizationId) { this.organizationId = organizationId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getOrganizationName() {
        if (organizationName != null && !organizationName.trim().isEmpty()) {
            return organizationName;
        }
        return companyName != null ? companyName : "";
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
        if (organizationName != null && !organizationName.isBlank()) {
            this.companyName = organizationName;
        }
    }

    public String getCompanyName() {
        if (companyName != null && !companyName.trim().isEmpty()) {
            return companyName;
        }
        return organizationName != null ? organizationName : "";
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
        if (companyName != null && !companyName.isBlank() && (this.organizationName == null || this.organizationName.isBlank())) {
            this.organizationName = companyName;
        }
    }

    public String getTagline() { return tagline; }
    public void setTagline(String tagline) { this.tagline = tagline; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCurrencySymbol() { return currencySymbol; }
    public void setCurrencySymbol(String currencySymbol) { this.currencySymbol = currencySymbol; }

    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }

    public double getDefaultTaxRate() { return defaultTaxRate; }
    public void setDefaultTaxRate(double defaultTaxRate) { this.defaultTaxRate = defaultTaxRate; }

    public boolean isConfigured() { return configured; }
    public void setConfigured(boolean configured) { this.configured = configured; }

    public String getSupabaseUrl() { return supabaseUrl; }
    public void setSupabaseUrl(String supabaseUrl) { this.supabaseUrl = supabaseUrl; }

    public String getSupabaseKey() { return supabaseKey; }
    public void setSupabaseKey(String supabaseKey) { this.supabaseKey = supabaseKey; }

    public boolean isAutoSync() { return autoSync; }
    public void setAutoSync(boolean autoSync) { this.autoSync = autoSync; }
}
