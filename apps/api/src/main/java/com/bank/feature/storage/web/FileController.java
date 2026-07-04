package com.bank.feature.storage.web;

import com.bank.feature.storage.domain.StorageService;
import com.bank.feature.storage.web.dto.FileMetaView;
import com.bank.shared.utils.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Files", description = "Secure file upload and download")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/v1/files")
public class FileController {

    public record UploadUrlRequest(@NotBlank String fileName, @NotBlank String contentType) {}

    private final StorageService storage;
    private final CurrentUser currentUser;

    public FileController(StorageService storage, CurrentUser currentUser) {
        this.storage = storage;
        this.currentUser = currentUser;
    }

    @Operation(summary = "Get a pre-signed upload URL")
    @PostMapping("/upload-url")
    public StorageService.UploadUrlResponse uploadUrl(@RequestBody UploadUrlRequest req) {
        return storage.getUploadUrl(currentUser.id().orElseThrow(), req.fileName(), req.contentType());
    }

    @Operation(summary = "Upload file content (virus-scanned and encrypted at rest)")
    @PostMapping(value = "/{id}/content", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public FileMetaView uploadContent(@PathVariable UUID id, @RequestBody byte[] content) {
        return storage.uploadContent(id, currentUser.id().orElseThrow(), content);
    }

    @Operation(summary = "Download decrypted file content")
    @GetMapping("/{id}/content")
    public ResponseEntity<byte[]> downloadContent(@PathVariable UUID id) {
        byte[] bytes = storage.getContent(id, currentUser.id().orElseThrow());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes);
    }

    @Operation(summary = "Get file metadata / pre-signed download URL")
    @GetMapping("/{id}")
    public FileMetaView download(@PathVariable UUID id) {
        return storage.getDownloadUrl(id, currentUser.id().orElseThrow());
    }

    @Operation(summary = "Delete a file record")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('file:manage')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        storage.delete(id);
    }
}
