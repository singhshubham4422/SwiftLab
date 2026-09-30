package com.example.invoiceapp.repository;

import com.example.invoiceapp.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    Optional<Organization> findByPublicId(String publicId);
}
