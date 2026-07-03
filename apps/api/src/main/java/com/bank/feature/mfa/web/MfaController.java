package com.bank.feature.mfa.web;

import com.bank.feature.mfa.domain.MfaService;
import com.bank.feature.mfa.web.dto.MfaEnrollView;
import com.bank.feature.mfa.web.dto.RecoveryCodesView;
import com.bank.shared.utils.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "MFA", description = "TOTP multi-factor authentication")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/v1/mfa")
public class MfaController {

    public record MfaVerifyRequest(@NotBlank String code) {}

    private final MfaService mfa;
    private final CurrentUser currentUser;

    public MfaController(MfaService mfa, CurrentUser currentUser) {
        this.mfa = mfa;
        this.currentUser = currentUser;
    }

    @Operation(summary = "Begin TOTP enrolment")
    @PostMapping("/enroll")
    public MfaEnrollView enroll() {
        return mfa.enroll(currentUser.name());
    }

    @Operation(summary = "Confirm enrolment with first TOTP code")
    @PostMapping("/enroll/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirmEnroll(@RequestBody MfaVerifyRequest req) {
        mfa.confirmEnrollment(currentUser.name(), req.code());
    }

    @Operation(summary = "Verify a TOTP code (step-up auth)")
    @PostMapping("/verify")
    public boolean verify(@RequestBody MfaVerifyRequest req) {
        return mfa.verify(currentUser.name(), req.code());
    }

    @Operation(summary = "View remaining recovery codes count")
    @GetMapping("/recovery-codes")
    public RecoveryCodesView recoveryCodes() {
        return mfa.getRecoveryCodes(currentUser.name());
    }

    @Operation(summary = "Re-generate recovery codes")
    @PostMapping("/recovery-codes/regenerate")
    public RecoveryCodesView regenerateCodes() {
        return mfa.regenerateRecoveryCodes(currentUser.name());
    }

    @Operation(summary = "Disable MFA")
    @DeleteMapping("/disable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disable() {
        mfa.disable(currentUser.name());
    }
}
