package com.example.invoiceapp.repository;

import com.example.invoiceapp.model.ApiToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ApiTokenRepository extends JpaRepository<ApiToken, Long> {
    Optional<ApiToken> findByTokenAndRevokedFalse(String token);
}
