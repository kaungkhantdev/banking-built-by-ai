import 'dart:io';

abstract final class AppConfig {
  // Run `ipconfig getifaddr en0` to find your Mac's LAN IP,
  // then set it here for physical device testing.
  // iOS Simulator can use localhost; physical iPhone needs the LAN IP.
  static const _apiHost = String.fromEnvironment('API_HOST', defaultValue: 'localhost');

  static String get apiBaseUrl {
    if (Platform.isAndroid) return 'http://10.0.2.2:8081';
    return 'http://$_apiHost:8081';
  }

  static const connectTimeoutMs = 10000;
  static const receiveTimeoutMs = 15000;
}
