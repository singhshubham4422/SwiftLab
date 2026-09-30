package com.example.invoiceapp.repository;

import com.example.invoiceapp.model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByUserId(Long userId);
    List<Invoice> findByUserIdOrderByIdDesc(Long userId);
    List<Invoice> findByOrganizationIdAndDeletedFalseOrderByIdDesc(Long organizationId);
    Optional<Invoice> findByIdAndOrganizationId(Long id, Long organizationId);
    Optional<Invoice> findByPublicIdAndOrganizationId(String publicId, Long organizationId);
    boolean existsByOrganizationIdAndInvoiceNumber(Long organizationId, String invoiceNumber);
    long countByOrganizationId(Long organizationId);
    List<Invoice> findBySyncedFalse();
    List<Invoice> findByUserIdAndSyncedFalse(Long userId);
}
