package com.bank.feature.mfa.web.dto;

import java.util.List;

public record RecoveryCodesView(List<String> codes, long remaining) {}
