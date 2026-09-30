package com.example.invoiceapp.repository;

import com.example.invoiceapp.model.AppliedSyncKey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppliedSyncKeyRepository extends JpaRepository<AppliedSyncKey, Long> {
    boolean existsByOrganizationIdAndIdempotencyKey(Long organizationId, String idempotencyKey);
}
