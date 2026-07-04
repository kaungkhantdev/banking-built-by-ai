import 'dart:io';

/// Runtime configuration, supplied at build/run time via `--dart-define`
/// (compile-time environment). A native app has no "origin" to derive from, so
/// unlike the Angular web app (which uses relative `/v1` URLs + a dev proxy),
/// the mobile client needs an explicit API base URL.
///
/// Resolution order for the base URL:
///   1. `API_BASE_URL` — a full override, wins if set. Use for staging/prod:
///        flutter run --dart-define=API_BASE_URL=https://api.example.com
///   2. `API_HOST` + `API_PORT` — composed for local dev:
///        flutter run --dart-define=API_HOST=192.168.1.37 --dart-define=API_PORT=8080
///
/// Defaults target a local Docker stack on the same machine. On the Android
/// emulator, `localhost` is remapped to `10.0.2.2` (the host loopback alias).
abstract final class AppConfig {
  static const _apiBaseUrlOverride = String.fromEnvironment('API_BASE_URL');
  static const _apiHost = String.fromEnvironment('API_HOST', defaultValue: 'localhost');
  static const _apiPort = String.fromEnvironment('API_PORT', defaultValue: '8080');

  static String get apiBaseUrl {
    if (_apiBaseUrlOverride.isNotEmpty) return _apiBaseUrlOverride;
    final host =
        (Platform.isAndroid && _apiHost == 'localhost') ? '10.0.2.2' : _apiHost;
    return 'http://$host:$_apiPort';
  }

  static const connectTimeoutMs = int.fromEnvironment(
      'API_CONNECT_TIMEOUT_MS', defaultValue: 10000);
  static const receiveTimeoutMs = int.fromEnvironment(
      'API_RECEIVE_TIMEOUT_MS', defaultValue: 15000);
}
