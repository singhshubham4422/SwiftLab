package com.example.invoiceapp.service;

import com.example.invoiceapp.dto.WarehouseDTO;
import com.example.invoiceapp.model.Warehouse;
import com.example.invoiceapp.repository.WarehouseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class WarehouseService {

    private final WarehouseRepository warehouseRepo;

    public WarehouseService(WarehouseRepository warehouseRepo) {
        this.warehouseRepo = warehouseRepo;
    }

    public List<WarehouseDTO> listByOrganization(Long organizationId) {
        if (organizationId == null) return List.of();
        ensureDefaultWarehouse(organizationId);
        return warehouseRepo.findByOrganizationIdAndDeletedFalseOrderByNameAsc(organizationId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public Optional<Warehouse> findById(Long id, Long organizationId) {
        if (id == null || organizationId == null) return Optional.empty();
        return warehouseRepo.findById(id)
                .filter(w -> !w.isDeleted() && w.getOrganizationId() != null && w.getOrganizationId().equals(organizationId));
    }

    public Warehouse get(Long id, Long organizationId) {
        return findById(id, organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Warehouse not found or not owned by organization"));
    }

    @Transactional
    public Warehouse ensureDefaultWarehouse(Long organizationId) {
        return warehouseRepo.findFirstByOrganizationIdAndDefaultWarehouseTrueAndDeletedFalse(organizationId)
                .orElseGet(() -> {
                    Warehouse w = new Warehouse();
                    w.setOrganizationId(organizationId);
                    w.setName("Main Warehouse");
                    w.setLocation("Default Facility");
                    w.setDefaultWarehouse(true);
                    w.setCreatedAt(Instant.now());
                    w.setUpdatedAt(Instant.now());
                    return warehouseRepo.save(w);
                });
    }

    @Transactional
    public Warehouse create(Long organizationId, String name, String location, boolean isDefault) {
        if (isDefault) {
            // Unset previous defaults
            warehouseRepo.findFirstByOrganizationIdAndDefaultWarehouseTrueAndDeletedFalse(organizationId)
                    .ifPresent(prev -> {
                        prev.setDefaultWarehouse(false);
                        warehouseRepo.save(prev);
                    });
        }
        Warehouse w = new Warehouse();
        w.setOrganizationId(organizationId);
        w.setName(name.trim());
        w.setLocation(location != null ? location.trim() : "");
        w.setDefaultWarehouse(isDefault);
        w.setCreatedAt(Instant.now());
        w.setUpdatedAt(Instant.now());
        return warehouseRepo.save(w);
    }

    @Transactional
    public Warehouse update(Long id, Long organizationId, String name, String location, boolean isDefault) {
        Warehouse w = get(id, organizationId);
        if (isDefault && !w.isDefaultWarehouse()) {
            warehouseRepo.findFirstByOrganizationIdAndDefaultWarehouseTrueAndDeletedFalse(organizationId)
                    .ifPresent(prev -> {
                        prev.setDefaultWarehouse(false);
                        warehouseRepo.save(prev);
                    });
        }
        w.setName(name.trim());
        w.setLocation(location != null ? location.trim() : "");
        w.setDefaultWarehouse(isDefault);
        w.setUpdatedAt(Instant.now());
        return warehouseRepo.save(w);
    }

    @Transactional
    public void delete(Long id, Long organizationId) {
        Warehouse w = get(id, organizationId);
        w.markDeleted();
        warehouseRepo.save(w);
    }

    public WarehouseDTO toDTO(Warehouse w) {
        if (w == null) return null;
        return new WarehouseDTO(
                w.getId(),
                w.getPublicId(),
                w.getName(),
                w.getLocation(),
                w.isDefaultWarehouse()
        );
    }
}
