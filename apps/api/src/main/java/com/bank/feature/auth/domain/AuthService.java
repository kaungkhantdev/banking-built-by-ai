package com.bank.feature.auth.domain;

import com.bank.feature.auth.web.dto.LoginRequest;
import com.bank.feature.auth.web.dto.RegisterRequest;
import com.bank.feature.auth.web.dto.UserView;

/** Port: registration, login, and refresh-token rotation. */
public interface AuthService {

    UserView register(RegisterRequest request);

    TokenPair login(String email, String rawPassword);

    /** Rotate a refresh token; reuse of a rotated token revokes the family. */
    TokenPair refresh(String presentedRefreshToken);
}
