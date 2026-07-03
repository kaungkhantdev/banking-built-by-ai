package com.bank.feature.auth.domain;

public interface PasswordService {

    void changePassword(String userId, String currentPassword, String newPassword);

    void initiateReset(String email);

    void confirmReset(String token, String newPassword);
}
