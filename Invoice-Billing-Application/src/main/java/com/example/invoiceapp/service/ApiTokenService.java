package com.example.invoiceapp.service;

import com.example.invoiceapp.model.ApiToken;
import com.example.invoiceapp.model.User;
import com.example.invoiceapp.repository.ApiTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
public class ApiTokenService {

    private final ApiTokenRepository apiTokenRepo;

    public ApiTokenService(ApiTokenRepository apiTokenRepo) {
        this.apiTokenRepo = apiTokenRepo;
    }

    @Transactional
    public ApiToken createToken(User user) {
        String tokenString = UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
        ApiToken token = new ApiToken();
        token.setToken(tokenString);
        token.setUserId(user.getId());
        token.setOrganizationId(user.getOrganizationId());
        token.setCreatedAt(Instant.now());
        token.setExpiresAt(Instant.now().plus(60, ChronoUnit.DAYS));
        token.setRevoked(false);
        return apiTokenRepo.save(token);
    }

    public Optional<ApiToken> validateToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return Optional.empty();
        Optional<ApiToken> tokenOpt = apiTokenRepo.findByTokenAndRevokedFalse(rawToken.trim());
        if (tokenOpt.isPresent()) {
            ApiToken token = tokenOpt.get();
            if (token.getExpiresAt() != null && token.getExpiresAt().isBefore(Instant.now())) {
                return Optional.empty();
            }
            return Optional.of(token);
        }
        return Optional.empty();
    }

    @Transactional
    public void revokeToken(String rawToken) {
        if (rawToken == null) return;
        apiTokenRepo.findByTokenAndRevokedFalse(rawToken.trim()).ifPresent(token -> {
            token.setRevoked(true);
            apiTokenRepo.save(token);
        });
    }
}
