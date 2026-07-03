# Feature 27 — File Storage

**Package:** `com.bank.feature.storage` · **Endpoints:** `/v1/files/*`

## Purpose
Provide secure, encrypted object storage for KYC documents and generated files
(statements, reports) with virus scanning and auto-expiring upload URLs.

## Layout
```
storage/
├── web/        FileController, dto/ (UploadUrlRequest, UploadUrlView, FileMetaView)
├── domain/     StorageService + DefaultStorageService, VirusScanner
└── persistence/ FileMeta, FileMetaRepository
```

## Endpoints
| Method | Path | Permission | Purpose |
|--------|------|-----------|---------|
| POST | `/v1/files/upload-url` | authenticated | Get a pre-signed upload URL |
| GET | `/v1/files/{id}` | authenticated | Get a pre-signed download URL |
| DELETE | `/v1/files/{id}` | `file:manage` | Delete a file record |

## How it works
1. The client requests a pre-signed upload URL (short TTL, e.g., 15 min) and uploads
   the file directly to object storage without routing through the API server.
2. After upload, the client notifies the API; a `VirusScanner` runs asynchronously.
3. On a clean scan result, `FileMeta` is marked `READY`; on a positive result it is
   marked `QUARANTINED` and the file is moved to an isolated bucket.
4. Download URLs are pre-signed and expire automatically (e.g., 5 min).

## Key rules
- Files are encrypted at rest using server-side encryption on the object storage layer.
- Upload URLs expire before the scan is complete; the API rejects access to files
  that are not yet `READY`.
- `FileMeta` records are soft-deleted to preserve references from KYC cases and
  statement records.
- Pre-signed URLs are scoped to the requesting user's identity to prevent enumeration.

## Why
Pre-signed URLs keep large file payloads off the API server, reducing memory pressure
and eliminating a potential DoS surface.

## Related requirements
FR-27.*, FR-6.2
