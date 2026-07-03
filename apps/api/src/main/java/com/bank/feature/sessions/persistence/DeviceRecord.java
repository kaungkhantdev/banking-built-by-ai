package com.bank.feature.sessions.persistence;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "device_records")
public class DeviceRecord {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID sessionId;

    @Column
    private String userAgent;

    @Column
    private String ipAddress;

    @Column
    private String deviceFingerprint;

    @Column(nullable = false)
    private Instant loggedInAt;

    protected DeviceRecord() {}

    public DeviceRecord(UUID userId, UUID sessionId, String userAgent,
                         String ipAddress, String deviceFingerprint) {
        this.userId = userId;
        this.sessionId = sessionId;
        this.userAgent = userAgent;
        this.ipAddress = ipAddress;
        this.deviceFingerprint = deviceFingerprint;
        this.loggedInAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getSessionId() { return sessionId; }
    public String getUserAgent() { return userAgent; }
    public String getIpAddress() { return ipAddress; }
    public Instant getLoggedInAt() { return loggedInAt; }
}
