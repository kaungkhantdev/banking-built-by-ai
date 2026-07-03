package com.bank.feature.statements.domain;

import com.bank.feature.statements.web.dto.StatementView;

import java.util.List;
import java.util.UUID;

public interface StatementService {

    StatementView request(UUID accountId, int year, int month);

    List<StatementView> list(UUID accountId);

    byte[] download(UUID accountId, UUID statementId);
}
