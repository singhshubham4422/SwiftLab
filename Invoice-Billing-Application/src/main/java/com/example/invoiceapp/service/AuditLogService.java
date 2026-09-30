package com.example.invoiceapp.service;

import com.example.invoiceapp.dto.AuditLogDTO;
import com.example.invoiceapp.model.AuditLog;
import com.example.invoiceapp.model.User;
import com.example.invoiceapp.repository.AuditLogRepository;
import com.example.invoiceapp.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepo;
    private final UserRepository userRepo;

    public AuditLogService(AuditLogRepository auditLogRepo, UserRepository userRepo) {
        this.auditLogRepo = auditLogRepo;
        this.userRepo = userRepo;
    }

    @Transactional
    public AuditLog record(Long organizationId, Long userId, String deviceId, String action, String entityType, String entityId, String metadata) {
        AuditLog log = new AuditLog();
        log.setOrganizationId(organizationId);
        log.setUserId(userId);
        log.setDeviceId(deviceId);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setMetadata(metadata);
        log.setCreatedAt(Instant.now());
        return auditLogRepo.save(log);
    }

    public List<AuditLogDTO> getRecentLogs(Long organizationId) {
        if (organizationId == null) return List.of();
        List<AuditLog> logs = auditLogRepo.findTop50ByOrganizationIdOrderByCreatedAtDesc(organizationId);
        Map<Long, String> userEmailMap = userRepo.findByOrganizationId(organizationId).stream()
                .collect(Collectors.toMap(User::getId, User::getEmail, (a, b) -> a));

        return logs.stream().map(log -> {
            AuditLogDTO dto = new AuditLogDTO();
            dto.setId(log.getId());
            dto.setOrganizationId(log.getOrganizationId());
            dto.setUserId(log.getUserId());
            dto.setUserEmail(log.getUserId() != null ? userEmailMap.getOrDefault(log.getUserId(), "System") : "System");
            dto.setDeviceId(log.getDeviceId());
            dto.setAction(log.getAction());
            dto.setEntityType(log.getEntityType());
            dto.setEntityId(log.getEntityId());
            dto.setCreatedAt(log.getCreatedAt());
            dto.setMetadata(log.getMetadata());
            return dto;
        }).collect(Collectors.toList());
    }
}
