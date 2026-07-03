package com.bank.feature.storage.web;

import com.bank.feature.storage.domain.StorageService;
import com.bank.feature.storage.web.dto.FileMetaView;
import com.bank.shared.utils.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
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

    @Operation(summary = "Get a pre-signed download URL for a file")
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
