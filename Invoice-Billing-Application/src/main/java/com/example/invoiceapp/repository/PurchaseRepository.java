package com.example.invoiceapp.repository;

import com.example.invoiceapp.model.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {
    List<Purchase> findByOrganizationIdAndDeletedFalseOrderByIdDesc(Long organizationId);
    Optional<Purchase> findByIdAndOrganizationId(Long id, Long organizationId);
    List<Purchase> findByOrganizationIdAndPurchaseDate(Long organizationId, LocalDate date);
}
