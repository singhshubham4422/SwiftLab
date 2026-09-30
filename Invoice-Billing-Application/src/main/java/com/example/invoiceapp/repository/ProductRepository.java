package com.example.invoiceapp.repository;

import com.example.invoiceapp.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByOrganizationIdAndDeletedFalseOrderByNameAsc(Long organizationId);
    Optional<Product> findByIdAndOrganizationId(Long id, Long organizationId);
    Optional<Product> findByPublicIdAndOrganizationId(String publicId, Long organizationId);
    boolean existsByOrganizationIdAndSkuAndDeletedFalse(Long organizationId, String sku);
    Optional<Product> findByOrganizationIdAndSkuAndDeletedFalse(Long organizationId, String sku);
}
