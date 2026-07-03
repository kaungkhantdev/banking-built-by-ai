package com.bank.feature.mfa.web.dto;

public record MfaEnrollView(String secret, String qrCodeUri, String issuer) {}
