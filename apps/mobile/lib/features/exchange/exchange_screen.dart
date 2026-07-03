import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import '../../core/api/api_client.dart';
import '../../core/models/api_models.dart';

class ExchangeScreen extends ConsumerStatefulWidget {
  const ExchangeScreen({super.key});

  @override
  ConsumerState<ExchangeScreen> createState() => _ExchangeScreenState();
}

class _ExchangeScreenState extends ConsumerState<ExchangeScreen> {
  List<String> _currencies = ['USD', 'EUR', 'GBP', 'THB', 'SGD', 'JPY', 'AUD'];
  String _from = 'USD';
  String _to   = 'THB';
  bool _loading        = false;
  bool _currencyLoading = true;
  ExchangeRateView? _rate;

  @override
  void initState() {
    super.initState();
    _loadCurrencies();
  }

  Future<void> _loadCurrencies() async {
    try {
      final list = await ref.read(apiClientProvider).supportedCurrencies();
      if (mounted) setState(() { _currencies = list; _currencyLoading = false; });
    } catch (_) {
      if (mounted) setState(() => _currencyLoading = false);
    }
  }

  Future<void> _lookup() async {
    setState(() { _loading = true; _rate = null; });
    try {
      final r = await ref.read(apiClientProvider).exchangeRate(_from, _to);
      if (mounted) setState(() { _rate = r; _loading = false; });
    } catch (e) {
      if (mounted) {
        setState(() => _loading = false);
        ScaffoldMessenger.of(context)
            .showSnackBar(SnackBar(content: Text('Rate not available: $e')));
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return Scaffold(
      appBar: AppBar(title: const Text('Exchange Rates')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [

          // ── Selector ──────────────────────────────────────────────────────
          Card(
            child: Padding(
              padding: const EdgeInsets.all(20),
              child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch, children: [
                const Text('Currency Pair',
                    style: TextStyle(
                        fontWeight: FontWeight.w600, fontSize: 15)),
                const SizedBox(height: 16),
                Row(children: [
                  Expanded(
                    child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start, children: [
                      const Text('From',
                          style: TextStyle(fontSize: 12, color: Colors.grey)),
                      const SizedBox(height: 6),
                      _CurrencyDropdown(
                        value: _from,
                        currencies: _currencies,
                        onChanged: (v) => setState(() => _from = v),
                      ),
                    ]),
                  ),
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 12),
                    child: IconButton(
                      icon: const Icon(Icons.swap_horiz),
                      onPressed: () => setState(() {
                        final tmp = _from; _from = _to; _to = tmp;
                      }),
                    ),
                  ),
                  Expanded(
                    child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start, children: [
                      const Text('To',
                          style: TextStyle(fontSize: 12, color: Colors.grey)),
                      const SizedBox(height: 6),
                      _CurrencyDropdown(
                        value: _to,
                        currencies: _currencies,
                        onChanged: (v) => setState(() => _to = v),
                      ),
                    ]),
                  ),
                ]),
                const SizedBox(height: 16),
                FilledButton.icon(
                  onPressed: (_loading || _currencyLoading) ? null : _lookup,
                  icon: _loading
                      ? const SizedBox(height: 18, width: 18,
                          child: CircularProgressIndicator(
                              strokeWidth: 2, color: Colors.white))
                      : const Icon(Icons.search),
                  label: const Text('Get Rate'),
                ),
              ]),
            ),
          ),

          // ── Result ────────────────────────────────────────────────────────
          if (_rate != null) ...[
            const SizedBox(height: 16),
            Container(
              padding: const EdgeInsets.all(24),
              decoration: BoxDecoration(
                gradient: LinearGradient(
                  colors: [cs.primary, cs.primary.withValues(alpha: 0.75)],
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                ),
                borderRadius: BorderRadius.circular(16),
              ),
              child: Column(children: [
                Text('1 ${_rate!.fromCurrency}',
                    style: TextStyle(
                        color: cs.onPrimary.withValues(alpha: 0.8),
                        fontSize: 14)),
                const SizedBox(height: 8),
                Text(
                  '${NumberFormat('#,##0.####').format(_rate!.rate)} ${_rate!.toCurrency}',
                  style: TextStyle(
                      color: cs.onPrimary,
                      fontSize: 32,
                      fontWeight: FontWeight.w800),
                ),
                const SizedBox(height: 8),
                Text(
                  'Rate as of ${_fmtDate(_rate!.effectiveAt)}',
                  style: TextStyle(
                      color: cs.onPrimary.withValues(alpha: 0.7),
                      fontSize: 12),
                ),
              ]),
            ),
          ],
        ],
      ),
    );
  }

  String _fmtDate(String iso) {
    try {
      return DateFormat('dd MMM yyyy, HH:mm')
          .format(DateTime.parse(iso).toLocal());
    } catch (_) {
      return iso.substring(0, 10);
    }
  }
}

class _CurrencyDropdown extends StatelessWidget {
  final String value;
  final List<String> currencies;
  final ValueChanged<String> onChanged;
  const _CurrencyDropdown(
      {required this.value, required this.currencies, required this.onChanged});

  @override
  Widget build(BuildContext context) => DropdownButtonFormField<String>(
        value: currencies.contains(value) ? value : currencies.first,
        decoration: const InputDecoration(isDense: true),
        items: currencies
            .map((c) => DropdownMenuItem(value: c, child: Text(c)))
            .toList(),
        onChanged: (v) { if (v != null) onChanged(v); },
      );
}
