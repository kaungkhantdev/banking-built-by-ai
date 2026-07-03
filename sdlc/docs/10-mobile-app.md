# Mobile App — Features & Implementation

> Digital Banking Platform · Customer Mobile Client
> Status: Baseline · Owner: Mobile Lead
> Runtime: Flutter 3 · Dart 3 · iOS 15+ / Android 8+ (API 26+)

---

## Overview

The customer-facing app is a single **Flutter + Dart** codebase that ships to
both iOS and Android via native widgets — no WebView, no JS bridge. It is a
**thin client over the `/v1` REST API** — it holds **no business logic** of its
own. Money correctness, RBAC, the ledger, and KYC decisioning all live in the
backend (see `09-backend-api.md`); the app's job is to authenticate securely,
render state, and submit well-formed, **idempotent** requests.

Three rules drive every decision in this document:

1. **Secrets live in the OS secure enclave, never in Dart heap or local files.**
   The long-lived refresh token is stored in `flutter_secure_storage` (iOS
   Keychain / Android Keystore); the short-lived access token lives only in
   memory.
2. **Every money request is idempotent.** Transfers carry a client-generated
   `Idempotency-Key`, so a retry over a flaky mobile network can never
   double-charge.
3. **The server is the source of truth.** The UI hides actions the user cannot
   take, but it always reflects what the API returns, including the standard
   error envelope `{code, message, trace_id}`.

---

## 1. Project Structure & Architecture

**What.** A **feature-first** layout: each capability (auth, kyc, wallets,
transfers) is a folder owning its screens, providers, repository calls, and
models. Shared plumbing (the HTTP client, secure storage, the design system)
lives under `core/`.

**Why feature-first.** A change to transfers stays inside `features/transfers/`
instead of being scattered across global `screens/`, `services/`, and `models/`
folders. It mirrors the backend's package-by-feature layout.

**How — the shape of the tree.**

```text
lib/
├── main.dart                    # ProviderScope root, GoRouter setup
│
├── core/
│   ├── api/
│   │   ├── api_client.dart      # Dio wrapper: base URL, auth header, refresh, error envelope
│   │   ├── endpoints.dart       # typed /v1 path constants
│   │   └── models/              # DTOs mirrored from the backend (TokenPair, Wallet, …)
│   ├── secure/
│   │   └── token_store.dart     # flutter_secure_storage-backed refresh-token storage
│   ├── security/
│   │   ├── integrity.dart       # jailbreak/root + debugger detection
│   │   └── pinning.dart         # certificate pinning via dio_pinning
│   └── idempotency.dart         # UUID v4 key generation
│
├── features/
│   ├── auth/                    # login, register, silent refresh, biometric unlock
│   ├── kyc/                     # document + selfie capture and upload
│   ├── wallets/                 # wallet list, balances, transaction history
│   ├── transfers/               # amount entry, confirm, idempotent submit
│   └── notifications/           # push registration + event handling
│
└── ui/                          # design system: AppButton, MoneyText, AppScreen, theme
```

**Stack choices.**

| Concern            | Choice                          | Why                                                               |
|--------------------|---------------------------------|-------------------------------------------------------------------|
| State / DI         | Riverpod 2                      | Compile-safe providers, `AsyncNotifier` for async state, no boilerplate. |
| Navigation         | GoRouter                        | Declarative, URL-based routing with redirect guards for auth.     |
| HTTP               | Dio                             | Interceptors for auth + refresh; `CancelToken` support.           |
| Secure storage     | flutter_secure_storage          | Wraps iOS Keychain + Android Keystore/EncryptedSharedPreferences. |
| Biometrics         | local_auth                      | Face ID / fingerprint gate before reading the refresh token.      |
| Push notifications | firebase_messaging              | FCM/APNs token registration + foreground message handling.        |
| Forms              | Built-in Flutter forms + validators | Server re-validates; client validates shape only.             |

---

## 2. Secure Token Storage & Session

**What.** The app receives a `TokenPair` — a short-lived **access JWT** and an
opaque **refresh token** — from `/v1/auth/login`. The access token is held in a
Riverpod provider (in memory); the refresh token is written to
`flutter_secure_storage`.

**Why.** A stolen device backup or a compromised process must not yield a usable
long-lived credential. `flutter_secure_storage` is hardware-backed and survives
app restarts without ever exposing the secret to unprotected storage. The access
token is intentionally ephemeral — losing it on a cold start just triggers a
silent refresh.

**How.**
- On login/refresh, persist only the refresh token in secure storage.
- Hold the access token in an in-memory `StateProvider`.
- On cold start, read the refresh token (biometric-gated), call
  `/v1/auth/refresh` to mint a new access token (silent login), then show home.
- On logout, delete the secure-storage entry and invalidate all providers.

**Code — secure token store.**

```dart
// core/secure/token_store.dart
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

class TokenStore {
  static const _storage = FlutterSecureStorage(
    iOptions: IOSOptions(accessibility: KeychainAccessibility.first_unlock_this_device),
    aOptions: AndroidOptions(encryptedSharedPreferences: true),
  );
  static const _refreshKey = 'com.bank.refresh_token';

  Future<void> saveRefreshToken(String token) =>
      _storage.write(key: _refreshKey, value: token);

  Future<String?> readRefreshToken() => _storage.read(key: _refreshKey);

  Future<void> clear() => _storage.delete(key: _refreshKey);
}
```

**Code — in-memory session (access token never touches disk).**

```dart
// features/auth/session_provider.dart
import 'package:riverpod_annotation/riverpod_annotation.dart';

part 'session_provider.g.dart';

class Session {
  const Session({required this.accessToken, required this.perms});
  final String accessToken;
  final List<String> perms;
}

@riverpod
class SessionNotifier extends _$SessionNotifier {
  @override
  Session? build() => null; // null = unauthenticated

  void setSession(Session session) => state = session;
  void clear() => state = null;
}
```

---

## 3. The API Client — Auth, Silent Refresh, Error Envelope

**What.** A Dio instance with interceptors that (a) attach the `Bearer` access
token, (b) transparently refresh once on a `401`, and (c) decode the backend's
standard error envelope `{code, message, trace_id}` into a typed `ApiException`.

**Why.** Centralizing auth + refresh in one interceptor means no screen ever
deals with token plumbing. A single `_refreshInFlight` lock prevents a
thundering herd of parallel refreshes when several requests `401` at once.

**Code.**

```dart
// core/api/api_client.dart
import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

class ApiException implements Exception {
  const ApiException({
    required this.code,
    required this.message,
    required this.statusCode,
    this.traceId,
  });
  final String code;
  final String message;
  final int statusCode;
  final String? traceId;
}

class AuthInterceptor extends Interceptor {
  AuthInterceptor(this._ref, this._store);

  final Ref _ref;
  final TokenStore _store;
  Future<void>? _refreshInFlight;

  @override
  void onRequest(RequestOptions options, RequestInterceptorHandler handler) {
    final token = _ref.read(sessionNotifierProvider)?.accessToken;
    if (token != null) {
      options.headers['Authorization'] = 'Bearer $token';
    }
    handler.next(options);
  }

  @override
  Future<void> onError(DioException err, ErrorInterceptorHandler handler) async {
    if (err.response?.statusCode != 401) return handler.next(err);

    _refreshInFlight ??= _doRefresh().whenComplete(() => _refreshInFlight = null);
    try {
      await _refreshInFlight;
      // Retry original request once with new token
      final opts = err.requestOptions;
      final token = _ref.read(sessionNotifierProvider)?.accessToken;
      opts.headers['Authorization'] = 'Bearer $token';
      final response = await Dio().fetch(opts);
      return handler.resolve(response);
    } catch (_) {
      _ref.read(sessionNotifierProvider.notifier).clear();
      handler.next(err);
    }
  }

  Future<void> _doRefresh() async {
    final refreshToken = await _store.readRefreshToken();
    if (refreshToken == null) throw ApiException(code: 'AUTH_NO_SESSION', message: 'No session', statusCode: 401);
    // POST /v1/auth/refresh and update session ...
  }
}
```

---

## 4. Login, Registration & Biometric Unlock

**What.** Email/password login against `/v1/auth/login`, registration via
`/v1/auth/register`, and a **biometric (Face ID / fingerprint) unlock** gate on
app open that authorizes reading the refresh token from the enclave.

**Why.** Biometrics bind the stored refresh token to the physical device owner —
even an unlocked phone left on a table cannot silently resume a banking session.

**How.** On launch, attempt a biometric-gated read of the refresh token; success
→ silent refresh → home. Failure or no stored token → the login screen.

**Code — login AsyncNotifier.**

```dart
// features/auth/login_notifier.dart
import 'package:riverpod_annotation/riverpod_annotation.dart';

part 'login_notifier.g.dart';

@riverpod
class LoginNotifier extends _$LoginNotifier {
  @override
  AsyncValue<void> build() => const AsyncData(null);

  Future<void> login(String email, String password) async {
    state = const AsyncLoading();
    state = await AsyncValue.guard(() async {
      final pair = await ref.read(apiClientProvider).post<Map<String, dynamic>>(
        '/auth/login',
        data: {'email': email, 'password': password},
      );
      final tokenPair = TokenPair.fromJson(pair.data!);
      await ref.read(tokenStoreProvider).saveRefreshToken(tokenPair.refreshToken);
      ref.read(sessionNotifierProvider.notifier).setSession(
        Session(
          accessToken: tokenPair.accessToken,
          perms: tokenPair.perms,
        ),
      );
    });
  }
}
```

**Code — biometric-gated silent resume.**

```dart
// features/auth/resume_session.dart
import 'package:local_auth/local_auth.dart';

Future<bool> resumeSession(Ref ref) async {
  final auth = LocalAuthentication();
  final canAuth = await auth.canCheckBiometrics;
  if (canAuth) {
    final passed = await auth.authenticate(
      localizedReason: 'Unlock your banking app',
      options: const AuthenticationOptions(biometricOnly: true),
    );
    if (!passed) return false;
  }

  final refreshToken = await ref.read(tokenStoreProvider).readRefreshToken();
  if (refreshToken == null) return false;

  try {
    final pair = await ref.read(apiClientProvider).post<Map<String, dynamic>>(
      '/auth/refresh',
      data: {'refreshToken': refreshToken},
    );
    final tokenPair = TokenPair.fromJson(pair.data!);
    await ref.read(tokenStoreProvider).saveRefreshToken(tokenPair.refreshToken);
    ref.read(sessionNotifierProvider.notifier).setSession(
      Session(accessToken: tokenPair.accessToken, perms: tokenPair.perms),
    );
    return true;
  } catch (_) {
    return false;
  }
}
```

---

## 5. KYC — Document & Selfie Capture

**What.** A guided flow that captures an identity document and a selfie, uploads
them, and then polls / waits for a push on the KYC decision. Maps to the backend
`/v1/kyc` endpoints and the `KYC_CASES` lifecycle
(`PENDING → VERIFIED | REJECTED`).

**Why.** The app **never decides** KYC — it only collects evidence and reflects
status. Capture quality (glare, framing) is validated on-device to cut
rejections, but the verdict is the backend's (and a compliance officer's) call.

**Code — KYC status provider.**

```dart
// features/kyc/kyc_status_provider.dart
import 'package:riverpod_annotation/riverpod_annotation.dart';

part 'kyc_status_provider.g.dart';

enum KycStatus { notStarted, pending, verified, rejected }

@riverpod
Future<KycStatus> kycStatus(Ref ref) async {
  final res = await ref.read(apiClientProvider).get<Map<String, dynamic>>('/kyc/status');
  return KycStatus.values.byName(
    (res.data!['status'] as String).toLowerCase().replaceAll('_', ''),
  );
}
```

---

## 6. Wallets & Transaction History

**What.** The home surface: a list of the customer's wallets with
**server-derived balances** and paginated transaction history.

**Why.** Balances are computed by the backend as `SUM(ledger_entries)` — the app
displays them but never computes or caches a stale balance.

**Code — wallets provider.**

```dart
// features/wallets/wallets_provider.dart
import 'package:riverpod_annotation/riverpod_annotation.dart';

part 'wallets_provider.g.dart';

class Wallet {
  const Wallet({required this.id, required this.currency, required this.status});
  final String id;
  final String currency;
  final String status;

  factory Wallet.fromJson(Map<String, dynamic> j) =>
      Wallet(id: j['id'], currency: j['currency'], status: j['status']);
}

@riverpod
Future<List<Wallet>> wallets(Ref ref) async {
  final res = await ref.read(apiClientProvider).get<List<dynamic>>('/wallets');
  return res.data!.map((e) => Wallet.fromJson(e as Map<String, dynamic>)).toList();
}

@riverpod
Future<({String amount, String currency})> walletBalance(
  Ref ref,
  String walletId,
) async {
  final res = await ref.read(apiClientProvider)
      .get<Map<String, dynamic>>('/wallets/$walletId/balance');
  return (amount: res.data!['amount'] as String, currency: res.data!['currency'] as String);
}
```

> **Money is a `String`, never a `double`.** Amounts come over the wire as
> decimal strings (mirroring the backend's `BigDecimal` / `NUMERIC(20,4)`). The
> app formats with `NumberFormat.currency` and never does floating-point
> arithmetic on balances.

---

## 7. Transfers — On-Device Idempotency

**What.** The money-moving flow: choose source wallet, enter amount + destination,
confirm, and submit `POST /v1/transfers` with a client-generated
**`Idempotency-Key`** header.

**Why.** Mobile networks drop responses. If the app resends a transfer after a
timeout, the backend must treat the **second** request as the **same** operation.
The backend enforces this with a unique constraint on the idempotency key; the
app's job is to generate the key **once per user intent** and reuse it across
every retry of that intent.

**Code — idempotent transfer notifier.**

```dart
// features/transfers/transfer_notifier.dart
import 'package:riverpod_annotation/riverpod_annotation.dart';
import 'package:uuid/uuid.dart';

part 'transfer_notifier.g.dart';

class TransferInput {
  const TransferInput({
    required this.fromWalletId,
    required this.toWalletId,
    required this.amount,
  });
  final String fromWalletId;
  final String toWalletId;
  final String amount;
}

@riverpod
class TransferNotifier extends _$TransferNotifier {
  @override
  AsyncValue<void> build() => const AsyncData(null);

  Future<void> submit(TransferInput input) async {
    // Key is generated once per confirmed intent; retries reuse this variable.
    final idempotencyKey = const Uuid().v4();
    state = const AsyncLoading();
    state = await AsyncValue.guard(() async {
      await ref.read(apiClientProvider).post<void>(
        '/transfers',
        data: {
          'sourceWalletId': input.fromWalletId,
          'destWalletId': input.toWalletId,
          'amount': input.amount,
        },
        options: Options(headers: {'Idempotency-Key': idempotencyKey}),
      );
      // Invalidate balances so the UI reflects the new state
      ref.invalidate(walletsProvider);
      ref.invalidate(walletBalanceProvider(input.fromWalletId));
      ref.invalidate(walletBalanceProvider(input.toWalletId));
    });
  }
}
```

```dart
// core/idempotency.dart
import 'package:uuid/uuid.dart';
String newIdempotencyKey() => const Uuid().v4();
```

---

## 8. Push Notifications (Event-Fed)

**What.** Device push for `TransferCompleted`, `KycDecided`, and security events.
These are downstream consumers of the backend's **outbox events** — the app is a
subscriber, not a poller, for real-time updates.

**Why.** Push closes the loop opened by event-driven design: a money event
commits in the backend, the outbox relay fans it out, a notification consumer
turns it into a device push. The user sees "transfer completed" without holding a
screen open.

**Code — FCM registration + handler.**

```dart
// features/notifications/push_service.dart
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

class PushService {
  PushService(this._ref);
  final Ref _ref;

  Future<void> register() async {
    await FirebaseMessaging.instance.requestPermission();
    final deviceToken = await FirebaseMessaging.instance.getToken();
    if (deviceToken == null) return;

    await _ref.read(apiClientProvider).post<void>(
      '/notifications/devices',
      data: {'deviceToken': deviceToken, 'platform': 'mobile'},
    );

    FirebaseMessaging.onMessage.listen((message) {
      switch (message.data['event']) {
        case 'TransferCompleted':
          _ref.invalidate(walletsProvider);
        case 'KycDecided':
          _ref.invalidate(kycStatusProvider);
      }
    });
  }
}
```

---

## 9. Client-Side Hardening

**What.** Defense-in-depth on the device: TLS **certificate pinning**,
**jailbreak / root detection**, screen-capture protection on sensitive screens,
and auto-logout on background timeout.

**Why.** A banking client runs on hostile, sometimes-compromised hardware. These
controls raise the cost of MITM and tampering — but they are
**belt-and-suspenders**: the server still authenticates and authorizes every
request, so a bypass on the device never grants money-moving power it didn't
already have.

**Code — integrity gate.**

```dart
// core/security/integrity.dart
import 'package:flutter_jailbreak_detection/flutter_jailbreak_detection.dart';

class IntegrityResult {
  const IntegrityResult({required this.safe, required this.reasons});
  final bool safe;
  final List<String> reasons;
}

Future<IntegrityResult> checkDeviceIntegrity() async {
  final reasons = <String>[];
  if (await FlutterJailbreakDetection.jailbroken) {
    reasons.add('jailbroken_or_rooted');
  }
  if (await FlutterJailbreakDetection.developerMode) {
    reasons.add('developer_mode');
  }
  return IntegrityResult(safe: reasons.isEmpty, reasons: reasons);
}
```

> In production a failed integrity check blocks money-moving actions and logs
> the reasons (with `trace_id`) to the backend; in development it only warns.

---

## 10. Testing Strategy (Client)

| Layer        | Tool                          | What it covers                                              |
|--------------|-------------------------------|-------------------------------------------------------------|
| Unit         | `flutter_test`                | API client refresh/retry logic, idempotency-key generation, money formatting, providers. |
| Widget       | `flutter_test` + `WidgetTester` | Screens render correct state for loading/error/success; RBAC-hidden actions stay hidden. |
| Integration  | `integration_test`            | Login → KYC → transfer happy path on a simulator/emulator; retry after timeout does not double-charge. |
| Contract     | Shared DTO models + mock Dio  | App DTOs stay in lockstep with the backend `/v1` contract.  |

**Critical test — a retried transfer charges once.**

```dart
// features/transfers/test/idempotency_test.dart
void main() {
  test('reuses the idempotency key across a network-timeout retry', () async {
    final seenKeys = <String>[];
    var callCount = 0;

    final mockDio = MockDio();
    when(() => mockDio.post<void>(
      '/transfers',
      data: any(named: 'data'),
      options: any(named: 'options'),
    )).thenAnswer((inv) async {
      final opts = inv.namedArguments[#options] as Options;
      seenKeys.add(opts.headers!['Idempotency-Key'] as String);
      callCount++;
      if (callCount == 1) throw DioException(requestOptions: RequestOptions());
      return Response(requestOptions: RequestOptions(), statusCode: 201);
    });

    final container = ProviderContainer(
      overrides: [apiClientProvider.overrideWithValue(mockDio)],
    );

    // First attempt fails, second succeeds; idempotency key must be the same.
    await container.read(transferNotifierProvider.notifier).submit(
      const TransferInput(fromWalletId: 'w1', toWalletId: 'w2', amount: '25.00'),
    );

    expect(seenKeys, hasLength(2));
    expect(seenKeys.toSet(), hasLength(1)); // SAME key on retry
  });
}
```

---

## 11. Decision Summary

| Decision                     | Choice                                                        |
|------------------------------|--------------------------------------------------------------|
| Framework                    | Flutter + Dart (single iOS + Android codebase)               |
| Navigation                   | GoRouter (declarative, URL-based, auth redirect guards)       |
| State management             | Riverpod 2 (`AsyncNotifier`, `FutureProvider`)               |
| Refresh-token storage        | flutter_secure_storage (iOS Keychain / Android Keystore)      |
| Access-token storage         | In memory only — never persisted                             |
| Auth model                   | Access JWT (`Bearer`) + opaque rotating refresh token        |
| Biometric unlock             | local_auth (Face ID / fingerprint) gates enclave read         |
| HTTP client                  | Dio with `AuthInterceptor` for transparent refresh           |
| Money safety                 | Client `Idempotency-Key` per confirmed transfer intent       |
| Money representation         | Decimal **strings**, `NumberFormat.currency` — never `double`|
| Real-time updates            | Push (outbox-fed) signals → provider invalidation            |
| Hardening                    | TLS pinning, jailbreak/root detection, FLAG_SECURE           |
| Testing                      | flutter_test + integration_test + mock Dio                   |

> **Bottom line:** the mobile app is a secure, idempotent, server-authoritative
> client. It protects the refresh token in hardware, makes every money request
> safe to retry, formats money exactly, and treats push as a hint to invalidate
> providers — never as the truth itself.
