import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/api/api_client.dart';
import '../../core/models/api_models.dart';
import '../../shared/widgets/empty_state.dart';

class BeneficiariesScreen extends ConsumerStatefulWidget {
  const BeneficiariesScreen({super.key});

  @override
  ConsumerState<BeneficiariesScreen> createState() => _BeneficiariesScreenState();
}

class _BeneficiariesScreenState extends ConsumerState<BeneficiariesScreen> {
  bool _loading = true;
  List<BeneficiaryView> _items = [];

  @override
  void initState() { super.initState(); _load(); }

  Future<void> _load() async {
    setState(() => _loading = true);
    try {
      final p = await ref.read(apiClientProvider).listBeneficiaries();
      if (mounted) setState(() { _items = p.content; _loading = false; });
    } catch (_) {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _delete(BeneficiaryView b) async {
    try {
      await ref.read(apiClientProvider).deleteBeneficiary(b.id);
      if (mounted) {
        ScaffoldMessenger.of(context)
            .showSnackBar(SnackBar(content: Text('${b.alias} removed')));
        _load();
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('$e')));
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Beneficiaries')),
      floatingActionButton: FloatingActionButton.extended(
        icon: const Icon(Icons.person_add_outlined),
        label: const Text('Add'),
        onPressed: () => _showAddSheet(context),
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _items.isEmpty
              ? const EmptyState(
                  icon: Icons.people_outline,
                  title:'No saved recipients yet.\nTap + Add to save one.')
              : RefreshIndicator(
                  onRefresh: _load,
                  child: ListView.separated(
                    padding: const EdgeInsets.all(16),
                    itemCount: _items.length,
                    separatorBuilder: (_, __) => const SizedBox(height: 8),
                    itemBuilder: (ctx, i) {
                      final b = _items[i];
                      return Card(
                        child: ListTile(
                          leading: CircleAvatar(
                            backgroundColor:
                                Theme.of(ctx).colorScheme.primaryContainer,
                            child: Text(
                              b.alias.isNotEmpty
                                  ? b.alias[0].toUpperCase()
                                  : '?',
                              style: TextStyle(
                                  color: Theme.of(ctx)
                                      .colorScheme
                                      .onPrimaryContainer,
                                  fontWeight: FontWeight.w700),
                            ),
                          ),
                          title: Text(b.alias,
                              style: const TextStyle(fontWeight: FontWeight.w600)),
                          subtitle: Text(
                            b.destinationWalletId,
                            style: const TextStyle(
                                fontFamily: 'monospace', fontSize: 11),
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                          ),
                          trailing: IconButton(
                            icon: const Icon(Icons.delete_outline,
                                color: Color(0xFFDC2626)),
                            onPressed: () => _confirmDelete(ctx, b),
                          ),
                        ),
                      );
                    },
                  ),
                ),
    );
  }

  void _confirmDelete(BuildContext ctx, BeneficiaryView b) {
    showDialog(
      context: ctx,
      builder: (_) => AlertDialog(
        title: const Text('Remove beneficiary?'),
        content: Text('Remove "${b.alias}" from your saved recipients?'),
        actions: [
          TextButton(
              onPressed: () => Navigator.pop(ctx),
              child: const Text('Cancel')),
          TextButton(
              onPressed: () { Navigator.pop(ctx); _delete(b); },
              child: const Text('Remove',
                  style: TextStyle(color: Color(0xFFDC2626)))),
        ],
      ),
    );
  }

  void _showAddSheet(BuildContext ctx) => showModalBottomSheet(
        context: ctx,
        isScrollControlled: true,
        shape: const RoundedRectangleBorder(
            borderRadius: BorderRadius.vertical(top: Radius.circular(24))),
        builder: (_) => _AddBeneficiarySheet(
          api: ref.read(apiClientProvider),
          onAdded: _load,
        ),
      );
}

// ── Add sheet ─────────────────────────────────────────────────────────────────

class _AddBeneficiarySheet extends StatefulWidget {
  final ApiClient api;
  final VoidCallback onAdded;
  const _AddBeneficiarySheet({required this.api, required this.onAdded});

  @override
  State<_AddBeneficiarySheet> createState() => _AddBeneficiarySheetState();
}

class _AddBeneficiarySheetState extends State<_AddBeneficiarySheet> {
  final _form    = GlobalKey<FormState>();
  final _alias   = TextEditingController();
  final _walletId= TextEditingController();
  bool _loading  = false;

  static final _uuidRe = RegExp(
      r'^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$',
      caseSensitive: false);

  @override
  void dispose() { _alias.dispose(); _walletId.dispose(); super.dispose(); }

  Future<void> _submit() async {
    if (!_form.currentState!.validate()) return;
    setState(() => _loading = true);
    try {
      await widget.api.addBeneficiary(
          _alias.text.trim(), _walletId.text.trim());
      if (mounted) {
        Navigator.pop(context);
        widget.onAdded();
      }
    } catch (e) {
      if (mounted) {
        setState(() => _loading = false);
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('$e')));
      }
    }
  }

  @override
  Widget build(BuildContext context) => Padding(
        padding: EdgeInsets.fromLTRB(
            24, 24, 24, MediaQuery.viewInsetsOf(context).bottom + 24),
        child: Form(
          key: _form,
          child: Column(mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.stretch, children: [
            const Text('Add Beneficiary',
                style: TextStyle(fontSize: 18, fontWeight: FontWeight.w700)),
            const SizedBox(height: 20),
            TextFormField(
              controller: _alias,
              decoration: const InputDecoration(
                labelText: 'Name / Alias',
                prefixIcon: Icon(Icons.person_outline),
              ),
              validator: (v) =>
                  (v == null || v.trim().isEmpty) ? 'Name is required' : null,
            ),
            const SizedBox(height: 12),
            TextFormField(
              controller: _walletId,
              style: const TextStyle(fontFamily: 'monospace', fontSize: 13),
              decoration: const InputDecoration(
                labelText: 'Destination Wallet ID (UUID)',
                prefixIcon: Icon(Icons.wallet_outlined),
              ),
              validator: (v) => (v == null || !_uuidRe.hasMatch(v.trim()))
                  ? 'Enter a valid UUID' : null,
            ),
            const SizedBox(height: 20),
            FilledButton.icon(
              onPressed: _loading ? null : _submit,
              icon: _loading
                  ? const SizedBox(height: 18, width: 18,
                      child: CircularProgressIndicator(
                          strokeWidth: 2, color: Colors.white))
                  : const Icon(Icons.check),
              label: const Text('Save Beneficiary'),
            ),
          ]),
        ),
      );
}
