import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/api/api_client.dart';
import '../../core/models/api_models.dart';
import '../../shared/format.dart';
import '../../shared/widgets/status_badge.dart';

const _brand = Color(0xFF4F46E5);

class AccountDetailScreen extends ConsumerStatefulWidget {
  final String accountId;

  /// Optional list item the user tapped — lets the hero render instantly
  /// while the full overview loads.
  final AccountListItem? initial;

  const AccountDetailScreen({super.key, required this.accountId, this.initial});

  @override
  ConsumerState<AccountDetailScreen> createState() => _AccountDetailScreenState();
}

class _AccountDetailScreenState extends ConsumerState<AccountDetailScreen> {
  AccountOverview? _data;
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() { _loading = true; _error = null; });
    try {
      final d = await ref.read(apiClientProvider).accountOverview(widget.accountId);
      if (mounted) setState(() { _data = d; _loading = false; });
    } catch (e) {
      if (mounted) setState(() { _error = e.toString(); _loading = false; });
    }
  }

  @override
  Widget build(BuildContext context) {
    final email = _data?.ownerEmail ?? widget.initial?.ownerEmail ?? '';
    final status = _data?.status ?? widget.initial?.status ?? 'PENDING';

    return Scaffold(
      appBar: AppBar(title: const Text('Account')),
      body: RefreshIndicator(
        onRefresh: _load,
        child: ListView(
          padding: const EdgeInsets.fromLTRB(16, 12, 16, 32),
          children: [
            _HeroCard(
              accountId: widget.accountId,
              email: email,
              status: status,
            ),
            const SizedBox(height: 20),
            if (_loading && _data == null)
              const Padding(
                padding: EdgeInsets.only(top: 48),
                child: Center(child: CircularProgressIndicator()),
              )
            else if (_error != null && _data == null)
              _ErrorBox(message: _error!, onRetry: _load)
            else if (_data != null) ...[
              _DetailsSection(data: _data!),
              const SizedBox(height: 24),
              _WalletsSection(wallets: _data!.wallets),
              const SizedBox(height: 24),
              _ActivitySection(transactions: _data!.recentTransactions),
            ],
          ],
        ),
      ),
    );
  }
}

// ── Hero card ─────────────────────────────────────────────────────────────────

class _HeroCard extends StatelessWidget {
  final String accountId, email, status;
  const _HeroCard(
      {required this.accountId, required this.email, required this.status});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(22),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(20),
        gradient: const LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [Color(0xFF4F46E5), Color(0xFF7C3AED)],
        ),
        boxShadow: [
          BoxShadow(
            color: _brand.withValues(alpha: 0.3),
            blurRadius: 24,
            offset: const Offset(0, 12),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Icon(Icons.account_balance, color: Colors.white70, size: 22),
              const SizedBox(width: 8),
              const Text('Bank Account',
                  style: TextStyle(
                      color: Colors.white70,
                      fontSize: 13,
                      fontWeight: FontWeight.w500)),
              const Spacer(),
              _WhiteBadge(status),
            ],
          ),
          const SizedBox(height: 26),
          Text(
            Fmt.accountNumber(accountId),
            style: const TextStyle(
              color: Colors.white,
              fontSize: 22,
              fontWeight: FontWeight.w700,
              letterSpacing: 2.5,
              fontFamily: 'monospace',
            ),
          ),
          const SizedBox(height: 20),
          Row(
            children: [
              const Icon(Icons.person_outline, color: Colors.white70, size: 15),
              const SizedBox(width: 6),
              Expanded(
                child: Text(email,
                    style: const TextStyle(color: Colors.white, fontSize: 13),
                    overflow: TextOverflow.ellipsis),
              ),
            ],
          ),
        ],
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

// ── Holder / KYC details ──────────────────────────────────────────────────────

class _DetailsSection extends StatelessWidget {
  final AccountOverview data;
  const _DetailsSection({required this.data});

  @override
  Widget build(BuildContext context) {
    return _Card(
      title: 'Holder details',
      child: Column(
        children: [
          _InfoRow(
            icon: Icons.mail_outline,
            label: 'Holder',
            value: data.ownerEmail,
          ),
          const Divider(height: 20),
          _InfoRow(
            icon: Icons.verified_user_outlined,
            label: 'KYC status',
            trailing: StatusBadge.forKycStatus(data.kycStatus),
          ),
          const Divider(height: 20),
          _InfoRow(
            icon: Icons.toggle_on_outlined,
            label: 'Account status',
            trailing: StatusBadge.forAccountStatus(data.status),
          ),
          const Divider(height: 20),
          _InfoRow(
            icon: Icons.event_outlined,
            label: 'Opened',
            value: Fmt.date(data.createdAt),
          ),
          const Divider(height: 20),
          _InfoRow(
            icon: Icons.tag,
            label: 'Account ID',
            value: data.id,
            mono: true,
            onCopy: () {
              Clipboard.setData(ClipboardData(text: data.id));
              ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(content: Text('Account ID copied')));
            },
          ),
        ],
      ),
    );
  }
}

// ── Wallets ───────────────────────────────────────────────────────────────────

class _WalletsSection extends StatelessWidget {
  final List<WalletBrief> wallets;
  const _WalletsSection({required this.wallets});

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return _Card(
      title: 'Wallets',
      trailing: Text('${wallets.length}',
          style: TextStyle(color: cs.onSurfaceVariant, fontSize: 13)),
      child: wallets.isEmpty
          ? const _EmptyHint(
              icon: Icons.account_balance_wallet_outlined,
              text: 'No wallets yet on this account')
          : Column(
              children: [
                for (var i = 0; i < wallets.length; i++) ...[
                  if (i != 0) const Divider(height: 20),
                  _WalletRow(wallets[i]),
                ]
              ],
            ),
    );
  }
}

class _WalletRow extends StatelessWidget {
  final WalletBrief wallet;
  const _WalletRow(this.wallet);

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return Row(
      children: [
        Container(
          width: 42,
          height: 42,
          decoration: BoxDecoration(
            color: _brand.withValues(alpha: 0.1),
            borderRadius: BorderRadius.circular(10),
          ),
          alignment: Alignment.center,
          child: Text(wallet.currency,
              style: const TextStyle(
                  color: _brand, fontWeight: FontWeight.w700, fontSize: 12)),
        ),
        const SizedBox(width: 12),
        Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('${wallet.currency} wallet',
                style: const TextStyle(
                    fontWeight: FontWeight.w600, fontSize: 14)),
            const SizedBox(height: 2),
            StatusBadge.forWalletStatus(wallet.status),
          ],
        ),
        const Spacer(),
        Text(
          Fmt.money(wallet.balance),
          style: TextStyle(
              fontWeight: FontWeight.w700, fontSize: 16, color: cs.onSurface),
        ),
      ],
    );
  }
}

// ── Recent activity ───────────────────────────────────────────────────────────

class _ActivitySection extends StatelessWidget {
  final List<TxBrief> transactions;
  const _ActivitySection({required this.transactions});

  @override
  Widget build(BuildContext context) {
    return _Card(
      title: 'Recent activity',
      child: transactions.isEmpty
          ? const _EmptyHint(
              icon: Icons.receipt_long_outlined,
              text: 'No transactions yet')
          : Column(
              children: [
                for (var i = 0; i < transactions.length; i++) ...[
                  if (i != 0) const Divider(height: 18),
                  _TxRow(transactions[i]),
                ]
              ],
            ),
    );
  }
}

class _TxRow extends StatelessWidget {
  final TxBrief tx;
  const _TxRow(this.tx);

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final isCredit = tx.direction == 'CREDIT';
    final color = isCredit ? const Color(0xFF059669) : const Color(0xFFDC2626);
    return Row(
      children: [
        Container(
          width: 38,
          height: 38,
          decoration: BoxDecoration(
            color: color.withValues(alpha: 0.1),
            borderRadius: BorderRadius.circular(10),
          ),
          child: Icon(
              isCredit ? Icons.south_west : Icons.north_east,
              color: color,
              size: 18),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                tx.memo?.isNotEmpty == true
                    ? tx.memo!
                    : (isCredit ? 'Received' : 'Sent'),
                style: const TextStyle(
                    fontWeight: FontWeight.w600, fontSize: 14),
                overflow: TextOverflow.ellipsis,
              ),
              const SizedBox(height: 2),
              Text(Fmt.dateTime(tx.postedAt),
                  style: TextStyle(color: cs.onSurfaceVariant, fontSize: 12)),
            ],
          ),
        ),
        const SizedBox(width: 8),
        Text(
          '${Fmt.signed(tx.amount, tx.direction)} ${tx.currency}',
          style: TextStyle(
              fontWeight: FontWeight.w700, fontSize: 14, color: color),
        ),
      ],
    );
  }
}

// ── Reusable bits ─────────────────────────────────────────────────────────────

class _Card extends StatelessWidget {
  final String title;
  final Widget child;
  final Widget? trailing;
  const _Card({required this.title, required this.child, this.trailing});

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(18),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(children: [
              Text(title,
                  style: const TextStyle(
                      fontWeight: FontWeight.w700, fontSize: 15)),
              const Spacer(),
              if (trailing != null) trailing!,
            ]),
            const SizedBox(height: 14),
            child,
          ],
        ),
      ),
    );
  }
}

class _InfoRow extends StatelessWidget {
  final IconData icon;
  final String label;
  final String? value;
  final Widget? trailing;
  final bool mono;
  final VoidCallback? onCopy;
  const _InfoRow({
    required this.icon,
    required this.label,
    this.value,
    this.trailing,
    this.mono = false,
    this.onCopy,
  });

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return Row(
      crossAxisAlignment: CrossAxisAlignment.center,
      children: [
        Icon(icon, size: 18, color: cs.onSurfaceVariant),
        const SizedBox(width: 12),
        Text(label, style: TextStyle(color: cs.onSurfaceVariant, fontSize: 13)),
        const SizedBox(width: 12),
        Expanded(
          child: trailing != null
              ? Align(alignment: Alignment.centerRight, child: trailing)
              : Text(
                  value ?? '',
                  textAlign: TextAlign.right,
                  overflow: TextOverflow.ellipsis,
                  style: TextStyle(
                    fontWeight: FontWeight.w600,
                    fontSize: mono ? 11 : 13,
                    fontFamily: mono ? 'monospace' : null,
                  ),
                ),
        ),
        if (onCopy != null)
          Padding(
            padding: const EdgeInsets.only(left: 4),
            child: InkWell(
              onTap: onCopy,
              borderRadius: BorderRadius.circular(6),
              child: Icon(Icons.copy_outlined, size: 16, color: cs.primary),
            ),
          ),
      ],
    );
  }
}

class _EmptyHint extends StatelessWidget {
  final IconData icon;
  final String text;
  const _EmptyHint({required this.icon, required this.text});
  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 12),
      child: Row(children: [
        Icon(icon, size: 20, color: cs.onSurfaceVariant),
        const SizedBox(width: 10),
        Text(text, style: TextStyle(color: cs.onSurfaceVariant, fontSize: 13)),
      ]),
    );
  }
}

class _ErrorBox extends StatelessWidget {
  final String message;
  final VoidCallback onRetry;
  const _ErrorBox({required this.message, required this.onRetry});
  @override
  Widget build(BuildContext context) {
    return _Card(
      title: 'Could not load account',
      child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
        Text(message,
            style: TextStyle(
                color: Theme.of(context).colorScheme.onSurfaceVariant,
                fontSize: 13)),
        const SizedBox(height: 12),
        OutlinedButton.icon(
          onPressed: onRetry,
          icon: const Icon(Icons.refresh),
          label: const Text('Retry'),
        ),
      ]),
    );
  }
}
