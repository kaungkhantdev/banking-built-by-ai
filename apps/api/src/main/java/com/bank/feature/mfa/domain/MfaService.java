package com.bank.feature.mfa.domain;

import com.bank.feature.mfa.web.dto.MfaEnrollView;
import com.bank.feature.mfa.web.dto.RecoveryCodesView;

public interface MfaService {

    MfaEnrollView enroll(String userId);

    void confirmEnrollment(String userId, String totpCode);

    boolean verify(String userId, String totpCode);

    RecoveryCodesView getRecoveryCodes(String userId);

    RecoveryCodesView regenerateRecoveryCodes(String userId);

    void disable(String userId);
}
