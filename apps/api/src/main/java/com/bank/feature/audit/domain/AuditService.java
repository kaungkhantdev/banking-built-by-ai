package com.bank.feature.audit.domain;

/** Port: write append-only audit records. */
public interface AuditService {

    void write(String actor, String action, Object before, Object after);
}
