package com.example.invoiceapp.repository;

import com.example.invoiceapp.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByOrganizationIdAndDeletedFalseOrderByNameAsc(Long organizationId);
}
