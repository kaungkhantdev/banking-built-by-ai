package com.bank.feature.audit.domain;

import com.bank.feature.audit.persistence.AuditRecord;
import com.bank.feature.audit.persistence.AuditRecordRepository;
import com.bank.shared.exception.ApiError;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes audit records. Joins the caller's transaction by default so an action
 * and its audit row commit together — the audit is part of the business change,
 * not a best-effort side note.
 */
@Service
public class DefaultAuditService implements AuditService {

    private final AuditRecordRepository repo;
    private final ObjectMapper mapper;

    public DefaultAuditService(AuditRecordRepository repo, ObjectMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public void write(String actor, String action, Object before, Object after) {
        repo.save(new AuditRecord(actor, action, toJson(before), toJson(after),
                ApiError.of("", "").traceId()));
    }

    private String toJson(Object o) {
        try {
            return o == null ? null : mapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            return "\"<unserializable>\"";
        }
    }
}
