import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import '../../core/api/api_client.dart';
import '../../core/models/api_models.dart';
import '../../shared/widgets/empty_state.dart';
import '../../shared/widgets/status_badge.dart';

class ScheduledScreen extends ConsumerStatefulWidget {
  const ScheduledScreen({super.key});

  @override
  ConsumerState<ScheduledScreen> createState() => _ScheduledScreenState();
}

class _ScheduledScreenState extends ConsumerState<ScheduledScreen> {
  bool _loading = true;
  List<ScheduledTransferView> _items = [];
  final _fmt = NumberFormat('#,##0.00##');

  @override
  void initState() { super.initState(); _load(); }

  Future<void> _load() async {
    setState(() => _loading = true);
    try {
      final p = await ref.read(apiClientProvider).listScheduled();
      if (mounted) setState(() { _items = p.content; _loading = false; });
    } catch (_) {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _cancel(ScheduledTransferView s) async {
    try {
      await ref.read(apiClientProvider).cancelScheduled(s.id);
      if (mounted) {
        ScaffoldMessenger.of(context)
            .showSnackBar(const SnackBar(content: Text('Schedule cancelled')));
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
      appBar: AppBar(title: const Text('Scheduled Transfers')),
      floatingActionButton: FloatingActionButton.extended(
        icon: const Icon(Icons.add),
        label: const Text('Schedule'),
        onPressed: () => _showScheduleSheet(context),
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _items.isEmpty
              ? const EmptyState(
                  icon: Icons.event_repeat_outlined,
                  title:'No scheduled transfers.\nTap + Schedule to create one.')
              : RefreshIndicator(
                  onRefresh: _load,
                  child: ListView.separated(
                    padding:
                        const EdgeInsets.fromLTRB(16, 16, 16, 88),
                    itemCount: _items.length,
                    separatorBuilder: (_, __) => const SizedBox(height: 8),
                    itemBuilder: (ctx, i) {
                      final s = _items[i];
                      final active = s.status == 'ACTIVE';
                      return Card(
                        child: ListTile(
                          leading: CircleAvatar(
                            backgroundColor: active
                                ? const Color(0xFFD1FAE5)
                                : const Color(0xFFF3F4F6),
                            child: Icon(
                              Icons.event_repeat,
                              size: 20,
                              color: active
                                  ? const Color(0xFF059669)
                                  : const Color(0xFF6B7280),
                            ),
                          ),
                          title: Text(
                            '${_fmt.format(s.amount)}',
                            style: const TextStyle(fontWeight: FontWeight.w600),
                          ),
                          subtitle: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(s.recurrenceRule ?? 'ONCE',
                                    style: const TextStyle(fontSize: 12)),
                                if (s.nextRunAt != null)
                                  Text('Next: ${_fmtDate(s.nextRunAt!)}',
                                      style: const TextStyle(fontSize: 11)),
                              ]),
                          trailing: active
                              ? TextButton(
                                  onPressed: () => _confirmCancel(ctx, s),
                                  child: const Text('Cancel',
                                      style: TextStyle(
                                          color: Color(0xFFDC2626),
                                          fontSize: 12)),
                                )
                              : StatusBadge(label: s.status),
                          isThreeLine: true,
                        ),
                      );
                    },
                  ),
                ),
    );
  }

  void _confirmCancel(BuildContext ctx, ScheduledTransferView s) {
    showDialog(
      context: ctx,
      builder: (_) => AlertDialog(
        title: const Text('Cancel schedule?'),
        content: const Text('This scheduled transfer will be stopped.'),
        actions: [
          TextButton(
              onPressed: () => Navigator.pop(ctx),
              child: const Text('Keep')),
          TextButton(
              onPressed: () { Navigator.pop(ctx); _cancel(s); },
              child: const Text('Cancel it',
                  style: TextStyle(color: Color(0xFFDC2626)))),
        ],
      ),
    );
  }

  void _showScheduleSheet(BuildContext ctx) => showModalBottomSheet(
        context: ctx,
        isScrollControlled: true,
        shape: const RoundedRectangleBorder(
            borderRadius: BorderRadius.vertical(top: Radius.circular(24))),
        builder: (_) =>
            _ScheduleSheet(api: ref.read(apiClientProvider), onCreated: _load),
      );

  String _fmtDate(String iso) {
    try {
      return DateFormat('dd MMM yyyy, HH:mm')
          .format(DateTime.parse(iso).toLocal());
    } catch (_) {
      return iso.substring(0, 10);
    }
  }
}

// ── Schedule sheet ────────────────────────────────────────────────────────────

class _ScheduleSheet extends StatefulWidget {
  final ApiClient api;
  final VoidCallback onCreated;
  const _ScheduleSheet({required this.api, required this.onCreated});

  @override
  State<_ScheduleSheet> createState() => _ScheduleSheetState();
}

class _ScheduleSheetState extends State<_ScheduleSheet> {
  final _form   = GlobalKey<FormState>();
  final _from   = TextEditingController();
  final _to     = TextEditingController();
  final _amount = TextEditingController();
  final _memo   = TextEditingController();
  String _recurrence = 'ONCE';
  DateTime _runAt = DateTime.now().add(const Duration(hours: 1));
  bool _loading = false;

  static final _uuidRe = RegExp(
      r'^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$',
      caseSensitive: false);

  @override
  void dispose() {
    _from.dispose(); _to.dispose(); _amount.dispose(); _memo.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_form.currentState!.validate()) return;
    setState(() => _loading = true);
    try {
      await widget.api.scheduleTransfer(
        fromWalletId: _from.text.trim(),
        toWalletId:   _to.text.trim(),
        amount:       double.parse(_amount.text.trim()),
        memo:         _memo.text.trim().isEmpty ? null : _memo.text.trim(),
        recurrenceRule: _recurrence == 'ONCE' ? null : _recurrence,
        runAt: _runAt,
      );
      if (mounted) { Navigator.pop(context); widget.onCreated(); }
    } catch (e) {
      if (mounted) {
        setState(() => _loading = false);
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('$e')));
      }
    }
  }

  @override
  Widget build(BuildContext context) => SingleChildScrollView(
        padding: EdgeInsets.fromLTRB(
            24, 24, 24, MediaQuery.viewInsetsOf(context).bottom + 24),
        child: Form(
          key: _form,
          child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
            const Text('Schedule Transfer',
                style: TextStyle(fontSize: 18, fontWeight: FontWeight.w700)),
            const SizedBox(height: 20),
            TextFormField(
              controller: _from,
              style: const TextStyle(fontFamily: 'monospace', fontSize: 12),
              decoration: const InputDecoration(
                labelText: 'From Wallet ID',
                prefixIcon: Icon(Icons.wallet_outlined),
              ),
              validator: (v) => (v == null || !_uuidRe.hasMatch(v.trim()))
                  ? 'Valid UUID required' : null,
            ),
            const SizedBox(height: 12),
            TextFormField(
              controller: _to,
              style: const TextStyle(fontFamily: 'monospace', fontSize: 12),
              decoration: const InputDecoration(
                labelText: 'To Wallet ID',
                prefixIcon: Icon(Icons.send_outlined),
              ),
              validator: (v) => (v == null || !_uuidRe.hasMatch(v.trim()))
                  ? 'Valid UUID required' : null,
            ),
            const SizedBox(height: 12),
            TextFormField(
              controller: _amount,
              keyboardType:
                  const TextInputType.numberWithOptions(decimal: true),
              decoration: const InputDecoration(
                labelText: 'Amount',
                prefixIcon: Icon(Icons.attach_money),
              ),
              validator: (v) {
                final d = double.tryParse(v ?? '');
                return (d == null || d <= 0) ? 'Enter a positive amount' : null;
              },
            ),
            const SizedBox(height: 12),
            TextFormField(
              controller: _memo,
              decoration: const InputDecoration(
                labelText: 'Memo (optional)',
                prefixIcon: Icon(Icons.notes_outlined),
              ),
            ),
            const SizedBox(height: 12),
            DropdownButtonFormField<String>(
              value: _recurrence,
              decoration: const InputDecoration(labelText: 'Recurrence'),
              items: const [
                DropdownMenuItem(value: 'ONCE',    child: Text('Once')),
                DropdownMenuItem(value: 'DAILY',   child: Text('Daily')),
                DropdownMenuItem(value: 'WEEKLY',  child: Text('Weekly')),
                DropdownMenuItem(value: 'MONTHLY', child: Text('Monthly')),
              ],
              onChanged: (v) { if (v != null) setState(() => _recurrence = v); },
            ),
            const SizedBox(height: 12),
            ListTile(
              contentPadding: EdgeInsets.zero,
              leading: const Icon(Icons.schedule_outlined),
              title: Text('Run at: ${DateFormat('dd MMM yyyy, HH:mm').format(_runAt)}'),
              trailing: const Icon(Icons.edit_outlined),
              onTap: () async {
                final date = await showDatePicker(
                  context: context,
                  initialDate: _runAt,
                  firstDate: DateTime.now(),
                  lastDate: DateTime.now().add(const Duration(days: 365)),
                );
                if (date != null && mounted) {
                  final time = await showTimePicker(
                      context: context,
                      initialTime: TimeOfDay.fromDateTime(_runAt));
                  if (time != null && mounted) {
                    setState(() => _runAt = DateTime(
                        date.year, date.month, date.day,
                        time.hour, time.minute));
                  }
                }
              },
            ),
            const SizedBox(height: 20),
            FilledButton.icon(
              onPressed: _loading ? null : _submit,
              icon: _loading
                  ? const SizedBox(height: 18, width: 18,
                      child: CircularProgressIndicator(
                          strokeWidth: 2, color: Colors.white))
                  : const Icon(Icons.schedule),
              label: const Text('Schedule Transfer'),
            ),
          ]),
        ),
      );
}
