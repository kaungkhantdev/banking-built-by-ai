package com.bank.feature.storage.domain;

import com.bank.feature.storage.web.dto.FileMetaView;

import java.util.UUID;

public interface StorageService {

    /** Returns a pre-signed upload URL (stub: returns a local path). */
    record UploadUrlResponse(UUID fileId, String uploadUrl) {}

    UploadUrlResponse getUploadUrl(UUID ownerUserId, String fileName, String contentType);

    FileMetaView getDownloadUrl(UUID fileId, UUID ownerUserId);

    void delete(UUID fileId);
}
