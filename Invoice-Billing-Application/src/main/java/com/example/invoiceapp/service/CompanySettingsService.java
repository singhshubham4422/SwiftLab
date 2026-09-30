package com.example.invoiceapp.service;

import com.example.invoiceapp.model.CompanySettings;
import com.example.invoiceapp.repository.CompanySettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompanySettingsService {

    private final CompanySettingsRepository repo;

    public CompanySettingsService(CompanySettingsRepository repo) {
        this.repo = repo;
    }

    public CompanySettings getSettingsForUser(Long userId) {
        if (userId != null) {
            return repo.findByUserId(userId).orElseGet(() -> {
                CompanySettings settings = new CompanySettings();
                settings.setUserId(userId);
                settings.setCompanyName("SwiftLab");
                settings.setOrganizationName("SwiftLab");
                settings.setCurrencySymbol("₹");
                settings.setCurrencyCode("INR");
                settings.setDefaultTaxRate(18.0);
                return repo.save(settings);
            });
        }
        return getSettings();
    }

    public CompanySettings getSettings() {
        return repo.findAll().stream().findFirst().orElseGet(() -> {
            CompanySettings defaults = new CompanySettings();
            defaults.setUserName("");
            defaults.setCompanyName("SwiftLab");
            defaults.setOrganizationName("");
            defaults.setTagline("Modern Enterprise Invoice Management");
            defaults.setEmail("");
            defaults.setPhone("");
            defaults.setAddress("");
            defaults.setCurrencySymbol("₹");
            defaults.setCurrencyCode("INR");
            defaults.setDefaultTaxRate(18.0);
            defaults.setConfigured(false);
            defaults.setAutoSync(true);
            return repo.save(defaults);
        });
    }

    @Transactional
    public CompanySettings saveSettings(CompanySettings settings) {
        CompanySettings current = settings.getUserId() != null 
                ? getSettingsForUser(settings.getUserId()) 
                : getSettings();
        
        if (settings.getOrganizationId() != null) {
            current.setOrganizationId(settings.getOrganizationId());
        }
        current.setUserName(settings.getUserName());
        current.setOrganizationName(settings.getOrganizationName());
        current.setCompanyName(settings.getOrganizationName() != null && !settings.getOrganizationName().isBlank()
                ? settings.getOrganizationName() : "SwiftLab");
        current.setTagline(settings.getTagline());
        current.setEmail(settings.getEmail());
        current.setPhone(settings.getPhone());
        current.setAddress(settings.getAddress());
        current.setCurrencySymbol(settings.getCurrencySymbol());
        current.setCurrencyCode(settings.getCurrencyCode());
        current.setDefaultTaxRate(settings.getDefaultTaxRate());
        current.setConfigured(true);
        current.setSupabaseUrl(settings.getSupabaseUrl());
        current.setSupabaseKey(settings.getSupabaseKey());
        current.setAutoSync(settings.isAutoSync());
        return repo.save(current);
    }

    public boolean isConfigured() {
        return getSettings().isConfigured();
    }
}
