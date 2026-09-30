package com.example.invoiceapp.repository;

import com.example.invoiceapp.model.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    List<StockMovement> findByOrganizationIdOrderByCreatedAtDesc(Long organizationId);
    Optional<StockMovement> findByPublicIdAndOrganizationId(String publicId, Long organizationId);
    List<StockMovement> findByOrganizationIdAndProductIdOrderByCreatedAtAsc(Long organizationId, Long productId);
}
