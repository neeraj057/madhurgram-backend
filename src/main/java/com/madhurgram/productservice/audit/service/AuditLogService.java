package com.madhurgram.productservice.audit.service;

public interface AuditLogService {
    void log(String action, String entityId, String details);
}
