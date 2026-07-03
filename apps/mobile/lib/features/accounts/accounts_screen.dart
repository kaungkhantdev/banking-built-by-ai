import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/api/api_client.dart';
import '../../core/models/api_models.dart';
import '../../shared/widgets/status_badge.dart';
import '../../shared/widgets/empty_state.dart';

class AccountsScreen extends ConsumerStatefulWidget {
  const AccountsScreen({super.key});

  @override
  ConsumerState<AccountsScreen> createState() => _AccountsScreenState();
}

class _AccountsScreenState extends ConsumerState<AccountsScreen> {
  List<AccountListItem> _items = [];
  int _page = 0;
  int _totalPages = 0;
  bool _loading = true;
  String? _error;

  @override
  void initState() { super.initState(); _load(); }

  Future<void> _load({int page = 0}) async {
    setState(() { _loading = true; _error = null; });
    try {
      final p = await ref.read(apiClientProvider).listAccounts(page: page);
      if (mounted) setState(() {
        _items      = p.content;
        _page       = p.number;
        _totalPages = p.totalPages;
        _loading    = false;
      });
    } catch (e) {
      if (mounted) setState(() { _error = e.toString(); _loading = false; });
    }
  }

  void _showCreateSheet() {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(
          borderRadius: BorderRadius.vertical(top: Radius.circular(24))),
      builder: (_) => _CreateAccountSheet(
        onCreated: () { Navigator.pop(context); _load(); },
        api: ref.read(apiClientProvider),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Accounts'),
        actions: [IconButton(icon: const Icon(Icons.refresh), onPressed: _load)],
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: _showCreateSheet,
        icon: const Icon(Icons.add),
        label: const Text('New'),
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? EmptyState(icon: Icons.error_outline, title: 'Error', subtitle: _error)
              : _items.isEmpty
                  ? EmptyState(
                      icon: Icons.account_circle_outlined,
                      title: 'No accounts',
                      subtitle: 'Tap + to open a new account',
                    )
                  : RefreshIndicator(
                      onRefresh: () => _load(page: _page),
                      child: ListView.separated(
                        padding: const EdgeInsets.fromLTRB(16, 8, 16, 96),
                        itemCount: _items.length + (_totalPages > 1 ? 1 : 0),
                        separatorBuilder: (_, __) => const SizedBox(height: 8),
                        itemBuilder: (context, i) {
                          if (i == _items.length) {
                            return _PaginationRow(
                              page: _page,
                              totalPages: _totalPages,
                              onPrev: () => _load(page: _page - 1),
                              onNext: () => _load(page: _page + 1),
                            );
                          }
                          return _AccountCard(
                            item: _items[i],
                            onActivate: () => _activate(_items[i]),
                          );
                        },
                      ),
                    ),
    );
  }

  Future<void> _activate(AccountListItem acc) async {
    try {
      await ref.read(apiClientProvider).activateAccount(acc.id);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Account activated')));
        _load(page: _page);
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context)
            .showSnackBar(SnackBar(content: Text('Error: $e')));
      }
    }
  }
}

class _AccountCard extends StatelessWidget {
  final AccountListItem item;
  final VoidCallback onActivate;
  const _AccountCard({required this.item, required this.onActivate});

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Row(children: [
            Expanded(
              child: Text(item.ownerEmail,
                  style: const TextStyle(fontWeight: FontWeight.w600)),
            ),
            StatusBadge.forAccountStatus(item.status),
          ]),
          const SizedBox(height: 6),
          Text('ID: ${item.id.substring(0, 18)}…',
              style: TextStyle(fontSize: 11, color: cs.onSurfaceVariant,
                  fontFamily: 'monospace')),
          const SizedBox(height: 12),
          Row(children: [
            Icon(Icons.calendar_today_outlined, size: 12, color: cs.onSurfaceVariant),
            const SizedBox(width: 4),
            Text(item.createdAt.substring(0, 10),
                style: TextStyle(fontSize: 12, color: cs.onSurfaceVariant)),
            const Spacer(),
            if (item.status == 'PENDING')
              FilledButton.tonalIcon(
                onPressed: onActivate,
                icon: const Icon(Icons.check, size: 14),
                label: const Text('Activate', style: TextStyle(fontSize: 12)),
                style: FilledButton.styleFrom(
                    minimumSize: const Size(0, 32),
                    padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4)),
              ),
          ]),
        ]),
      ),
    );
  }
}

class _CreateAccountSheet extends StatefulWidget {
  final VoidCallback onCreated;
  final ApiClient api;
  const _CreateAccountSheet({required this.onCreated, required this.api});

  @override
  State<_CreateAccountSheet> createState() => _CreateAccountSheetState();
}

class _CreateAccountSheetState extends State<_CreateAccountSheet> {
  final _ctrl    = TextEditingController();
  final _form    = GlobalKey<FormState>();
  bool _loading  = false;

  static final _uuidRe = RegExp(
      r'^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$',
      caseSensitive: false);

  @override
  void dispose() { _ctrl.dispose(); super.dispose(); }

  Future<void> _submit() async {
    if (!_form.currentState!.validate()) return;
    setState(() => _loading = true);
    try {
      await widget.api.openAccount(_ctrl.text.trim());
      widget.onCreated();
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context)
            .showSnackBar(SnackBar(content: Text('Error: $e')));
        setState(() => _loading = false);
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
        child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.stretch, children: [
          const Text('Open New Account',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.w700)),
          const SizedBox(height: 20),
          TextFormField(
            controller: _ctrl,
            decoration: const InputDecoration(
              labelText: 'Owner User ID (UUID)',
              hintText: 'xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx',
              prefixIcon: Icon(Icons.person_outline),
            ),
            style: const TextStyle(fontFamily: 'monospace', fontSize: 13),
            validator: (v) => (v == null || !_uuidRe.hasMatch(v.trim()))
                ? 'Enter a valid UUID'
                : null,
          ),
          const SizedBox(height: 20),
          FilledButton(
            onPressed: _loading ? null : _submit,
            child: _loading
                ? const SizedBox(height: 20, width: 20,
                    child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                : const Text('Open Account'),
          ),
        ]),
      ),
    );
  }
}

class _PaginationRow extends StatelessWidget {
  final int page, totalPages;
  final VoidCallback onPrev, onNext;
  const _PaginationRow({required this.page, required this.totalPages,
      required this.onPrev, required this.onNext});

  @override
  Widget build(BuildContext context) => Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          OutlinedButton(onPressed: page > 0 ? onPrev : null, child: const Text('Prev')),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            child: Text('${page + 1} / $totalPages'),
          ),
          OutlinedButton(
              onPressed: page + 1 < totalPages ? onNext : null,
              child: const Text('Next')),
        ],
      );
}
