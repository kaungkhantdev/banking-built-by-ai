package com.bank.feature.storage.domain;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Default scanner for local/dev. Detects the standard EICAR antivirus test
 * signature so the pipeline can be exercised end-to-end without real malware.
 * Activate the {@code realav} profile to plug in a real scanner.
 */
@Component
@Profile("!realav")
public class StubVirusScanner implements VirusScanner {

    private static final String EICAR =
            "X5O!P%@AP[4\\PZX54(P^)7CC)7}$EICAR";

    @Override
    public boolean isClean(byte[] content) {
        if (content == null) return true;
        String head = new String(content, 0, Math.min(content.length, 128), StandardCharsets.US_ASCII);
        return !head.contains(EICAR);
    }
}
