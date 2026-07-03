package com.bank.feature.sessions.domain;

import com.bank.feature.auth.persistence.RefreshTokenRepository;
import com.bank.feature.sessions.persistence.DeviceRecord;
import com.bank.feature.sessions.persistence.DeviceRecordRepository;
import com.bank.feature.sessions.web.dto.SessionView;
import com.bank.shared.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class DefaultSessionService implements SessionService {

    private final DeviceRecordRepository devices;
    private final RefreshTokenRepository tokens;

    public DefaultSessionService(DeviceRecordRepository devices, RefreshTokenRepository tokens) {
        this.devices = devices;
        this.tokens = tokens;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SessionView> listSessions(UUID userId) {
        return devices.findByUserIdOrderByLoggedInAtDesc(userId)
                .stream().map(SessionView::of).toList();
    }

    @Override
    @Transactional
    public void revokeSession(UUID userId, UUID sessionId) {
        DeviceRecord rec = devices.findByUserIdOrderByLoggedInAtDesc(userId)
                .stream()
                .filter(d -> d.getSessionId().equals(sessionId))
                .findFirst()
                .orElseThrow(() -> new ApiException("SESSION_NOT_FOUND", "Session not found", 404));
        tokens.revokeSession(rec.getSessionId());
    }

    @Override
    @Transactional
    public void revokeAllSessions(UUID userId, UUID currentSessionId) {
        devices.findByUserIdOrderByLoggedInAtDesc(userId)
                .stream()
                .filter(d -> !d.getSessionId().equals(currentSessionId))
                .map(DeviceRecord::getSessionId)
                .distinct()
                .forEach(tokens::revokeSession);
    }
}
