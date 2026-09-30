package com.example.invoiceapp.repository;

import com.example.invoiceapp.model.Sale;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SaleRepository extends JpaRepository<Sale, Long> {
    List<Sale> findByOrganizationIdAndDeletedFalseOrderByIdDesc(Long organizationId);
    Optional<Sale> findByIdAndOrganizationId(Long id, Long organizationId);
    List<Sale> findByOrganizationIdAndSaleDate(Long organizationId, LocalDate date);
}
