package com.madhurgram.productservice.audit.service.impl;

import com.madhurgram.productservice.audit.entity.AuditLog;
import com.madhurgram.productservice.audit.repository.AuditLogRepository;
import com.madhurgram.productservice.audit.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * Service implementation for writing enterprise security audit log trails 
 * capturing admin action details, target entities, and request IP addresses.
 */
@Slf4j
@Service
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository repository;

    /**
     * Constructor injection for AuditLogServiceImpl.
     *
     * @param repository audit log database repository
     */
    public AuditLogServiceImpl(AuditLogRepository repository) {
        this.repository = repository;
    }

    /**
     * Records administrative or system action details into audit trail logs.
     *
     * @param action   action label keyword
     * @param entityId target modified entity identifier index
     * @param details  action parameters summary description
     */
    @Override
    @Transactional
    public void log(String action, String entityId, String details) {
        if (action == null || action.trim().isEmpty()) {
            throw new IllegalArgumentException("Audit action must not be null or blank.");
        }

        String username = getAuthenticatedUsername();
        String ipAddress = getClientIpAddress();

        AuditLog auditLog = AuditLog.builder()
                .username(username)
                .action(action.trim())
                .entityId(entityId != null ? entityId.trim() : null)
                .details(details != null ? details.trim() : null)
                .ipAddress(ipAddress)
                .createdAt(LocalDateTime.now())
                .build();

        try {
            repository.save(auditLog);
            log.info("Audit log written successfully: [{}] by user: '{}' on entity: '{}'", action, username, entityId);
        } catch (Exception e) {
            log.error("Failed to persist security audit log entry. Error: {}", e.getMessage(), e);
        }
    }

    private String getAuthenticatedUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return auth.getName();
        }
        return "SYSTEM";
    }

    private String getClientIpAddress() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String xfHeader = request.getHeader("X-Forwarded-For");
                if (xfHeader == null || xfHeader.isEmpty()) {
                    return request.getRemoteAddr();
                }
                return xfHeader.split(",")[0].trim();
            }
        } catch (Exception e) {
            log.debug("Could not resolve request attributes context for IP extraction: {}", e.getMessage());
        }
        return "UNKNOWN";
    }
}
