package com.bank.feature.health.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin", description = "Operator-only user and role management")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/v1/admin")
public class AdminStatsController {

    public record PlatformStats(
            long totalUsers,
            long totalAccounts,
            long totalWallets,
            long pendingKycCount,
            long totalTransactions) {
    }

    private final JdbcTemplate jdbc;

    public AdminStatsController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Operation(summary = "Platform-wide KPI counts")
    @GetMapping("/stats")
    public PlatformStats stats() {
        return jdbc.queryForObject("""
                SELECT
                    (SELECT COUNT(*) FROM users)                                                     AS total_users,
                    (SELECT COUNT(*) FROM accounts)                                                  AS total_accounts,
                    (SELECT COUNT(*) FROM wallets)                                                   AS total_wallets,
                    (SELECT COUNT(*) FROM kyc_cases WHERE status NOT IN ('VERIFIED','REJECTED'))     AS pending_kyc,
                    (SELECT COUNT(DISTINCT transaction_id) FROM ledger_entries)                      AS total_transactions
                """,
                (rs, rn) -> new PlatformStats(
                        rs.getLong("total_users"),
                        rs.getLong("total_accounts"),
                        rs.getLong("total_wallets"),
                        rs.getLong("pending_kyc"),
                        rs.getLong("total_transactions")));
    }
}
