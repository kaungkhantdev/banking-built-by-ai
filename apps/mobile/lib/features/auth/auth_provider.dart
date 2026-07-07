import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/config/app_config.dart';
import '../../core/models/api_models.dart';
import '../../core/storage/token_storage.dart';

// ── State ─────────────────────────────────────────────────────────────────────

class AuthState {
  final String? accessToken;
  final String? refreshToken;
  final bool isLoading;
  final String? error;

  const AuthState({
    this.accessToken,
    this.refreshToken,
    this.isLoading = false,
    this.error,
  });

  bool get isAuthenticated => accessToken != null;

  AuthState copyWith({
    String? accessToken,
    String? refreshToken,
    bool? isLoading,
    String? error,
    bool clearTokens = false,
    bool clearError = false,
  }) =>
      AuthState(
        accessToken: clearTokens ? null : (accessToken ?? this.accessToken),
        refreshToken: clearTokens ? null : (refreshToken ?? this.refreshToken),
        isLoading: isLoading ?? this.isLoading,
        error: clearError ? null : (error ?? this.error),
      );
}

// ── Notifier ──────────────────────────────────────────────────────────────────

class AuthNotifier extends Notifier<AuthState> {
  // Bare Dio used only for login/register — avoids circular dependency with
  // the main dioProvider (which needs authProvider to inject the token).
  late final Dio _bare = Dio(BaseOptions(
    baseUrl: AppConfig.apiBaseUrl,
    connectTimeout: const Duration(milliseconds: AppConfig.connectTimeoutMs),
    receiveTimeout: const Duration(milliseconds: AppConfig.receiveTimeoutMs),
  ));

  @override
  AuthState build() => const AuthState();

  /// Restores tokens from secure storage on startup.
  Future<void> init() async {
    final s = ref.read(tokenStorageProvider);
    final access  = await s.getAccess();
    final refresh = await s.getRefresh();
    if (access != null) {
      state = AuthState(accessToken: access, refreshToken: refresh);
    }
  }

  Future<void> login(String email, String password) async {
    state = state.copyWith(isLoading: true, clearError: true);
    try {
      final res = await _bare.post<Map<String, dynamic>>(
        '/v1/auth/login',
        data: {'email': email, 'password': password},
      );
      await _persist(TokenPair.fromJson(res.data!));
    } on DioException catch (e) {
      final msg = _dioMsg(e);
      state = state.copyWith(isLoading: false, error: msg);
      throw Exception(msg);
    }
  }

  Future<void> register(String email, String password) async {
    state = state.copyWith(isLoading: true, clearError: true);
    try {
      await _bare.post<dynamic>(
        '/v1/auth/register',
        data: {'email': email, 'password': password},
      );
      state = state.copyWith(isLoading: false);
    } on DioException catch (e) {
      final msg = _dioMsg(e);
      state = state.copyWith(isLoading: false, error: msg);
      throw Exception(msg);
    }
  }

  void logout() {
    ref.read(tokenStorageProvider).clear();
    state = const AuthState();
  }

  Future<void> _persist(TokenPair pair) async {
    await ref.read(tokenStorageProvider).save(pair.accessToken, pair.refreshToken);
    state = AuthState(
      accessToken: pair.accessToken,
      refreshToken: pair.refreshToken,
    );
  }

  String _dioMsg(DioException e) {
    final status = e.response?.statusCode;

    // Friendly overrides for the common auth cases (clearer than the raw
    // server text, and independent of any wording changes on the backend).
    switch (status) {
      case 401:
        return 'Invalid email or password';
      case 409:
        return 'Email already registered';
      case 423:
        return 'Account locked — too many failed attempts. Try again later.';
      case 429:
        return 'Too many attempts — please wait and try again.';
    }

    // Otherwise prefer the server's {code, message, traceId} envelope so the
    // user sees a meaningful reason instead of a generic network error.
    final data = e.response?.data;
    if (data is Map && data['message'] is String &&
        (data['message'] as String).trim().isNotEmpty) {
      return data['message'] as String;
    }
    if (status == 400) return 'Invalid input';

    // No HTTP response at all → genuinely a connectivity/timeout problem.
    if (e.response == null) return 'Cannot reach the server — check your connection';
    return 'Something went wrong (HTTP $status)';
  }
}

final authProvider = NotifierProvider<AuthNotifier, AuthState>(AuthNotifier.new);
