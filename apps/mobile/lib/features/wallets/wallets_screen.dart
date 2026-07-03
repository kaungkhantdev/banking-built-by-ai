import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../core/api/api_client.dart';
import '../../core/models/api_models.dart';
import '../../shared/widgets/status_badge.dart';
import '../../shared/widgets/empty_state.dart';

class WalletsScreen extends ConsumerStatefulWidget {
  const WalletsScreen({super.key});

  @override
  ConsumerState<WalletsScreen> createState() => _WalletsScreenState();
}

class _WalletsScreenState extends ConsumerState<WalletsScreen> {
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Wallets')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          _ActionCard(
            icon: Icons.add_card_outlined,
            title: 'Open Wallet',
            subtitle: 'Create a new wallet for an account',
            color: const Color(0xFF10B981),
            onTap: () => _showOpenSheet(context),
          ),
          const SizedBox(height: 12),
          _ActionCard(
            icon: Icons.account_balance_outlined,
            title: 'Check Balance',
            subtitle: 'View current wallet balance',
            color: const Color(0xFF4F46E5),
            onTap: () => _showBalanceSheet(context),
          ),
          const SizedBox(height: 12),
          _ActionCard(
            icon: Icons.receipt_long_outlined,
            title: 'Transaction History',
            subtitle: 'Browse wallet transactions',
            color: const Color(0xFF0EA5E9),
            onTap: () => context.push('/transactions'),
          ),
        ],
      ),
    );
  }

  void _showOpenSheet(BuildContext context) => showModalBottomSheet(
        context: context,
        isScrollControlled: true,
        shape: const RoundedRectangleBorder(
            borderRadius: BorderRadius.vertical(top: Radius.circular(24))),
        builder: (_) => _OpenWalletSheet(api: ref.read(apiClientProvider)),
      );

  void _showBalanceSheet(BuildContext context) => showModalBottomSheet(
        context: context,
        isScrollControlled: true,
        shape: const RoundedRectangleBorder(
            borderRadius: BorderRadius.vertical(top: Radius.circular(24))),
        builder: (_) => _BalanceSheet(api: ref.read(apiClientProvider)),
      );
}

class _ActionCard extends StatelessWidget {
  final IconData icon;
  final String title, subtitle;
  final Color color;
  final VoidCallback onTap;
  const _ActionCard({required this.icon, required this.title,
      required this.subtitle, required this.color, required this.onTap});

  @override
  Widget build(BuildContext context) => Card(
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(12),
          child: Padding(
            padding: const EdgeInsets.all(20),
            child: Row(children: [
              Container(
                width: 48, height: 48,
                decoration: BoxDecoration(
                    color: color.withValues(alpha: 0.12),
                    borderRadius: BorderRadius.circular(12)),
                child: Icon(icon, color: color, size: 24),
              ),
              const SizedBox(width: 16),
              Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Text(title,
                    style: const TextStyle(
                        fontWeight: FontWeight.w600, fontSize: 15)),
                const SizedBox(height: 2),
                Text(subtitle,
                    style: TextStyle(
                        fontSize: 12,
                        color: Theme.of(context).colorScheme.onSurfaceVariant)),
              ])),
              Icon(Icons.chevron_right,
                  color: Theme.of(context).colorScheme.onSurfaceVariant),
            ]),
          ),
        ),
      );
}

// ── Open Wallet sheet ─────────────────────────────────────────────────────────

class _OpenWalletSheet extends StatefulWidget {
  final ApiClient api;
  const _OpenWalletSheet({required this.api});

  @override
  State<_OpenWalletSheet> createState() => _OpenWalletSheetState();
}

class _OpenWalletSheetState extends State<_OpenWalletSheet> {
  final _form     = GlobalKey<FormState>();
  final _accCtrl  = TextEditingController();
  final _currCtrl = TextEditingController(text: 'THB');
  bool _loading   = false;
  WalletView? _created;

  static final _uuidRe = RegExp(
      r'^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$',
      caseSensitive: false);

  @override
  void dispose() { _accCtrl.dispose(); _currCtrl.dispose(); super.dispose(); }

  Future<void> _submit() async {
    if (!_form.currentState!.validate()) return;
    setState(() { _loading = true; _created = null; });
    try {
      final w = await widget.api.openWallet(
          _accCtrl.text.trim(), _currCtrl.text.trim().toUpperCase());
      if (mounted) setState(() { _created = w; _loading = false; });
    } catch (e) {
      if (mounted) {
        setState(() => _loading = false);
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('$e')));
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.fromLTRB(
          24, 24, 24, MediaQuery.viewInsetsOf(context).bottom + 24),
      child: Form(
        key: _form,
        child: Column(mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch, children: [
          const Text('Open Wallet',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.w700)),
          const SizedBox(height: 20),
          TextFormField(
            controller: _accCtrl,
            style: const TextStyle(fontFamily: 'monospace', fontSize: 13),
            decoration: const InputDecoration(
              labelText: 'Account ID (UUID)',
              prefixIcon: Icon(Icons.account_circle_outlined),
            ),
            validator: (v) => (v == null || !_uuidRe.hasMatch(v.trim()))
                ? 'Enter a valid UUID' : null,
          ),
          const SizedBox(height: 12),
          TextFormField(
            controller: _currCtrl,
            textCapitalization: TextCapitalization.characters,
            decoration: const InputDecoration(
              labelText: 'Currency (ISO 4217)',
              prefixIcon: Icon(Icons.currency_exchange),
              hintText: 'THB, USD, EUR…',
            ),
            validator: (v) => (v == null || v.trim().length != 3)
                ? 'Enter a 3-letter currency code' : null,
          ),
          const SizedBox(height: 20),
          FilledButton.icon(
            onPressed: _loading ? null : _submit,
            icon: _loading
                ? const SizedBox(height: 18, width: 18,
                    child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                : const Icon(Icons.add_card),
            label: const Text('Open Wallet'),
          ),
          if (_created != null) ...[
            const SizedBox(height: 16),
            _WalletResultCard(wallet: _created!),
          ],
        ]),
      ),
    );
  }
}

// ── Balance sheet ─────────────────────────────────────────────────────────────

class _BalanceSheet extends StatefulWidget {
  final ApiClient api;
  const _BalanceSheet({required this.api});

  @override
  State<_BalanceSheet> createState() => _BalanceSheetState();
}

class _BalanceSheetState extends State<_BalanceSheet> {
  final _form   = GlobalKey<FormState>();
  final _ctrl   = TextEditingController();
  bool _loading = false;
  BalanceView? _balance;

  static final _uuidRe = RegExp(
      r'^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$',
      caseSensitive: false);

  @override
  void dispose() { _ctrl.dispose(); super.dispose(); }

  Future<void> _fetch() async {
    if (!_form.currentState!.validate()) return;
    setState(() { _loading = true; _balance = null; });
    try {
      final b = await widget.api.walletBalance(_ctrl.text.trim());
      if (mounted) setState(() { _balance = b; _loading = false; });
    } catch (e) {
      if (mounted) {
        setState(() => _loading = false);
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('$e')));
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return Padding(
      padding: EdgeInsets.fromLTRB(
          24, 24, 24, MediaQuery.viewInsetsOf(context).bottom + 24),
      child: Form(
        key: _form,
        child: Column(mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch, children: [
          const Text('Check Balance',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.w700)),
          const SizedBox(height: 20),
          TextFormField(
            controller: _ctrl,
            style: const TextStyle(fontFamily: 'monospace', fontSize: 13),
            decoration: const InputDecoration(
              labelText: 'Wallet ID (UUID)',
              prefixIcon: Icon(Icons.wallet_outlined),
            ),
            validator: (v) => (v == null || !_uuidRe.hasMatch(v.trim()))
                ? 'Enter a valid UUID' : null,
          ),
          const SizedBox(height: 16),
          FilledButton.icon(
            onPressed: _loading ? null : _fetch,
            icon: _loading
                ? const SizedBox(height: 18, width: 18,
                    child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                : const Icon(Icons.search),
            label: const Text('Get Balance'),
          ),
          if (_balance != null) ...[
            const SizedBox(height: 20),
            Container(
              padding: const EdgeInsets.all(20),
              decoration: BoxDecoration(
                color: cs.primaryContainer.withValues(alpha: 0.3),
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: cs.primary.withValues(alpha: 0.2)),
              ),
              child: Column(children: [
                Text(_balance!.balance.toStringAsFixed(2),
                    style: Theme.of(context).textTheme.displaySmall?.copyWith(
                        fontWeight: FontWeight.w700, color: cs.primary)),
                const SizedBox(height: 4),
                Text('Current balance',
                    style: TextStyle(color: cs.onSurfaceVariant, fontSize: 13)),
              ]),
            ),
          ],
        ]),
      ),
    );
  }
}

class _WalletResultCard extends StatelessWidget {
  final WalletView wallet;
  const _WalletResultCard({required this.wallet});

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: const Color(0xFFECFDF5),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: const Color(0xFF6EE7B7)),
      ),
      child: Row(children: [
        const Icon(Icons.check_circle, color: Color(0xFF059669)),
        const SizedBox(width: 12),
        Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          const Text('Wallet opened',
              style: TextStyle(
                  fontWeight: FontWeight.w700, color: Color(0xFF065F46))),
          const SizedBox(height: 2),
          Text('${wallet.currency} · ${wallet.status}',
              style: const TextStyle(fontSize: 12, color: Color(0xFF065F46))),
          Text(wallet.id,
              style: const TextStyle(
                  fontFamily: 'monospace', fontSize: 10, color: Color(0xFF065F46))),
        ])),
      ]),
    );
  }
}
