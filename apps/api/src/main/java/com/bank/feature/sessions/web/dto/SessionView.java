package com.bank.feature.sessions.web.dto;

import com.bank.feature.sessions.persistence.DeviceRecord;

import java.time.Instant;
import java.util.UUID;

public record SessionView(
        UUID sessionId,
        String userAgent,
        String ipAddress,
        Instant loggedInAt
) {
    public static SessionView of(DeviceRecord d) {
        return new SessionView(d.getSessionId(), d.getUserAgent(), d.getIpAddress(), d.getLoggedInAt());
    }
}
