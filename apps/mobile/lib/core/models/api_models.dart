// ── Auth ──────────────────────────────────────────────────────────────────────

class TokenPair {
  final String accessToken;
  final String refreshToken;
  const TokenPair({required this.accessToken, required this.refreshToken});
  factory TokenPair.fromJson(Map<String, dynamic> j) => TokenPair(
        accessToken: j['accessToken'] as String,
        refreshToken: j['refreshToken'] as String,
      );
}

class UserView {
  final String id;
  final String email;
  final bool enabled;
  const UserView({required this.id, required this.email, required this.enabled});
  factory UserView.fromJson(Map<String, dynamic> j) => UserView(
        id: j['id'] as String,
        email: j['email'] as String,
        enabled: j['enabled'] as bool,
      );
}

// ── Accounts ──────────────────────────────────────────────────────────────────

class AccountListItem {
  final String id;
  final String ownerUserId;
  final String ownerEmail;
  final String status;
  final String createdAt;
  const AccountListItem({
    required this.id,
    required this.ownerUserId,
    required this.ownerEmail,
    required this.status,
    required this.createdAt,
  });
  factory AccountListItem.fromJson(Map<String, dynamic> j) => AccountListItem(
        id: j['id'] as String,
        ownerUserId: j['ownerUserId'] as String,
        ownerEmail: j['ownerEmail'] as String,
        status: j['status'] as String,
        createdAt: j['createdAt'] as String,
      );
}

class AccountView {
  final String id;
  final String ownerUserId;
  final String status;
  const AccountView(
      {required this.id, required this.ownerUserId, required this.status});
  factory AccountView.fromJson(Map<String, dynamic> j) => AccountView(
        id: j['id'] as String,
        ownerUserId: j['ownerUserId'] as String,
        status: j['status'] as String,
      );
}

// ── Wallets ───────────────────────────────────────────────────────────────────

class WalletView {
  final String id;
  final String accountId;
  final String currency;
  final String status;
  const WalletView(
      {required this.id,
      required this.accountId,
      required this.currency,
      required this.status});
  factory WalletView.fromJson(Map<String, dynamic> j) => WalletView(
        id: j['id'] as String,
        accountId: j['accountId'] as String,
        currency: j['currency'] as String,
        status: j['status'] as String,
      );
}

class BalanceView {
  final String walletId;
  final double balance;
  const BalanceView({required this.walletId, required this.balance});
  factory BalanceView.fromJson(Map<String, dynamic> j) => BalanceView(
        walletId: j['walletId'].toString(),
        balance: (j['balance'] as num).toDouble(),
      );
}

// ── Transfers ─────────────────────────────────────────────────────────────────

class TransferResult {
  final String transactionId;
  final String status;
  final bool replayed;
  const TransferResult(
      {required this.transactionId,
      required this.status,
      required this.replayed});
  factory TransferResult.fromJson(Map<String, dynamic> j) => TransferResult(
        transactionId: j['transactionId'] as String,
        status: j['status'] as String,
        replayed: j['replayed'] as bool,
      );
}

// ── KYC ───────────────────────────────────────────────────────────────────────

class KycCaseView {
  final String id;
  final String accountId;
  final String status;
  final String? vendorRef;
  final String? rejectReason;
  final String updatedAt;
  const KycCaseView({
    required this.id,
    required this.accountId,
    required this.status,
    this.vendorRef,
    this.rejectReason,
    required this.updatedAt,
  });
  factory KycCaseView.fromJson(Map<String, dynamic> j) => KycCaseView(
        id: j['id'] as String,
        accountId: j['accountId'] as String,
        status: j['status'] as String,
        vendorRef: j['vendorRef'] as String?,
        rejectReason: j['rejectReason'] as String?,
        updatedAt: j['updatedAt'] as String,
      );
}

class KycStatusView {
  final String accountId;
  final String status;
  const KycStatusView({required this.accountId, required this.status});
  factory KycStatusView.fromJson(Map<String, dynamic> j) => KycStatusView(
        accountId: j['accountId'] as String,
        status: j['status'] as String,
      );
}

// ── Transaction history ───────────────────────────────────────────────────────

class TransactionView {
  final String id;
  final String transactionId;
  final String walletId;
  final String direction; // DEBIT | CREDIT
  final double amount;
  final String currency;
  final String? memo;
  final String postedAt;
  const TransactionView({
    required this.id,
    required this.transactionId,
    required this.walletId,
    required this.direction,
    required this.amount,
    required this.currency,
    this.memo,
    required this.postedAt,
  });
  factory TransactionView.fromJson(Map<String, dynamic> j) => TransactionView(
        id: j['id'] as String,
        transactionId: j['transactionId'] as String,
        walletId: j['walletId'] as String,
        direction: j['direction'] as String,
        amount: (j['amount'] as num).toDouble(),
        currency: j['currency'] as String,
        memo: j['memo'] as String?,
        postedAt: j['postedAt'] as String,
      );
}

// ── Beneficiaries ─────────────────────────────────────────────────────────────

class BeneficiaryView {
  final String id;
  final String alias;
  final String destinationWalletId;
  final String createdAt;
  const BeneficiaryView({
    required this.id,
    required this.alias,
    required this.destinationWalletId,
    required this.createdAt,
  });
  factory BeneficiaryView.fromJson(Map<String, dynamic> j) => BeneficiaryView(
        id: j['id'] as String,
        alias: j['alias'] as String,
        destinationWalletId: j['destinationWalletId'] as String,
        createdAt: j['createdAt'] as String,
      );
}

// ── Statements ────────────────────────────────────────────────────────────────

class StatementView {
  final String id;
  final String accountId;
  final int periodYear;
  final int periodMonth;
  final String status;
  final String createdAt;
  const StatementView({
    required this.id,
    required this.accountId,
    required this.periodYear,
    required this.periodMonth,
    required this.status,
    required this.createdAt,
  });
  factory StatementView.fromJson(Map<String, dynamic> j) => StatementView(
        id: j['id'] as String,
        accountId: j['accountId'] as String,
        periodYear: j['periodYear'] as int,
        periodMonth: j['periodMonth'] as int,
        status: j['status'] as String,
        createdAt: j['createdAt'] as String,
      );
}

// ── Exchange ──────────────────────────────────────────────────────────────────

class ExchangeRateView {
  final String fromCurrency;
  final String toCurrency;
  final double rate;
  final String effectiveAt;
  const ExchangeRateView({
    required this.fromCurrency,
    required this.toCurrency,
    required this.rate,
    required this.effectiveAt,
  });
  factory ExchangeRateView.fromJson(Map<String, dynamic> j) => ExchangeRateView(
        fromCurrency: j['fromCurrency'] as String,
        toCurrency: j['toCurrency'] as String,
        rate: (j['rate'] as num).toDouble(),
        effectiveAt: j['effectiveAt'] as String,
      );
}

// ── Scheduled transfers ───────────────────────────────────────────────────────

class ScheduledTransferView {
  final String id;
  final String fromWalletId;
  final String toWalletId;
  final double amount;
  final String? memo;
  final String? recurrenceRule;
  final String? nextRunAt;
  final String status;
  const ScheduledTransferView({
    required this.id,
    required this.fromWalletId,
    required this.toWalletId,
    required this.amount,
    this.memo,
    this.recurrenceRule,
    this.nextRunAt,
    required this.status,
  });
  factory ScheduledTransferView.fromJson(Map<String, dynamic> j) =>
      ScheduledTransferView(
        id: j['id'] as String,
        fromWalletId: j['fromWalletId'] as String,
        toWalletId: j['toWalletId'] as String,
        amount: (j['amount'] as num).toDouble(),
        memo: j['memo'] as String?,
        recurrenceRule: j['recurrenceRule'] as String?,
        nextRunAt: j['nextRunAt'] as String?,
        status: j['status'] as String,
      );
}

// ── Pagination ────────────────────────────────────────────────────────────────

class ApiPage<T> {
  final List<T> content;
  final int totalElements;
  final int totalPages;
  final int number;
  final int size;
  const ApiPage({
    required this.content,
    required this.totalElements,
    required this.totalPages,
    required this.number,
    required this.size,
  });
}
