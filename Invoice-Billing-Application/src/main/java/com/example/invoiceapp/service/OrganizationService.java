package com.example.invoiceapp.service;

import com.example.invoiceapp.dto.OrganizationDTO;
import com.example.invoiceapp.model.Organization;
import com.example.invoiceapp.model.enums.DataMode;
import com.example.invoiceapp.repository.OrganizationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class OrganizationService {

    private final OrganizationRepository orgRepo;

    public OrganizationService(OrganizationRepository orgRepo) {
        this.orgRepo = orgRepo;
    }

    public Optional<Organization> findById(Long id) {
        if (id == null) return Optional.empty();
        return orgRepo.findById(id);
    }

    public Organization get(Long id) {
        return findById(id).orElseThrow(() -> new IllegalArgumentException("Organization not found with ID: " + id));
    }

    @Transactional
    public Organization createOrganization(String name, DataMode preferredMode) {
        Organization org = new Organization();
        org.setName(name.trim());
        org.setPreferredDataMode(preferredMode != null ? preferredMode : DataMode.LOCAL_ONLY);
        org.setCreatedAt(Instant.now());
        org.setUpdatedAt(Instant.now());
        return orgRepo.save(org);
    }

    @Transactional
    public Organization updateOrganization(Long id, String name, boolean allowNegativeInventory) {
        Organization org = get(id);
        if (name != null && !name.isBlank()) {
            org.setName(name.trim());
        }
        org.setAllowNegativeInventory(allowNegativeInventory);
        org.setUpdatedAt(Instant.now());
        return orgRepo.save(org);
    }

    public OrganizationDTO toDTO(Organization org) {
        if (org == null) return null;
        return new OrganizationDTO(
                org.getId(),
                org.getPublicId(),
                org.getName(),
                org.isAllowNegativeInventory(),
                org.getPreferredDataMode()
        );
    }
}
