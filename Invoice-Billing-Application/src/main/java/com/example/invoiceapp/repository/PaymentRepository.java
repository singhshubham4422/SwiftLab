package com.example.invoiceapp.repository;

import com.example.invoiceapp.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByOrganizationIdAndDeletedFalseOrderByIdDesc(Long organizationId);
    List<Payment> findByInvoiceIdAndDeletedFalse(Long invoiceId);
}
