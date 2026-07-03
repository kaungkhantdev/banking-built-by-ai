import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

const _kAccessKey  = 'bc_access';
const _kRefreshKey = 'bc_refresh';

class TokenStorage {
  static const _store = FlutterSecureStorage(
    aOptions: AndroidOptions(encryptedSharedPreferences: true),
    iOptions: IOSOptions(accessibility: KeychainAccessibility.first_unlock),
  );

  Future<String?> getAccess()  => _store.read(key: _kAccessKey);
  Future<String?> getRefresh() => _store.read(key: _kRefreshKey);

  Future<void> save(String access, String refresh) => Future.wait([
        _store.write(key: _kAccessKey,  value: access),
        _store.write(key: _kRefreshKey, value: refresh),
      ]);

  Future<void> clear() => _store.deleteAll();
}

final tokenStorageProvider = Provider<TokenStorage>((_) => TokenStorage());
