package com.bank.feature.auth.web;

import com.bank.feature.auth.domain.PasswordService;
import com.bank.shared.utils.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Password", description = "Self-service password management")
@RestController
@RequestMapping("/v1/auth/password")
public class PasswordController {

    public record ChangePasswordRequest(@NotBlank String currentPassword, @NotBlank String newPassword) {}
    public record InitResetRequest(@NotBlank String email) {}
    public record ConfirmResetRequest(@NotBlank String token, @NotBlank String newPassword) {}

    private final PasswordService passwords;
    private final CurrentUser currentUser;

    public PasswordController(PasswordService passwords, CurrentUser currentUser) {
        this.passwords = passwords;
        this.currentUser = currentUser;
    }

    @Operation(summary = "Change own password")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/change")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void change(@RequestBody ChangePasswordRequest req) {
        passwords.changePassword(currentUser.name(), req.currentPassword(), req.newPassword());
    }

    @Operation(summary = "Request a password reset email")
    @PostMapping("/reset/init")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void initReset(@RequestBody InitResetRequest req) {
        passwords.initiateReset(req.email());
    }

    @Operation(summary = "Apply a password reset using the one-time token")
    @PostMapping("/reset/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirmReset(@RequestBody ConfirmResetRequest req) {
        passwords.confirmReset(req.token(), req.newPassword());
    }
}
