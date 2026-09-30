package com.example.invoiceapp.repository;

import com.example.invoiceapp.model.Brand;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BrandRepository extends JpaRepository<Brand, Long> {
    List<Brand> findByOrganizationIdAndDeletedFalseOrderByNameAsc(Long organizationId);
}
