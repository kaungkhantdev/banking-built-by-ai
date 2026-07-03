import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import '../../core/api/api_client.dart';
import '../../core/models/api_models.dart';

class TransactionsScreen extends ConsumerStatefulWidget {
  final String? walletId;
  const TransactionsScreen({super.key, this.walletId});

  @override
  ConsumerState<TransactionsScreen> createState() => _TransactionsScreenState();
}

class _TransactionsScreenState extends ConsumerState<TransactionsScreen> {
  final _ctrl = TextEditingController();
  final _fmt  = NumberFormat('#,##0.00##');
  String? _walletId;
  String? _direction;
  int _page = 0;
  bool _loading = false;
  ApiPage<TransactionView>? _result;
  String? _error;

  @override
  void initState() {
    super.initState();
    if (widget.walletId != null) {
      _ctrl.text = widget.walletId!;
      WidgetsBinding.instance.addPostFrameCallback((_) => _load(widget.walletId!));
    }
  }

  @override
  void dispose() { _ctrl.dispose(); super.dispose(); }

  Future<void> _load(String id, {int page = 0}) async {
    setState(() { _loading = true; _walletId = id; _page = page; _error = null; });
    try {
      final p = await ref.read(apiClientProvider)
          .listTransactions(id, page: page, direction: _direction);
      if (mounted) setState(() { _result = p; _loading = false; });
    } catch (e) {
      if (mounted) setState(() { _loading = false; _error = e.toString(); });
    }
  }

  void _submit() {
    final id = _ctrl.text.trim();
    if (id.isEmpty) return;
    _load(id);
  }

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return Scaffold(
      backgroundColor: cs.surfaceContainerLow,
      appBar: AppBar(
        backgroundColor: cs.surface,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_ios_new),
          onPressed: () => context.pop(),
        ),
        title: const Text('Transaction History'),
      ),
      body: Column(children: [

        // ── Wallet ID input ──────────────────────────────────────────────
        Container(
          color: cs.surface,
          padding: const EdgeInsets.fromLTRB(16, 12, 16, 12),
          child: Column(crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
            Row(children: [
              Expanded(
                child: TextField(
                  controller: _ctrl,
                  style: const TextStyle(fontFamily: 'monospace', fontSize: 12),
                  decoration: InputDecoration(
                    hintText: 'Paste wallet ID (UUID)',
                    prefixIcon: const Icon(Icons.wallet_outlined, size: 20),
                    filled: true,
                    fillColor: cs.surfaceContainerLow,
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(10),
                      borderSide: BorderSide.none,
                    ),
                    contentPadding: const EdgeInsets.symmetric(
                        horizontal: 12, vertical: 10),
                    isDense: true,
                  ),
                  onSubmitted: (_) => _submit(),
                ),
              ),
              const SizedBox(width: 8),
              FilledButton(
                onPressed: _loading ? null : _submit,
                style: FilledButton.styleFrom(
                    padding: const EdgeInsets.symmetric(
                        horizontal: 16, vertical: 12)),
                child: const Text('Load'),
              ),
            ]),
            if (_walletId != null) ...[
              const SizedBox(height: 8),
              SizedBox(
                height: 32,
                child: ListView(
                  scrollDirection: Axis.horizontal,
                  children: [
                    _FilterChip('All', _direction == null, () {
                      setState(() => _direction = null);
                      _load(_walletId!);
                    }),
                    const SizedBox(width: 6),
                    _FilterChip('Credits', _direction == 'CREDIT', () {
                      setState(() => _direction = 'CREDIT');
                      _load(_walletId!);
                    }),
                    const SizedBox(width: 6),
                    _FilterChip('Debits', _direction == 'DEBIT', () {
                      setState(() => _direction = 'DEBIT');
                      _load(_walletId!);
                    }),
                  ],
                ),
              ),
            ],
          ]),
        ),

        const SizedBox(height: 1),

        // ── Body ─────────────────────────────────────────────────────────
        Expanded(child: _buildBody(cs)),
      ]),
    );
  }

  Widget _buildBody(ColorScheme cs) {
    if (_loading) {
      return const Center(child: CircularProgressIndicator());
    }

    if (_error != null) {
      return _Placeholder(
        icon: Icons.error_outline,
        iconColor: cs.error,
        title: 'Failed to load',
        subtitle: _error!,
        action: FilledButton.icon(
          icon: const Icon(Icons.refresh),
          label: const Text('Retry'),
          onPressed: () => _load(_walletId!),
        ),
      );
    }

    if (_walletId == null) {
      return _Placeholder(
        icon: Icons.receipt_long_outlined,
        iconColor: cs.primary,
        title: 'Enter a wallet ID',
        subtitle: 'Paste your wallet UUID above and tap Load\nto view transaction history.',
      );
    }

    if (_result == null || _result!.content.isEmpty) {
      return _Placeholder(
        icon: Icons.inbox_outlined,
        iconColor: cs.onSurfaceVariant,
        title: 'No transactions',
        subtitle: 'This wallet has no transactions yet.',
      );
    }

    return Column(children: [
      Expanded(
        child: ListView.builder(
          padding: const EdgeInsets.symmetric(vertical: 8),
          itemCount: _result!.content.length,
          itemBuilder: (ctx, i) => _TxTile(tx: _result!.content[i], fmt: _fmt),
        ),
      ),
      if (_result!.totalPages > 1)
        _Pager(
          current: _page,
          total: _result!.totalPages,
          onPrev: _page > 0 ? () => _load(_walletId!, page: _page - 1) : null,
          onNext: _page < _result!.totalPages - 1
              ? () => _load(_walletId!, page: _page + 1)
              : null,
        ),
    ]);
  }
}

// ── Transaction tile ──────────────────────────────────────────────────────────

class _TxTile extends StatelessWidget {
  final TransactionView tx;
  final NumberFormat fmt;
  const _TxTile({required this.tx, required this.fmt});

  @override
  Widget build(BuildContext context) {
    final isCredit = tx.direction == 'CREDIT';
    final cs = Theme.of(context).colorScheme;
    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
      decoration: BoxDecoration(
        color: cs.surface,
        borderRadius: BorderRadius.circular(12),
      ),
      child: ListTile(
        contentPadding:
            const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
        leading: CircleAvatar(
          backgroundColor: isCredit
              ? const Color(0xFFD1FAE5)
              : const Color(0xFFFEE2E2),
          child: Icon(
            isCredit ? Icons.arrow_downward : Icons.arrow_upward,
            size: 18,
            color: isCredit
                ? const Color(0xFF059669)
                : const Color(0xFFDC2626),
          ),
        ),
        title: Text(
          '${isCredit ? '+' : '−'}${fmt.format(tx.amount)} ${tx.currency}',
          style: TextStyle(
            fontWeight: FontWeight.w700,
            fontSize: 15,
            color: isCredit
                ? const Color(0xFF059669)
                : const Color(0xFFDC2626),
          ),
        ),
        subtitle: Text(
          tx.memo ?? tx.transactionId.substring(0, 8),
          style: TextStyle(fontSize: 12, color: cs.onSurfaceVariant),
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
        ),
        trailing: Text(
          _fmt(tx.postedAt),
          style: TextStyle(fontSize: 11, color: cs.onSurfaceVariant),
        ),
      ),
    );
  }

  String _fmt(String iso) {
    try {
      return DateFormat('dd MMM\nHH:mm').format(DateTime.parse(iso).toLocal());
    } catch (_) {
      return iso.substring(0, 10);
    }
  }
}

// ── Placeholder ───────────────────────────────────────────────────────────────

class _Placeholder extends StatelessWidget {
  final IconData icon;
  final Color iconColor;
  final String title;
  final String subtitle;
  final Widget? action;
  const _Placeholder({
    required this.icon,
    required this.iconColor,
    required this.title,
    required this.subtitle,
    this.action,
  });

  @override
  Widget build(BuildContext context) => Center(
        child: Padding(
          padding: const EdgeInsets.all(40),
          child: Column(mainAxisSize: MainAxisSize.min, children: [
            Icon(icon, size: 60, color: iconColor.withValues(alpha: 0.35)),
            const SizedBox(height: 16),
            Text(title,
                style: const TextStyle(
                    fontSize: 17, fontWeight: FontWeight.w600),
                textAlign: TextAlign.center),
            const SizedBox(height: 8),
            Text(subtitle,
                style: TextStyle(
                    fontSize: 13,
                    color: Theme.of(context).colorScheme.onSurfaceVariant,
                    height: 1.5),
                textAlign: TextAlign.center),
            if (action != null) ...[
              const SizedBox(height: 20),
              action!,
            ],
          ]),
        ),
      );
}

// ── Filter chip ───────────────────────────────────────────────────────────────

class _FilterChip extends StatelessWidget {
  final String label;
  final bool selected;
  final VoidCallback onTap;
  const _FilterChip(this.label, this.selected, this.onTap);

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return GestureDetector(
      onTap: onTap,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 150),
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 6),
        decoration: BoxDecoration(
          color: selected ? cs.primary : cs.surfaceContainerLow,
          borderRadius: BorderRadius.circular(16),
        ),
        child: Text(label,
            style: TextStyle(
                fontSize: 12,
                fontWeight: FontWeight.w600,
                color: selected ? cs.onPrimary : cs.onSurfaceVariant)),
      ),
    );
  }
}

// ── Pager ─────────────────────────────────────────────────────────────────────

class _Pager extends StatelessWidget {
  final int current, total;
  final VoidCallback? onPrev, onNext;
  const _Pager(
      {required this.current,
      required this.total,
      this.onPrev,
      this.onNext});

  @override
  Widget build(BuildContext context) => Padding(
        padding: const EdgeInsets.symmetric(vertical: 12),
        child: Row(mainAxisAlignment: MainAxisAlignment.center, children: [
          IconButton(
              icon: const Icon(Icons.chevron_left), onPressed: onPrev),
          Text('${current + 1} / $total',
              style: const TextStyle(
                  fontSize: 13, fontWeight: FontWeight.w600)),
          IconButton(
              icon: const Icon(Icons.chevron_right), onPressed: onNext),
        ]),
      );
}
