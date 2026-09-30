package com.example.invoiceapp.repository;

import com.example.invoiceapp.model.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {
    List<Warehouse> findByOrganizationIdAndDeletedFalseOrderByNameAsc(Long organizationId);
    Optional<Warehouse> findFirstByOrganizationIdAndDefaultWarehouseTrueAndDeletedFalse(Long organizationId);
}
