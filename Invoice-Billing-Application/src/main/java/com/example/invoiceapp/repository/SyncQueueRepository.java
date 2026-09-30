package com.example.invoiceapp.repository;

import com.example.invoiceapp.model.SyncQueueItem;
import com.example.invoiceapp.model.enums.SyncStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SyncQueueRepository extends JpaRepository<SyncQueueItem, Long> {
    List<SyncQueueItem> findByStatusInOrderByCreatedAtAsc(List<SyncStatus> statuses);
    long countByStatus(SyncStatus status);
    Optional<SyncQueueItem> findByIdempotencyKey(String idempotencyKey);
    List<SyncQueueItem> findByOrganizationIdOrderByCreatedAtDesc(Long organizationId);
}
