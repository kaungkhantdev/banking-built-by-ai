package com.bank.feature.storage.domain;

/**
 * Seam for malware scanning of uploaded bytes (production: ClamAV/vendor;
 * default: {@link StubVirusScanner}). Mirrors the {@code KycVendorClient} pattern.
 */
public interface VirusScanner {

    /** True if the content is clean; false if malware was detected. */
    boolean isClean(byte[] content);
}
