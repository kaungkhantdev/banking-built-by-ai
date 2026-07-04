package com.bank.feature.storage.persistence;

import com.bank.shared.entity.BaseAuditEntity;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "file_metas")
public class FileMeta extends BaseAuditEntity {

    @Column(nullable = false)
    private UUID ownerUserId;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String contentType;

    @Column
    private Long sizeBytes;

    @Column(nullable = false, unique = true)
    private String storageKey;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private boolean deleted;

    /** AES-256-GCM ciphertext of the file bytes (FR-27.3). */
    @Column(columnDefinition = "TEXT")
    private String contentEncrypted;

    /** After this instant the pre-signed upload URL is rejected (FR-27.4). */
    @Column
    private Instant uploadExpiresAt;

    protected FileMeta() {}

    public FileMeta(UUID ownerUserId, String fileName, String contentType,
                     Long sizeBytes, String storageKey) {
        this.ownerUserId = ownerUserId;
        this.fileName = fileName;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.storageKey = storageKey;
        this.status = "PENDING";
        this.deleted = false;
    }

    public UUID getOwnerUserId() { return ownerUserId; }
    public String getFileName() { return fileName; }
    public String getContentType() { return contentType; }
    public Long getSizeBytes() { return sizeBytes; }
    public String getStorageKey() { return storageKey; }
    public String getStatus() { return status; }
    public boolean isDeleted() { return deleted; }
    public String getContentEncrypted() { return contentEncrypted; }
    public Instant getUploadExpiresAt() { return uploadExpiresAt; }

    public void setStatus(String status) { this.status = status; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
    public void setSizeBytes(Long sizeBytes) { this.sizeBytes = sizeBytes; }
    public void setContentEncrypted(String contentEncrypted) { this.contentEncrypted = contentEncrypted; }
    public void setUploadExpiresAt(Instant uploadExpiresAt) { this.uploadExpiresAt = uploadExpiresAt; }
}
