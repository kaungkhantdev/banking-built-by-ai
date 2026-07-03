package com.bank.feature.sessions.domain;

import com.bank.feature.sessions.web.dto.SessionView;

import java.util.List;
import java.util.UUID;

public interface SessionService {

    List<SessionView> listSessions(UUID userId);

    void revokeSession(UUID userId, UUID sessionId);

    void revokeAllSessions(UUID userId, UUID currentSessionId);
}
