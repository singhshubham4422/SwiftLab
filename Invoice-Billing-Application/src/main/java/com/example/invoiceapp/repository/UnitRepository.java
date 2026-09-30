package com.example.invoiceapp.repository;

import com.example.invoiceapp.model.Unit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UnitRepository extends JpaRepository<Unit, Long> {
    List<Unit> findByOrganizationIdAndDeletedFalseOrderByNameAsc(Long organizationId);
}
