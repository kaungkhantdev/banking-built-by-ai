package com.bank.feature.storage.web.dto;

import com.bank.feature.storage.persistence.FileMeta;

import java.time.Instant;
import java.util.UUID;

public record FileMetaView(UUID id, String fileName, String contentType,
                            Long sizeBytes, String status, Instant createdAt) {
    public static FileMetaView of(FileMeta f) {
        return new FileMetaView(f.getId(), f.getFileName(), f.getContentType(),
                f.getSizeBytes(), f.getStatus(), f.getCreatedAt());
    }
}
