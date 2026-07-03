package com.bank.feature.auth.web;

import com.bank.feature.auth.domain.AuthService;
import com.bank.feature.auth.domain.TokenPair;
import com.bank.feature.auth.web.dto.LoginRequest;
import com.bank.feature.auth.web.dto.RefreshRequest;
import com.bank.feature.auth.web.dto.RegisterRequest;
import com.bank.feature.auth.web.dto.UserView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "Register, login, and refresh tokens (public endpoints)")
@RestController
@RequestMapping("/v1/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @Operation(summary = "Register a new user")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserView register(@Valid @RequestBody RegisterRequest req) {
        return auth.register(req);
    }

    @Operation(summary = "Login and receive access + refresh tokens")
    @PostMapping("/login")
    public TokenPair login(@Valid @RequestBody LoginRequest req) {
        return auth.login(req.email(), req.password());
    }

    @Operation(summary = "Exchange a refresh token for a new token pair")
    @PostMapping("/refresh")
    public TokenPair refresh(@Valid @RequestBody RefreshRequest req) {
        return auth.refresh(req.refreshToken());
    }
}
