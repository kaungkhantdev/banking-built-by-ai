package com.bank.feature.storage.domain;

import com.bank.feature.storage.web.dto.FileMetaView;

import java.util.UUID;

public interface StorageService {

    /** A pre-signed upload URL with an expiry the client must honor (FR-27.4). */
    record UploadUrlResponse(UUID fileId, String uploadUrl, java.time.Instant expiresAt) {}

    UploadUrlResponse getUploadUrl(UUID ownerUserId, String fileName, String contentType);

    /** Accept, virus-scan (FR-27.2), and encrypt-at-rest (FR-27.3) the file bytes. */
    FileMetaView uploadContent(UUID fileId, UUID ownerUserId, byte[] content);

    /** Decrypt and return the stored bytes. */
    byte[] getContent(UUID fileId, UUID ownerUserId);

    FileMetaView getDownloadUrl(UUID fileId, UUID ownerUserId);

    void delete(UUID fileId);
}
