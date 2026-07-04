package com.bank.feature.mfa.domain;

import com.bank.feature.mfa.web.dto.MfaEnrollView;
import com.bank.feature.mfa.web.dto.RecoveryCodesView;

public interface MfaService {

    MfaEnrollView enroll(String userId);

    void confirmEnrollment(String userId, String totpCode);

    boolean verify(String userId, String totpCode);

    /**
     * Step-up gate for sensitive operations (FR-15.4): if the user has MFA
     * enrolled, a valid TOTP {@code code} is required or the call fails; if MFA is
     * not enrolled this is a no-op.
     */
    void assertStepUp(String userId, String totpCode);

    RecoveryCodesView getRecoveryCodes(String userId);

    RecoveryCodesView regenerateRecoveryCodes(String userId);

    void disable(String userId);
}
