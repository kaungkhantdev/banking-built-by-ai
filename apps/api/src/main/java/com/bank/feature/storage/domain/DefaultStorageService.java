package com.bank.feature.storage.domain;

import com.bank.feature.storage.persistence.FileMeta;
import com.bank.feature.storage.persistence.FileMetaRepository;
import com.bank.feature.storage.web.dto.FileMetaView;
import com.bank.shared.exception.ApiException;
import com.bank.shared.utils.Aes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
public class DefaultStorageService implements StorageService {

    private final FileMetaRepository files;
    private final Aes aes;
    private final VirusScanner scanner;
    private final Duration uploadTtl;

    public DefaultStorageService(FileMetaRepository files, Aes aes, VirusScanner scanner,
                                 @Value("${storage.upload-url-ttl:PT15M}") Duration uploadTtl) {
        this.files = files;
        this.aes = aes;
        this.scanner = scanner;
        this.uploadTtl = uploadTtl;
    }

    @Override
    @Transactional
    public UploadUrlResponse getUploadUrl(UUID ownerUserId, String fileName, String contentType) {
        String storageKey = "uploads/" + UUID.randomUUID() + "/" + fileName;
        FileMeta meta = files.save(new FileMeta(ownerUserId, fileName, contentType, null, storageKey));
        Instant expiresAt = Instant.now().plus(uploadTtl);
        meta.setUploadExpiresAt(expiresAt);
        return new UploadUrlResponse(meta.getId(), "/v1/files/" + meta.getId() + "/content", expiresAt);
    }

    @Override
    @Transactional
    public FileMetaView uploadContent(UUID fileId, UUID ownerUserId, byte[] content) {
        FileMeta f = requireOwned(fileId, ownerUserId);
        // FR-27.4: reject uploads after the pre-signed URL has expired.
        if (f.getUploadExpiresAt() == null || f.getUploadExpiresAt().isBefore(Instant.now())) {
            throw new ApiException("UPLOAD_URL_EXPIRED", "The upload URL has expired", 410);
        }
        // FR-27.2: scan before persisting anything.
        if (!scanner.isClean(content)) {
            f.setStatus("INFECTED");
            throw new ApiException("FILE_INFECTED", "Uploaded file failed the virus scan", 422);
        }
        // FR-27.3: encrypt the bytes at rest.
        f.setContentEncrypted(aes.encrypt(Base64.getEncoder().encodeToString(content)));
        f.setSizeBytes((long) content.length);
        f.setStatus("READY");
        return FileMetaView.of(f);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] getContent(UUID fileId, UUID ownerUserId) {
        FileMeta f = requireOwned(fileId, ownerUserId);
        if (f.isDeleted()) throw new ApiException("FILE_DELETED", "File has been deleted", 404);
        if (f.getContentEncrypted() == null || !"READY".equals(f.getStatus())) {
            throw new ApiException("FILE_NOT_READY", "File has no stored content", 409);
        }
        return Base64.getDecoder().decode(aes.decrypt(f.getContentEncrypted()));
    }

    @Override
    @Transactional
    public FileMetaView getDownloadUrl(UUID fileId, UUID ownerUserId) {
        FileMeta f = requireOwned(fileId, ownerUserId);
        if (f.isDeleted()) throw new ApiException("FILE_DELETED", "File has been deleted", 404);
        return FileMetaView.of(f);
    }

    @Override
    @Transactional
    public void delete(UUID fileId) {
        FileMeta f = files.findById(fileId)
                .orElseThrow(() -> new ApiException("FILE_NOT_FOUND", "File not found", 404));
        f.setDeleted(true);
        f.setStatus("DELETED");
        f.setContentEncrypted(null);   // drop the ciphertext
    }

    private FileMeta requireOwned(UUID fileId, UUID ownerUserId) {
        FileMeta f = files.findById(fileId)
                .orElseThrow(() -> new ApiException("FILE_NOT_FOUND", "File not found", 404));
        if (!f.getOwnerUserId().equals(ownerUserId)) {
            throw new ApiException("FORBIDDEN", "You do not own this file", 403);
        }
        return f;
    }
}
