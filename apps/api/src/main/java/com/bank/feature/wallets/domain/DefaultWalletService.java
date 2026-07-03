package com.bank.feature.wallets.domain;

import com.bank.feature.ledger.domain.LedgerService;
import com.bank.feature.wallets.persistence.Wallet;
import com.bank.feature.wallets.persistence.WalletRepository;
import com.bank.feature.wallets.persistence.WalletStatus;
import com.bank.feature.wallets.web.dto.WalletView;
import com.bank.shared.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class DefaultWalletService implements WalletService {

    private final WalletRepository wallets;
    private final LedgerService ledger;

    public DefaultWalletService(WalletRepository wallets, LedgerService ledger) {
        this.wallets = wallets;
        this.ledger = ledger;
    }

    @Override
    @Transactional
    public WalletView open(UUID accountId, String currency) {
        return WalletView.of(wallets.save(new Wallet(accountId, currency.toUpperCase())));
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal balance(UUID walletId) {
        require(walletId);
        return ledger.balanceOf(walletId);   // derived from immutable ledger facts
    }

    @Override
    @Transactional
    public WalletView freeze(UUID walletId) {
        Wallet w = require(walletId);
        w.setStatus(WalletStatus.FROZEN);
        return WalletView.of(w);
    }

    @Override
    @Transactional
    public WalletView unfreeze(UUID walletId) {
        Wallet w = require(walletId);
        if (w.getStatus() == WalletStatus.CLOSED) {
            throw new ApiException("WALLET_CLOSED", "Closed wallet cannot be reactivated", 409);
        }
        w.setStatus(WalletStatus.ACTIVE);
        return WalletView.of(w);
    }

    @Override
    @Transactional(readOnly = true)
    public Wallet getActive(UUID walletId) {
        Wallet w = require(walletId);
        if (w.getStatus() != WalletStatus.ACTIVE) {
            throw new ApiException("WALLET_NOT_ACTIVE",
                    "Wallet is " + w.getStatus(), 422);
        }
        return w;
    }

    private Wallet require(UUID id) {
        return wallets.findById(id)
                .orElseThrow(() -> new ApiException("WALLET_NOT_FOUND", "Unknown wallet", 404));
    }
}
