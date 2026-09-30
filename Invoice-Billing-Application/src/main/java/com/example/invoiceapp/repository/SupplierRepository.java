package com.example.invoiceapp.repository;

import com.example.invoiceapp.model.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    List<Supplier> findByOrganizationIdAndDeletedFalseOrderByNameAsc(Long organizationId);
    Optional<Supplier> findByIdAndOrganizationId(Long id, Long organizationId);
}
