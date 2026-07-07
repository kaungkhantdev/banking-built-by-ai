import 'package:intl/intl.dart';

/// Presentation helpers shared across feature screens.
abstract final class Fmt {
  static final _money = NumberFormat('#,##0.00');
  static final _date = DateFormat('d MMM yyyy');
  static final _dateTime = DateFormat('d MMM yyyy · HH:mm');

  /// `1,234.50 THB`
  static String money(num value, [String? currency]) =>
      currency == null ? _money.format(value) : '${_money.format(value)} $currency';

  /// Signed amount for a ledger row, e.g. `+1,200.00` / `-350.00`.
  static String signed(num amount, String direction) {
    final sign = direction == 'CREDIT' ? '+' : '-';
    return '$sign${_money.format(amount)}';
  }

  static String date(String iso) => _date.format(DateTime.parse(iso).toLocal());

  static String dateTime(String iso) =>
      _dateTime.format(DateTime.parse(iso).toLocal());

  /// A human account "number" derived from the UUID — grouped last 12 hex
  /// digits, e.g. `0000 0000 0001`. Deterministic, non-sensitive, readable.
  static String accountNumber(String id) {
    final hex = id.replaceAll('-', '');
    final tail = hex.substring(hex.length - 12).toUpperCase();
    final b = StringBuffer();
    for (var i = 0; i < tail.length; i++) {
      if (i != 0 && i % 4 == 0) b.write(' ');
      b.write(tail[i]);
    }
    return b.toString();
  }

  /// Masked form for list rows: `•••• •••• 0001`.
  static String maskedAccountNumber(String id) {
    final n = accountNumber(id);
    final last = n.split(' ').last;
    return '•••• •••• $last';
  }
}
