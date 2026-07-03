package com.bank.feature.auth.domain;

/**
 * Issued credential pair. The access token is a short-lived RS256 JWT; the
 * refresh token is an opaque high-entropy secret (the client stores it; the
 * server stores only its hash).
 */
public record TokenPair(String accessToken, String refreshToken) {
}
