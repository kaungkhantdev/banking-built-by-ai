import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/api/api_client.dart';
import '../../core/models/api_models.dart';
import '../../shared/widgets/empty_state.dart';
import '../../shared/widgets/status_badge.dart';

class StatementsScreen extends ConsumerStatefulWidget {
  const StatementsScreen({super.key});

  @override
  ConsumerState<StatementsScreen> createState() => _StatementsScreenState();
}

class _StatementsScreenState extends ConsumerState<StatementsScreen> {
  final _ctrl = TextEditingController();
  String? _accountId;
  bool _loading = false;
  List<StatementView> _items = [];

  static final _uuidRe = RegExp(
      r'^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$',
      caseSensitive: false);

  @override
  void dispose() { _ctrl.dispose(); super.dispose(); }

  Future<void> _load(String accountId) async {
    setState(() { _loading = true; _accountId = accountId; });
    try {
      final list = await ref.read(apiClientProvider).listStatements(accountId);
      if (mounted) setState(() { _items = list; _loading = false; });
    } catch (e) {
      if (mounted) {
        setState(() => _loading = false);
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('$e')));
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Statements')),
      body: Column(children: [

        // ── Account input ─────────────────────────────────────────────────
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 12, 16, 8),
          child: Row(children: [
            Expanded(
              child: TextField(
                controller: _ctrl,
                style: const TextStyle(fontFamily: 'monospace', fontSize: 12),
                decoration: const InputDecoration(
                  labelText: 'Account ID (UUID)',
                  prefixIcon: Icon(Icons.account_circle_outlined),
                  isDense: true,
                ),
              ),
            ),
            const SizedBox(width: 8),
            FilledButton(
              onPressed: _loading
                  ? null
                  : () {
                      final id = _ctrl.text.trim();
                      if (!_uuidRe.hasMatch(id)) {
                        ScaffoldMessenger.of(context).showSnackBar(
                            const SnackBar(
                                content: Text('Enter a valid account UUID')));
                        return;
                      }
                      _load(id);
                    },
              child: const Text('Load'),
            ),
          ]),
        ),

        // ── Request button ────────────────────────────────────────────────
        if (_accountId != null)
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
            child: OutlinedButton.icon(
              icon: const Icon(Icons.add, size: 18),
              label: const Text('Request Statement'),
              onPressed: () => _showRequestSheet(context),
            ),
          ),

        // ── List ─────────────────────────────────────────────────────────
        Expanded(
          child: _loading
              ? const Center(child: CircularProgressIndicator())
              : _accountId == null
                  ? const EmptyState(
                      icon: Icons.description_outlined,
                      title:'Enter your account ID to view statements')
                  : _items.isEmpty
                      ? const EmptyState(
                          icon: Icons.description_outlined,
                          title:'No statements yet')
                      : ListView.separated(
                          padding: const EdgeInsets.all(16),
                          itemCount: _items.length,
                          separatorBuilder: (_, __) =>
                              const SizedBox(height: 8),
                          itemBuilder: (ctx, i) {
                            final s = _items[i];
                            final months = [
                              '', 'Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun',
                              'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'
                            ];
                            return Card(
                              child: ListTile(
                                leading: const Icon(Icons.description_outlined,
                                    color: Color(0xFF4F46E5)),
                                title: Text(
                                  '${months[s.periodMonth]} ${s.periodYear}',
                                  style: const TextStyle(
                                      fontWeight: FontWeight.w600),
                                ),
                                subtitle: StatusBadge(label: s.status),
                                trailing: s.status == 'READY'
                                    ? const Icon(Icons.download_outlined,
                                        color: Color(0xFF4F46E5))
                                    : null,
                              ),
                            );
                          },
                        ),
        ),
      ]),
    );
  }

  void _showRequestSheet(BuildContext ctx) {
    final now = DateTime.now();
    int selYear  = now.year;
    int selMonth = now.month == 1 ? 12 : now.month - 1;
    if (now.month == 1) selYear--;

    showModalBottomSheet(
      context: ctx,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(
          borderRadius: BorderRadius.vertical(top: Radius.circular(24))),
      builder: (sheetCtx) => StatefulBuilder(
        builder: (_, setSheetState) => Padding(
          padding: EdgeInsets.fromLTRB(
              24, 24, 24, MediaQuery.viewInsetsOf(sheetCtx).bottom + 24),
          child: Column(mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.stretch, children: [
            const Text('Request Statement',
                style: TextStyle(fontSize: 18, fontWeight: FontWeight.w700)),
            const SizedBox(height: 20),
            Row(children: [
              Expanded(
                child: DropdownButtonFormField<int>(
                  value: selYear,
                  decoration: const InputDecoration(labelText: 'Year'),
                  items: List.generate(5, (i) => now.year - i)
                      .map((y) => DropdownMenuItem(value: y, child: Text('$y')))
                      .toList(),
                  onChanged: (v) { if (v != null) setSheetState(() => selYear = v); },
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: DropdownButtonFormField<int>(
                  value: selMonth,
                  decoration: const InputDecoration(labelText: 'Month'),
                  items: List.generate(12, (i) => i + 1)
                      .map((m) => DropdownMenuItem(value: m, child: Text('$m')))
                      .toList(),
                  onChanged: (v) { if (v != null) setSheetState(() => selMonth = v); },
                ),
              ),
            ]),
            const SizedBox(height: 20),
            FilledButton.icon(
              icon: const Icon(Icons.send_outlined),
              label: const Text('Request'),
              onPressed: () async {
                Navigator.pop(sheetCtx);
                try {
                  await ref.read(apiClientProvider)
                      .requestStatement(_accountId!, selYear, selMonth);
                  if (mounted) {
                    ScaffoldMessenger.of(context).showSnackBar(
                        const SnackBar(
                            content: Text('Statement requested — check back soon')));
                    _load(_accountId!);
                  }
                } catch (e) {
                  if (mounted) {
                    ScaffoldMessenger.of(context)
                        .showSnackBar(SnackBar(content: Text('$e')));
                  }
                }
              },
            ),
          ]),
        ),
      ),
    );
  }
}
