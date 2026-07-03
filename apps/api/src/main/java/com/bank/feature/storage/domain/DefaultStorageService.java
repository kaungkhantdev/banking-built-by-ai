package com.bank.feature.storage.domain;

import com.bank.feature.storage.persistence.FileMeta;
import com.bank.feature.storage.persistence.FileMetaRepository;
import com.bank.feature.storage.web.dto.FileMetaView;
import com.bank.shared.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class DefaultStorageService implements StorageService {

    private final FileMetaRepository files;

    public DefaultStorageService(FileMetaRepository files) {
        this.files = files;
    }

    @Override
    @Transactional
    public UploadUrlResponse getUploadUrl(UUID ownerUserId, String fileName, String contentType) {
        String storageKey = "uploads/" + UUID.randomUUID() + "/" + fileName;
        FileMeta meta = files.save(new FileMeta(ownerUserId, fileName, contentType, null, storageKey));
        // Stub: real implementation would call S3/GCS presign API
        String uploadUrl = "/v1/files/" + meta.getId() + "/content";
        return new UploadUrlResponse(meta.getId(), uploadUrl);
    }

    @Override
    @Transactional
    public FileMetaView getDownloadUrl(UUID fileId, UUID ownerUserId) {
        FileMeta f = requireReady(fileId);
        // Stub: mark as READY immediately if PENDING
        if ("PENDING".equals(f.getStatus())) f.setStatus("READY");
        return FileMetaView.of(f);
    }

    @Override
    @Transactional
    public void delete(UUID fileId) {
        FileMeta f = files.findById(fileId)
                .orElseThrow(() -> new ApiException("FILE_NOT_FOUND", "File not found", 404));
        f.setDeleted(true);
        f.setStatus("DELETED");
    }

    private FileMeta requireReady(UUID fileId) {
        FileMeta f = files.findById(fileId)
                .orElseThrow(() -> new ApiException("FILE_NOT_FOUND", "File not found", 404));
        if (f.isDeleted()) throw new ApiException("FILE_DELETED", "File has been deleted", 404);
        return f;
    }
}
