package com.example.invoiceapp.repository;

import com.example.invoiceapp.model.AppRuntime;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppRuntimeRepository extends JpaRepository<AppRuntime, Long> {
}
