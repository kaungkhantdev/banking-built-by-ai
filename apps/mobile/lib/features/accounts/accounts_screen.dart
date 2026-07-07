import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../core/api/api_client.dart';
import '../../core/models/api_models.dart';
import '../../shared/format.dart';
import '../../shared/widgets/empty_state.dart';

const _brand = Color(0xFF4F46E5);

class AccountsScreen extends ConsumerStatefulWidget {
  const AccountsScreen({super.key});

  @override
  ConsumerState<AccountsScreen> createState() => _AccountsScreenState();
}

class _AccountsScreenState extends ConsumerState<AccountsScreen> {
  List<AccountListItem> _items = [];
  bool _loading = true;
  bool _opening = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() { _loading = true; _error = null; });
    try {
      final items = await ref.read(apiClientProvider).listMyAccounts();
      if (mounted) setState(() { _items = items; _loading = false; });
    } catch (e) {
      if (mounted) setState(() { _error = e.toString(); _loading = false; });
    }
  }

  Future<void> _openAccount() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Open a new account'),
        content: const Text(
            'A new bank account will be opened in your name. '
            'You can add wallets and start transacting once it is active.'),
        actions: [
          TextButton(
              onPressed: () => Navigator.pop(ctx, false),
              child: const Text('Cancel')),
          FilledButton(
              onPressed: () => Navigator.pop(ctx, true),
              child: const Text('Open account')),
        ],
      ),
    );
    if (confirmed != true) return;

    setState(() => _opening = true);
    try {
      final acc = await ref.read(apiClientProvider).openMyAccount();
      await _load();
      if (mounted) {
        setState(() => _opening = false);
        ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Account opened')));
        context.push('/accounts/${acc.id}');
      }
    } catch (e) {
      if (mounted) {
        setState(() => _opening = false);
        ScaffoldMessenger.of(context)
            .showSnackBar(SnackBar(content: Text('Could not open account: $e')));
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Accounts'),
        actions: [
          IconButton(
              icon: const Icon(Icons.refresh),
              onPressed: _loading ? null : _load),
        ],
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: _opening ? null : _openAccount,
        icon: _opening
            ? const SizedBox(
                height: 18,
                width: 18,
                child: CircularProgressIndicator(
                    strokeWidth: 2, color: Colors.white))
            : const Icon(Icons.add),
        label: const Text('Open account'),
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? _ErrorState(message: _error!, onRetry: _load)
              : _items.isEmpty
                  ? _EmptyAccounts(onOpen: _openAccount)
                  : RefreshIndicator(
                      onRefresh: _load,
                      child: ListView.separated(
                        padding: const EdgeInsets.fromLTRB(16, 12, 16, 96),
                        itemCount: _items.length,
                        separatorBuilder: (_, __) => const SizedBox(height: 14),
                        itemBuilder: (context, i) => _AccountCard(
                          item: _items[i],
                          onTap: () => context.push(
                            '/accounts/${_items[i].id}',
                            extra: _items[i],
                          ),
                        ),
                      ),
                    ),
    );
  }
}

// ── Account card ──────────────────────────────────────────────────────────────

class _AccountCard extends StatelessWidget {
  final AccountListItem item;
  final VoidCallback onTap;
  const _AccountCard({required this.item, required this.onTap});

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(20),
        child: Ink(
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(20),
            gradient: const LinearGradient(
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
              colors: [Color(0xFF4F46E5), Color(0xFF7C3AED)],
            ),
            boxShadow: [
              BoxShadow(
                color: _brand.withValues(alpha: 0.25),
                blurRadius: 18,
                offset: const Offset(0, 10),
              ),
            ],
          ),
          child: Padding(
            padding: const EdgeInsets.all(20),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    const Icon(Icons.account_balance,
                        color: Colors.white70, size: 20),
                    const SizedBox(width: 8),
                    const Text('Bank Account',
                        style: TextStyle(
                            color: Colors.white70,
                            fontSize: 12,
                            fontWeight: FontWeight.w500)),
                    const Spacer(),
                    _WhiteBadge(item.status),
                  ],
                ),
                const SizedBox(height: 22),
                Text(
                  Fmt.maskedAccountNumber(item.id),
                  style: const TextStyle(
                    color: Colors.white,
                    fontSize: 19,
                    fontWeight: FontWeight.w700,
                    letterSpacing: 2,
                    fontFamily: 'monospace',
                  ),
                ),
                const SizedBox(height: 18),
                Row(
                  children: [
                    Expanded(
                      child: Text(item.ownerEmail,
                          style: const TextStyle(
                              color: Colors.white, fontSize: 12),
                          overflow: TextOverflow.ellipsis),
                    ),
                    Text('Opened ${Fmt.date(item.createdAt)}',
                        style: const TextStyle(
                            color: Colors.white70, fontSize: 11)),
                    const SizedBox(width: 4),
                    const Icon(Icons.chevron_right,
                        color: Colors.white70, size: 18),
                  ],
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _WhiteBadge extends StatelessWidget {
  final String label;
  const _WhiteBadge(this.label);
  @override
  Widget build(BuildContext context) => Container(
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
        decoration: BoxDecoration(
          color: Colors.white.withValues(alpha: 0.2),
          borderRadius: BorderRadius.circular(20),
        ),
        child: Text(label,
            style: const TextStyle(
                color: Colors.white,
                fontSize: 11,
                fontWeight: FontWeight.w700,
                letterSpacing: 0.4)),
      );
}

// ── Empty / error states ──────────────────────────────────────────────────────

class _EmptyAccounts extends StatelessWidget {
  final VoidCallback onOpen;
  const _EmptyAccounts({required this.onOpen});
  @override
  Widget build(BuildContext context) {
    return ListView(
      children: [
        const SizedBox(height: 80),
        const EmptyState(
          icon: Icons.account_balance_outlined,
          title: 'No accounts yet',
          subtitle: 'Open your first account to get started',
        ),
        const SizedBox(height: 20),
        Center(
          child: FilledButton.icon(
            onPressed: onOpen,
            icon: const Icon(Icons.add),
            label: const Text('Open account'),
            style: FilledButton.styleFrom(
                minimumSize: const Size(200, 48)),
          ),
        ),
      ],
    );
  }
}

class _ErrorState extends StatelessWidget {
  final String message;
  final VoidCallback onRetry;
  const _ErrorState({required this.message, required this.onRetry});
  @override
  Widget build(BuildContext context) {
    return ListView(
      children: [
        const SizedBox(height: 80),
        EmptyState(
            icon: Icons.error_outline, title: 'Error', subtitle: message),
        const SizedBox(height: 16),
        Center(
          child: OutlinedButton.icon(
            onPressed: onRetry,
            icon: const Icon(Icons.refresh),
            label: const Text('Retry'),
            style: OutlinedButton.styleFrom(minimumSize: const Size(160, 46)),
          ),
        ),
      ],
    );
  }
}
