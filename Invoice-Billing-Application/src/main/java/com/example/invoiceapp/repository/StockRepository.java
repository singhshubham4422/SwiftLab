package com.example.invoiceapp.repository;

import com.example.invoiceapp.model.Stock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {
    List<Stock> findByOrganizationId(Long organizationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Stock> findByOrganizationIdAndWarehouseIdAndProductId(Long organizationId, Long warehouseId, Long productId);
}
