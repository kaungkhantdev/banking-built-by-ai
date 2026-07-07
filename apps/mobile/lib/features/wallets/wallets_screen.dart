import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
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
  final _fmt = NumberFormat('#,##0.00');
  late Future<List<MyWalletView>> _future;

  @override
  void initState() {
    super.initState();
    _future = _load();
  }

  Future<List<MyWalletView>> _load() =>
      ref.read(apiClientProvider).listMyWallets();

  Future<void> _refresh() async {
    final f = _load();
    setState(() => _future = f);
    await f;
  }

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return Scaffold(
      backgroundColor: cs.surfaceContainerLow,
      appBar: AppBar(
        title: const Text('My Wallets'),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: _refresh,
            tooltip: 'Refresh',
          ),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: _refresh,
        child: FutureBuilder<List<MyWalletView>>(
          future: _future,
          builder: (context, snap) {
            if (snap.connectionState == ConnectionState.waiting) {
              return const Center(child: CircularProgressIndicator());
            }
            if (snap.hasError) {
              return ListView(
                children: [
                  const SizedBox(height: 120),
                  EmptyState(
                    icon: Icons.error_outline,
                    title: 'Could not load wallets',
                    subtitle: '${snap.error}',
                    action: FilledButton.tonal(
                        onPressed: _refresh, child: const Text('Retry')),
                  ),
                ],
              );
            }
            final wallets = snap.data ?? [];
            if (wallets.isEmpty) {
              return ListView(
                children: const [
                  SizedBox(height: 120),
                  EmptyState(
                    icon: Icons.account_balance_wallet_outlined,
                    title: 'No wallets yet',
                    subtitle:
                        'Your wallets will appear here once your account is set up.',
                  ),
                ],
              );
            }
            return ListView.separated(
              padding: const EdgeInsets.all(16),
              itemCount: wallets.length,
              separatorBuilder: (_, __) => const SizedBox(height: 12),
              itemBuilder: (_, i) => _WalletCard(
                wallet: wallets[i],
                balanceText: _fmt.format(wallets[i].balance),
                onTap: () => context.push('/transactions', extra: wallets[i].id),
              ),
            );
          },
        ),
      ),
    );
  }
}

class _WalletCard extends StatelessWidget {
  final MyWalletView wallet;
  final String balanceText;
  final VoidCallback onTap;
  const _WalletCard(
      {required this.wallet, required this.balanceText, required this.onTap});

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return Card(
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(12),
        child: Padding(
          padding: const EdgeInsets.all(20),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Container(
                    width: 44,
                    height: 44,
                    alignment: Alignment.center,
                    decoration: BoxDecoration(
                      color: cs.primary.withValues(alpha: 0.12),
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: Text(
                      wallet.currency,
                      style: TextStyle(
                          fontWeight: FontWeight.w700,
                          fontSize: 12,
                          color: cs.primary),
                    ),
                  ),
                  const SizedBox(width: 14),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text('${wallet.currency} Wallet',
                            style: const TextStyle(
                                fontWeight: FontWeight.w600, fontSize: 14)),
                        const SizedBox(height: 4),
                        StatusBadge.forWalletStatus(wallet.status),
                      ],
                    ),
                  ),
                  Icon(Icons.chevron_right, color: cs.onSurfaceVariant),
                ],
              ),
              const SizedBox(height: 16),
              Text('Available balance',
                  style: TextStyle(
                      fontSize: 12, color: cs.onSurfaceVariant)),
              const SizedBox(height: 2),
              Text('$balanceText ${wallet.currency}',
                  style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                      fontWeight: FontWeight.w700, color: cs.onSurface)),
            ],
          ),
        ),
      ),
    );
  }
}
