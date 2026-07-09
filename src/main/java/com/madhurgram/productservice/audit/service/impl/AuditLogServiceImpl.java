package com.madhurgram.productservice.audit.service.impl;

import com.madhurgram.productservice.audit.entity.AuditLog;
import com.madhurgram.productservice.audit.repository.AuditLogRepository;
import com.madhurgram.productservice.audit.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogServiceImpl.class);
    private final AuditLogRepository repository;

    public AuditLogServiceImpl(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void log(String action, String entityId, String details) {
        String username = getAuthenticatedUsername();
        String ipAddress = getClientIpAddress();

        AuditLog auditLog = AuditLog.builder()
                .username(username)
                .action(action)
                .entityId(entityId)
                .details(details)
                .ipAddress(ipAddress)
                .createdAt(LocalDateTime.now())
                .build();

        try {
            repository.save(auditLog);
            log.info("Audit log written: [{}] by user: {} on entity: {}", action, username, entityId);
        } catch (Exception e) {
            log.error("Failed to write audit log: {}", e.getMessage(), e);
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
            log.debug("Could not resolve request attributes for IP extraction: {}", e.getMessage());
        }
        return "UNKNOWN";
    }
}
