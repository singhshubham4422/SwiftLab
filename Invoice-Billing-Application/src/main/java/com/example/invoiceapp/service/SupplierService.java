package com.example.invoiceapp.service;

import com.example.invoiceapp.dto.SupplierDTO;
import com.example.invoiceapp.model.Supplier;
import com.example.invoiceapp.repository.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SupplierService {

    private final SupplierRepository supplierRepo;

    public SupplierService(SupplierRepository supplierRepo) {
        this.supplierRepo = supplierRepo;
    }

    public List<SupplierDTO> listByOrganization(Long organizationId) {
        if (organizationId == null) return List.of();
        return supplierRepo.findByOrganizationIdAndDeletedFalseOrderByNameAsc(organizationId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public Optional<Supplier> findById(Long id, Long organizationId) {
        if (id == null || organizationId == null) return Optional.empty();
        return supplierRepo.findByIdAndOrganizationId(id, organizationId)
                .filter(s -> !s.isDeleted());
    }

    public Supplier get(Long id, Long organizationId) {
        return findById(id, organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found with ID: " + id));
    }

    @Transactional
    public Supplier save(Long organizationId, SupplierDTO dto) {
        if (organizationId == null) throw new IllegalArgumentException("Organization ID is required");

        Supplier supplier;
        if (dto.getId() != null) {
            supplier = get(dto.getId(), organizationId);
        } else {
            supplier = new Supplier();
            supplier.setOrganizationId(organizationId);
            supplier.setCreatedAt(Instant.now());
        }

        supplier.setName(dto.getName() != null ? dto.getName().trim() : "Unnamed Supplier");
        supplier.setPhone(dto.getPhone());
        supplier.setEmail(dto.getEmail());
        supplier.setAddress(dto.getAddress());
        supplier.setTaxNumber(dto.getTaxNumber());
        supplier.setNotes(dto.getNotes());
        supplier.setUpdatedAt(Instant.now());

        return supplierRepo.save(supplier);
    }

    @Transactional
    public void delete(Long id, Long organizationId) {
        Supplier s = get(id, organizationId);
        s.markDeleted();
        supplierRepo.save(s);
    }

    public SupplierDTO toDTO(Supplier s) {
        if (s == null) return null;
        SupplierDTO dto = new SupplierDTO();
        dto.setId(s.getId());
        dto.setPublicId(s.getPublicId());
        dto.setName(s.getName());
        dto.setPhone(s.getPhone());
        dto.setEmail(s.getEmail());
        dto.setAddress(s.getAddress());
        dto.setTaxNumber(s.getTaxNumber());
        dto.setNotes(s.getNotes());
        return dto;
    }
}
