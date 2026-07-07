import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import '../../core/api/api_client.dart';
import '../../core/models/api_models.dart';

/// The signed-in customer's own wallets (with balances). Auto-disposed so it
/// refetches when a picker is reopened.
final myWalletsProvider = FutureProvider.autoDispose<List<MyWalletView>>((ref) {
  return ref.read(apiClientProvider).listMyWallets();
});

/// A dropdown that lets a customer pick one of their own wallets by
/// currency + balance instead of pasting a UUID. The selected value is the
/// wallet id.
class WalletPicker extends ConsumerWidget {
  final String label;
  final String? value;
  final ValueChanged<String?> onChanged;
  final String? Function(String?)? validator;

  const WalletPicker({
    super.key,
    required this.label,
    required this.value,
    required this.onChanged,
    this.validator,
  });

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final async = ref.watch(myWalletsProvider);
    final cs = Theme.of(context).colorScheme;

    return async.when(
      loading: () => InputDecorator(
        decoration: InputDecoration(
          labelText: label,
          prefixIcon: const Icon(Icons.wallet_outlined),
        ),
        child: const SizedBox(
          height: 20,
          child: Align(
            alignment: Alignment.centerLeft,
            child: SizedBox(
                height: 16, width: 16,
                child: CircularProgressIndicator(strokeWidth: 2)),
          ),
        ),
      ),
      error: (e, _) => InputDecorator(
        decoration: InputDecoration(
          labelText: label,
          prefixIcon: Icon(Icons.error_outline, color: cs.error),
        ),
        child: Text('Could not load wallets',
            style: TextStyle(color: cs.error, fontSize: 13)),
      ),
      data: (wallets) {
        if (wallets.isEmpty) {
          return InputDecorator(
            decoration: InputDecoration(
              labelText: label,
              prefixIcon: const Icon(Icons.wallet_outlined),
            ),
            child: Text('No wallets available',
                style: TextStyle(color: cs.onSurfaceVariant, fontSize: 13)),
          );
        }
        final fmt = NumberFormat('#,##0.00');
        return DropdownButtonFormField<String>(
          initialValue: wallets.any((w) => w.id == value) ? value : null,
          isExpanded: true,
          decoration: InputDecoration(
            labelText: label,
            prefixIcon: const Icon(Icons.wallet_outlined),
          ),
          items: wallets
              .map((w) => DropdownMenuItem(
                    value: w.id,
                    child: Text(
                      '${w.currency} · ${fmt.format(w.balance)}'
                      '${w.status == 'ACTIVE' ? '' : ' (${w.status})'}',
                      overflow: TextOverflow.ellipsis,
                    ),
                  ))
              .toList(),
          validator: validator,
          onChanged: onChanged,
        );
      },
    );
  }
}
