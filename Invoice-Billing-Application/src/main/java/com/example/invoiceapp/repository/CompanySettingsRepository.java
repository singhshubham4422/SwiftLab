package com.example.invoiceapp.repository;

import com.example.invoiceapp.model.CompanySettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanySettingsRepository extends JpaRepository<CompanySettings, Long> {
    Optional<CompanySettings> findByUserId(Long userId);
    Optional<CompanySettings> findByOrganizationId(Long organizationId);
}
