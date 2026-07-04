import 'dart:io';
import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../config/app_config.dart';
import '../models/api_models.dart';
import '../storage/token_storage.dart';
import '../../features/auth/auth_provider.dart';

// ── Dio provider ──────────────────────────────────────────────────────────────

final dioProvider = Provider<Dio>((ref) {
  final dio = Dio(BaseOptions(
    baseUrl: AppConfig.apiBaseUrl,
    connectTimeout: const Duration(milliseconds: AppConfig.connectTimeoutMs),
    receiveTimeout: const Duration(milliseconds: AppConfig.receiveTimeoutMs),
    headers: {'Content-Type': 'application/json'},
  ));

  dio.interceptors.add(_AuthInterceptor(ref));
  return dio;
});

class _AuthInterceptor extends Interceptor {
  final Ref _ref;
  _AuthInterceptor(this._ref);

  @override
  void onRequest(RequestOptions options, RequestInterceptorHandler handler) {
    final token = _ref.read(authProvider).accessToken;
    if (token != null) {
      options.headers['Authorization'] = 'Bearer $token';
    }
    handler.next(options);
  }

  @override
  void onError(DioException err, ErrorInterceptorHandler handler) {
    if (err.response?.statusCode == 401) {
      _ref.read(authProvider.notifier).logout();
    }
    handler.next(err);
  }
}

// ── API client ────────────────────────────────────────────────────────────────

final apiClientProvider = Provider<ApiClient>((ref) {
  return ApiClient(ref.watch(dioProvider));
});

class ApiClient {
  final Dio _dio;
  const ApiClient(this._dio);

  // ── Auth ────────────────────────────────────────────────────────────────────

  Future<TokenPair> login(String email, String password) async {
    final res = await _dio.post<Map<String, dynamic>>(
        '/v1/auth/login', data: {'email': email, 'password': password});
    return TokenPair.fromJson(res.data!);
  }

  Future<UserView> register(String email, String password) async {
    final res = await _dio.post<Map<String, dynamic>>(
        '/v1/auth/register', data: {'email': email, 'password': password});
    return UserView.fromJson(res.data!);
  }

  // ── Accounts ────────────────────────────────────────────────────────────────

  /// The signed-in user's own accounts (customer-scoped; requires wallet:read).
  Future<List<AccountListItem>> listMyAccounts() async {
    final res = await _dio.get<List<dynamic>>('/v1/accounts/me');
    return (res.data ?? [])
        .map((e) => AccountListItem.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  /// Open a new account for the signed-in user (self-service; no UUID needed).
  Future<AccountView> openMyAccount() async {
    final res = await _dio.post<Map<String, dynamic>>('/v1/accounts/me');
    return AccountView.fromJson(res.data!);
  }

  /// Full detail for one of the user's accounts: holder/KYC, wallets, recent txns.
  Future<AccountOverview> accountOverview(String accountId) async {
    final res =
        await _dio.get<Map<String, dynamic>>('/v1/accounts/$accountId/overview');
    return AccountOverview.fromJson(res.data!);
  }

  Future<AccountView> activateAccount(String id) async {
    final res = await _dio.post<Map<String, dynamic>>('/v1/accounts/$id/activate');
    return AccountView.fromJson(res.data!);
  }

  // ── Wallets ─────────────────────────────────────────────────────────────────

  /// The signed-in customer's own wallets (across their accounts), with balances.
  Future<List<MyWalletView>> listMyWallets() async {
    final res = await _dio.get<List<dynamic>>('/v1/wallets/me');
    return (res.data ?? [])
        .map((e) => MyWalletView.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<WalletView> openWallet(String accountId, String currency) async {
    final res = await _dio.post<Map<String, dynamic>>(
        '/v1/wallets', data: {'accountId': accountId, 'currency': currency});
    return WalletView.fromJson(res.data!);
  }

  Future<BalanceView> walletBalance(String id) async {
    final res = await _dio.get<Map<String, dynamic>>('/v1/wallets/$id/balance');
    return BalanceView.fromJson(res.data!);
  }

  Future<WalletView> freezeWallet(String id) async {
    final res = await _dio.post<Map<String, dynamic>>('/v1/wallets/$id/freeze');
    return WalletView.fromJson(res.data!);
  }

  // ── Transfers ───────────────────────────────────────────────────────────────

  /// Destination is either a raw wallet id or a saved beneficiary — pass exactly one.
  Future<TransferResult> transfer({
    required String fromWalletId,
    String? toWalletId,
    String? beneficiaryId,
    required double amount,
    String? memo,
  }) async {
    final key = _uuid();
    final res = await _dio.post<Map<String, dynamic>>(
      '/v1/transfers',
      data: {
        'fromWalletId': fromWalletId,
        if (toWalletId != null) 'toWalletId': toWalletId,
        if (beneficiaryId != null) 'beneficiaryId': beneficiaryId,
        'amount': amount,
        if (memo != null) 'memo': memo,
      },
      options: Options(headers: {'Idempotency-Key': key}),
    );
    return TransferResult.fromJson(res.data!);
  }

  // ── KYC ─────────────────────────────────────────────────────────────────────

  Future<ApiPage<KycCaseView>> listKyc({String? status, int page = 0, int size = 20}) async {
    final res = await _dio.get<Map<String, dynamic>>('/v1/kyc', queryParameters: {
      'page': page,
      'size': size,
      if (status != null) 'status': status,
    });
    final d = res.data!;
    return ApiPage(
      content: (d['content'] as List)
          .map((e) => KycCaseView.fromJson(e as Map<String, dynamic>))
          .toList(),
      totalElements: d['totalElements'] as int,
      totalPages: d['totalPages'] as int,
      number: d['number'] as int,
      size: d['size'] as int,
    );
  }

  Future<void> openKycCase(String accountId) async {
    await _dio.post<dynamic>('/v1/kyc', data: {'accountId': accountId});
  }

  Future<void> submitKycDocument(String accountId, File file) async {
    final form = FormData.fromMap({
      'file': await MultipartFile.fromFile(file.path,
          filename: file.path.split('/').last),
    });
    await _dio.post<dynamic>('/v1/kyc/$accountId/documents', data: form);
  }

  Future<KycStatusView> kycStatus(String accountId) async {
    final res =
        await _dio.get<Map<String, dynamic>>('/v1/kyc/$accountId/status');
    return KycStatusView.fromJson(res.data!);
  }

  // ── Transaction history ──────────────────────────────────────────────────────

  Future<ApiPage<TransactionView>> listTransactions(String walletId,
      {int page = 0, int size = 20, String? direction}) async {
    final res = await _dio.get<Map<String, dynamic>>(
        '/v1/wallets/$walletId/transactions',
        queryParameters: {
          'page': page,
          'size': size,
          if (direction != null) 'direction': direction,
        });
    final d = res.data!;
    return ApiPage(
      content: (d['content'] as List)
          .map((e) => TransactionView.fromJson(e as Map<String, dynamic>))
          .toList(),
      totalElements: d['totalElements'] as int,
      totalPages: d['totalPages'] as int,
      number: d['number'] as int,
      size: d['size'] as int,
    );
  }

  // ── Beneficiaries ────────────────────────────────────────────────────────────

  Future<ApiPage<BeneficiaryView>> listBeneficiaries({int page = 0}) async {
    final res = await _dio.get<Map<String, dynamic>>('/v1/beneficiaries',
        queryParameters: {'page': page, 'size': 50});
    final d = res.data!;
    return ApiPage(
      content: (d['content'] as List)
          .map((e) => BeneficiaryView.fromJson(e as Map<String, dynamic>))
          .toList(),
      totalElements: d['totalElements'] as int,
      totalPages: d['totalPages'] as int,
      number: d['number'] as int,
      size: d['size'] as int,
    );
  }

  Future<BeneficiaryView> addBeneficiary(String alias, String destinationWalletId) async {
    final res = await _dio.post<Map<String, dynamic>>('/v1/beneficiaries',
        data: {'alias': alias, 'destinationWalletId': destinationWalletId});
    return BeneficiaryView.fromJson(res.data!);
  }

  Future<void> deleteBeneficiary(String id) async {
    await _dio.delete<void>('/v1/beneficiaries/$id');
  }

  // ── Password ─────────────────────────────────────────────────────────────────

  Future<void> changePassword(String currentPassword, String newPassword) async {
    await _dio.post<void>('/v1/auth/password/change',
        data: {'currentPassword': currentPassword, 'newPassword': newPassword});
  }

  // ── Statements ───────────────────────────────────────────────────────────────

  Future<List<StatementView>> listStatements(String accountId) async {
    final res = await _dio.get<List<dynamic>>('/v1/accounts/$accountId/statements');
    return res.data!
        .map((e) => StatementView.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<StatementView> requestStatement(String accountId, int year, int month) async {
    final res = await _dio.post<Map<String, dynamic>>(
        '/v1/accounts/$accountId/statements',
        data: {'year': year, 'month': month});
    return StatementView.fromJson(res.data!);
  }

  // ── Exchange ─────────────────────────────────────────────────────────────────

  Future<List<String>> supportedCurrencies() async {
    final res = await _dio.get<List<dynamic>>('/v1/exchange/currencies');
    return res.data!.cast<String>();
  }

  Future<ExchangeRateView> exchangeRate(String from, String to) async {
    final res = await _dio.get<Map<String, dynamic>>('/v1/exchange/rates',
        queryParameters: {'from': from, 'to': to});
    return ExchangeRateView.fromJson(res.data!);
  }

  // ── Scheduled transfers ──────────────────────────────────────────────────────

  Future<ApiPage<ScheduledTransferView>> listScheduled({int page = 0}) async {
    final res = await _dio.get<Map<String, dynamic>>('/v1/scheduled-transfers',
        queryParameters: {'page': page, 'size': 20});
    final d = res.data!;
    return ApiPage(
      content: (d['content'] as List)
          .map((e) => ScheduledTransferView.fromJson(e as Map<String, dynamic>))
          .toList(),
      totalElements: d['totalElements'] as int,
      totalPages: d['totalPages'] as int,
      number: d['number'] as int,
      size: d['size'] as int,
    );
  }

  Future<ScheduledTransferView> scheduleTransfer({
    required String fromWalletId,
    required String toWalletId,
    required double amount,
    String? memo,
    String? recurrenceRule,
    required DateTime runAt,
  }) async {
    final res = await _dio.post<Map<String, dynamic>>('/v1/scheduled-transfers', data: {
      'fromWalletId': fromWalletId,
      'toWalletId': toWalletId,
      'amount': amount,
      if (memo != null) 'memo': memo,
      if (recurrenceRule != null) 'recurrenceRule': recurrenceRule,
      'runAt': runAt.toUtc().toIso8601String(),
    });
    return ScheduledTransferView.fromJson(res.data!);
  }

  Future<void> cancelScheduled(String id) async {
    await _dio.delete<void>('/v1/scheduled-transfers/$id');
  }

  // ── Helpers ──────────────────────────────────────────────────────────────────

  static String _uuid() {
    // Simple RFC-4122 v4 UUID without a package dependency.
    const chars = '0123456789abcdef';
    final buf = StringBuffer();
    for (var i = 0; i < 32; i++) {
      if (i == 8 || i == 12 || i == 16 || i == 20) buf.write('-');
      if (i == 12) { buf.write('4'); continue; }
      if (i == 16) {
        buf.write(chars[(8 + (DateTime.now().microsecondsSinceEpoch % 4)).toInt()]);
        continue;
      }
      buf.write(chars[(DateTime.now().microsecondsSinceEpoch + i) % 16]);
    }
    return buf.toString();
  }
}
