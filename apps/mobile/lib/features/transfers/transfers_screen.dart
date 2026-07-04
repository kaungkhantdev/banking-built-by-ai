import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/api/api_client.dart';
import '../../core/models/api_models.dart';
import '../../shared/widgets/wallet_picker.dart';

class TransfersScreen extends ConsumerStatefulWidget {
  const TransfersScreen({super.key});

  @override
  ConsumerState<TransfersScreen> createState() => _TransfersScreenState();
}

class _TransfersScreenState extends ConsumerState<TransfersScreen> {
  final _form   = GlobalKey<FormState>();
  final _to     = TextEditingController();
  final _amount = TextEditingController();
  final _memo   = TextEditingController();
  bool _loading = false;
  TransferResult? _result;

  // Source wallet: chosen from the customer's own wallets.
  String? _fromWalletId;

  // Destination selection: pay a saved beneficiary or a raw wallet id.
  String _destMode = 'beneficiary'; // 'beneficiary' | 'wallet'
  bool _loadingBenef = true;
  List<BeneficiaryView> _beneficiaries = [];
  String? _selectedBeneficiaryId;

  static final _uuidRe = RegExp(
      r'^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$',
      caseSensitive: false);

  @override
  void initState() {
    super.initState();
    _loadBeneficiaries();
  }

  @override
  void dispose() {
    _to.dispose(); _amount.dispose(); _memo.dispose();
    super.dispose();
  }

  Future<void> _loadBeneficiaries() async {
    try {
      final p = await ref.read(apiClientProvider).listBeneficiaries();
      if (!mounted) return;
      setState(() {
        _beneficiaries = p.content;
        _loadingBenef = false;
        // If the customer has no saved payees, default to wallet-id entry.
        if (_beneficiaries.isEmpty) _destMode = 'wallet';
      });
    } catch (_) {
      if (mounted) setState(() { _loadingBenef = false; _destMode = 'wallet'; });
    }
  }

  Future<void> _submit() async {
    if (!_form.currentState!.validate()) return;
    if (_destMode == 'beneficiary' && _selectedBeneficiaryId == null) {
      ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Choose a payee')));
      return;
    }
    setState(() { _loading = true; _result = null; });
    try {
      final r = await ref.read(apiClientProvider).transfer(
        fromWalletId:  _fromWalletId!,
        toWalletId:    _destMode == 'wallet' ? _to.text.trim() : null,
        beneficiaryId: _destMode == 'beneficiary' ? _selectedBeneficiaryId : null,
        amount:        double.parse(_amount.text.trim()),
        memo:          _memo.text.trim().isEmpty ? null : _memo.text.trim(),
      );
      if (mounted) setState(() { _result = r; _loading = false; });
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
      appBar: AppBar(title: const Text('Send Money')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Form(
          key: _form,
          child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
            WalletPicker(
              label: 'From wallet',
              value: _fromWalletId,
              validator: (v) => v == null ? 'Choose a wallet' : null,
              onChanged: (v) => setState(() => _fromWalletId = v),
            ),
            const SizedBox(height: 16),

            // ── Destination mode toggle ──────────────────────────────────
            SegmentedButton<String>(
              segments: const [
                ButtonSegment(value: 'beneficiary',
                    label: Text('Saved payee'), icon: Icon(Icons.contacts_outlined)),
                ButtonSegment(value: 'wallet',
                    label: Text('Wallet ID'), icon: Icon(Icons.wallet)),
              ],
              selected: {_destMode},
              onSelectionChanged: (s) => setState(() => _destMode = s.first),
            ),
            const SizedBox(height: 12),

            if (_destMode == 'beneficiary') _buildBeneficiaryPicker()
            else _UuidField(_to, 'To Wallet ID', Icons.wallet, _uuidRe),

            const SizedBox(height: 12),
            TextFormField(
              controller: _amount,
              keyboardType: const TextInputType.numberWithOptions(decimal: true),
              decoration: const InputDecoration(
                labelText: 'Amount',
                prefixIcon: Icon(Icons.attach_money),
              ),
              validator: (v) {
                if (v == null || v.isEmpty) return 'Amount required';
                final n = double.tryParse(v);
                if (n == null || n <= 0) return 'Enter a positive number';
                return null;
              },
            ),
            const SizedBox(height: 12),
            TextFormField(
              controller: _memo,
              decoration: const InputDecoration(
                labelText: 'Note (optional)',
                prefixIcon: Icon(Icons.notes_outlined),
              ),
            ),
            const SizedBox(height: 24),
            FilledButton.icon(
              onPressed: _loading ? null : _submit,
              icon: _loading
                  ? const SizedBox(height: 18, width: 18,
                      child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                  : const Icon(Icons.send),
              label: const Text('Send Money'),
            ),
            if (_result != null) ...[
              const SizedBox(height: 20),
              _SuccessCard(result: _result!),
            ],
          ]),
        ),
      ),
    );
  }

  Widget _buildBeneficiaryPicker() {
    final cs = Theme.of(context).colorScheme;
    if (_loadingBenef) {
      return const Padding(
        padding: EdgeInsets.symmetric(vertical: 12),
        child: Center(child: CircularProgressIndicator()),
      );
    }
    if (_beneficiaries.isEmpty) {
      return Container(
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          color: cs.surfaceContainerLow,
          borderRadius: BorderRadius.circular(10),
        ),
        child: Row(children: [
          Icon(Icons.info_outline, size: 18, color: cs.onSurfaceVariant),
          const SizedBox(width: 8),
          Expanded(
            child: Text(
              'No saved payees yet. Add one on the Beneficiaries screen, '
              'or switch to “Wallet ID”.',
              style: TextStyle(fontSize: 12, color: cs.onSurfaceVariant),
            ),
          ),
        ]),
      );
    }
    return DropdownButtonFormField<String>(
      initialValue: _selectedBeneficiaryId,
      isExpanded: true,
      decoration: const InputDecoration(
        labelText: 'Pay to',
        prefixIcon: Icon(Icons.contacts_outlined),
      ),
      items: _beneficiaries
          .map((b) => DropdownMenuItem(
                value: b.id,
                child: Text('${b.alias} · ${b.destinationWalletId.substring(0, 8)}…',
                    overflow: TextOverflow.ellipsis),
              ))
          .toList(),
      validator: (v) => v == null ? 'Choose a payee' : null,
      onChanged: (v) => setState(() => _selectedBeneficiaryId = v),
    );
  }
}

class _UuidField extends StatelessWidget {
  final TextEditingController ctrl;
  final String label;
  final IconData icon;
  final RegExp? pattern;

  static final _defaultRe = RegExp(
      r'^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$',
      caseSensitive: false);

  const _UuidField(this.ctrl, this.label, this.icon, [this.pattern]);

  @override
  Widget build(BuildContext context) => TextFormField(
        controller: ctrl,
        style: const TextStyle(fontFamily: 'monospace', fontSize: 13),
        decoration: InputDecoration(
          labelText: label,
          prefixIcon: Icon(icon),
          hintText: 'xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx',
        ),
        validator: (v) => (v == null || !(pattern ?? _defaultRe).hasMatch(v.trim()))
            ? 'Enter a valid UUID'
            : null,
      );
}

class _SuccessCard extends StatelessWidget {
  final TransferResult result;
  const _SuccessCard({required this.result});

  @override
  Widget build(BuildContext context) => Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: const Color(0xFFECFDF5),
          borderRadius: BorderRadius.circular(12),
          border: Border.all(color: const Color(0xFF6EE7B7)),
        ),
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Row(children: [
            const Icon(Icons.check_circle, color: Color(0xFF059669), size: 18),
            const SizedBox(width: 8),
            Text(
              result.replayed ? 'Already processed' : 'Transfer sent!',
              style: const TextStyle(
                  fontWeight: FontWeight.w700, color: Color(0xFF065F46)),
            ),
          ]),
          const SizedBox(height: 6),
          Text('Status: ${result.status}',
              style: const TextStyle(color: Color(0xFF065F46), fontSize: 13)),
          const SizedBox(height: 2),
          Text('Ref: ${result.transactionId}',
              style: const TextStyle(
                  fontFamily: 'monospace', fontSize: 10, color: Color(0xFF065F46))),
        ]),
      );
}
