package com.example.invoiceapp.repository;

import com.example.invoiceapp.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    List<Customer> findByOrganizationIdAndDeletedFalseOrderByNameAsc(Long organizationId);
    Optional<Customer> findByIdAndOrganizationId(Long id, Long organizationId);
    Optional<Customer> findByPublicIdAndOrganizationId(String publicId, Long organizationId);
}
