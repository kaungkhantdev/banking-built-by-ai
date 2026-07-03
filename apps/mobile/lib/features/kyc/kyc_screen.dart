import 'dart:io';
import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/api/api_client.dart';
import '../../core/models/api_models.dart';
import '../../shared/widgets/status_badge.dart';
import '../../shared/widgets/empty_state.dart';

const _kStatuses = ['', 'CREATED', 'DOCS_SUBMITTED', 'UNDER_REVIEW', 'VERIFIED', 'REJECTED'];

class KycScreen extends ConsumerStatefulWidget {
  const KycScreen({super.key});

  @override
  ConsumerState<KycScreen> createState() => _KycScreenState();
}

class _KycScreenState extends ConsumerState<KycScreen> {
  List<KycCaseView> _items = [];
  int _page = 0, _totalPages = 0;
  String? _statusFilter;
  bool _loading = true;

  @override
  void initState() { super.initState(); _load(); }

  Future<void> _load({int page = 0}) async {
    setState(() => _loading = true);
    try {
      final p = await ref.read(apiClientProvider)
          .listKyc(status: _statusFilter, page: page);
      if (mounted) setState(() {
        _items      = p.content;
        _page       = p.number;
        _totalPages = p.totalPages;
        _loading    = false;
      });
    } catch (_) {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('KYC Cases'),
        actions: [
          IconButton(icon: const Icon(Icons.refresh), onPressed: _load),
          IconButton(
            icon: const Icon(Icons.add),
            onPressed: _showOpenCaseSheet,
            tooltip: 'Open KYC Case',
          ),
        ],
        bottom: PreferredSize(
          preferredSize: const Size.fromHeight(52),
          child: SizedBox(
            height: 52,
            child: ListView(
              scrollDirection: Axis.horizontal,
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
              children: _kStatuses.map((s) => Padding(
                padding: const EdgeInsets.only(right: 8),
                child: FilterChip(
                  label: Text(s.isEmpty ? 'All' : s),
                  selected: _statusFilter == (s.isEmpty ? null : s),
                  onSelected: (_) {
                    setState(() => _statusFilter = s.isEmpty ? null : s);
                    _load();
                  },
                ),
              )).toList(),
            ),
          ),
        ),
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _items.isEmpty
              ? const EmptyState(
                  icon: Icons.verified_user_outlined,
                  title: 'No KYC cases',
                  subtitle: 'Tap + to open a new case',
                )
              : RefreshIndicator(
                  onRefresh: _load,
                  child: ListView.separated(
                    padding: const EdgeInsets.fromLTRB(16, 8, 16, 24),
                    itemCount: _items.length + (_totalPages > 1 ? 1 : 0),
                    separatorBuilder: (_, __) => const SizedBox(height: 8),
                    itemBuilder: (context, i) {
                      if (i == _items.length) {
                        return _Pagination(
                          page: _page, total: _totalPages,
                          onPrev: () => _load(page: _page - 1),
                          onNext: () => _load(page: _page + 1),
                        );
                      }
                      return _KycCard(
                        item: _items[i],
                        onUpload: () => _showUploadSheet(_items[i].accountId),
                      );
                    },
                  ),
                ),
    );
  }

  void _showOpenCaseSheet() {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(
          borderRadius: BorderRadius.vertical(top: Radius.circular(24))),
      builder: (_) => _OpenCaseSheet(
        api: ref.read(apiClientProvider),
        onCreated: () { Navigator.pop(context); _load(); },
      ),
    );
  }

  void _showUploadSheet(String accountId) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(
          borderRadius: BorderRadius.vertical(top: Radius.circular(24))),
      builder: (_) => _UploadDocSheet(
        accountId: accountId,
        api: ref.read(apiClientProvider),
        onUploaded: () { Navigator.pop(context); _load(); },
      ),
    );
  }
}

class _KycCard extends StatelessWidget {
  final KycCaseView item;
  final VoidCallback onUpload;
  const _KycCard({required this.item, required this.onUpload});

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Row(children: [
            Expanded(
              child: Text('Case ${item.id.substring(0, 8)}…',
                  style: const TextStyle(
                      fontWeight: FontWeight.w600, fontFamily: 'monospace',
                      fontSize: 13)),
            ),
            StatusBadge.forKycStatus(item.status),
          ]),
          const SizedBox(height: 6),
          Text('Account: ${item.accountId.substring(0, 18)}…',
              style: TextStyle(fontSize: 11, color: cs.onSurfaceVariant,
                  fontFamily: 'monospace')),
          if (item.vendorRef != null) ...[
            const SizedBox(height: 4),
            Text('Vendor ref: ${item.vendorRef}',
                style: TextStyle(fontSize: 11, color: cs.onSurfaceVariant)),
          ],
          if (item.rejectReason != null) ...[
            const SizedBox(height: 4),
            Text('Rejected: ${item.rejectReason}',
                style: const TextStyle(fontSize: 11, color: Color(0xFFDC2626))),
          ],
          const SizedBox(height: 10),
          Row(children: [
            Text(item.updatedAt.substring(0, 10),
                style: TextStyle(fontSize: 11, color: cs.onSurfaceVariant)),
            const Spacer(),
            if (item.status == 'CREATED' || item.status == 'DOCS_SUBMITTED')
              TextButton.icon(
                onPressed: onUpload,
                icon: const Icon(Icons.upload_file, size: 14),
                label: const Text('Upload', style: TextStyle(fontSize: 12)),
                style: TextButton.styleFrom(
                    minimumSize: const Size(0, 28),
                    padding: const EdgeInsets.symmetric(horizontal: 8)),
              ),
          ]),
        ]),
      ),
    );
  }
}

class _OpenCaseSheet extends StatefulWidget {
  final ApiClient api;
  final VoidCallback onCreated;
  const _OpenCaseSheet({required this.api, required this.onCreated});

  @override
  State<_OpenCaseSheet> createState() => _OpenCaseSheetState();
}

class _OpenCaseSheetState extends State<_OpenCaseSheet> {
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
      await widget.api.openKycCase(_ctrl.text.trim());
      widget.onCreated();
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
        child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.stretch, children: [
          const Text('Open KYC Case',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.w700)),
          const SizedBox(height: 20),
          TextFormField(
            controller: _ctrl,
            style: const TextStyle(fontFamily: 'monospace', fontSize: 13),
            decoration: const InputDecoration(
              labelText: 'Account ID (UUID)',
              prefixIcon: Icon(Icons.account_circle_outlined),
            ),
            validator: (v) => (v == null || !_uuidRe.hasMatch(v.trim()))
                ? 'Enter a valid UUID' : null,
          ),
          const SizedBox(height: 20),
          FilledButton(
            onPressed: _loading ? null : _submit,
            child: _loading
                ? const SizedBox(height: 20, width: 20,
                    child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                : const Text('Open Case'),
          ),
        ]),
      ),
    );
  }
}

class _UploadDocSheet extends StatefulWidget {
  final String accountId;
  final ApiClient api;
  final VoidCallback onUploaded;
  const _UploadDocSheet(
      {required this.accountId, required this.api, required this.onUploaded});

  @override
  State<_UploadDocSheet> createState() => _UploadDocSheetState();
}

class _UploadDocSheetState extends State<_UploadDocSheet> {
  File? _file;
  bool _loading = false;

  Future<void> _pick() async {
    final result = await FilePicker.platform.pickFiles(
      type: FileType.custom,
      allowedExtensions: ['pdf', 'png', 'jpg', 'jpeg'],
    );
    if (result != null && result.files.single.path != null) {
      setState(() => _file = File(result.files.single.path!));
    }
  }

  Future<void> _upload() async {
    if (_file == null) return;
    setState(() => _loading = true);
    try {
      await widget.api.submitKycDocument(widget.accountId, _file!);
      widget.onUploaded();
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
      child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.stretch, children: [
        const Text('Upload KYC Document',
            style: TextStyle(fontSize: 18, fontWeight: FontWeight.w700)),
        const SizedBox(height: 6),
        Text('Account: ${widget.accountId.substring(0, 18)}…',
            style: TextStyle(fontSize: 12, color: cs.onSurfaceVariant,
                fontFamily: 'monospace')),
        const SizedBox(height: 20),
        InkWell(
          onTap: _pick,
          borderRadius: BorderRadius.circular(12),
          child: Container(
            padding: const EdgeInsets.all(24),
            decoration: BoxDecoration(
              border: Border.all(
                  color: _file != null ? cs.primary : cs.outlineVariant,
                  width: 2,
                  style: BorderStyle.solid),
              borderRadius: BorderRadius.circular(12),
              color: _file != null
                  ? cs.primaryContainer.withValues(alpha: 0.3)
                  : null,
            ),
            child: Column(children: [
              Icon(
                _file != null ? Icons.check_circle : Icons.upload_file,
                size: 40,
                color: _file != null ? cs.primary : cs.onSurfaceVariant,
              ),
              const SizedBox(height: 8),
              Text(
                _file != null
                    ? _file!.path.split('/').last
                    : 'Tap to select file\nPDF, PNG, JPG',
                textAlign: TextAlign.center,
                style: TextStyle(
                    color: _file != null ? cs.primary : cs.onSurfaceVariant,
                    fontSize: 13),
              ),
            ]),
          ),
        ),
        const SizedBox(height: 20),
        FilledButton.icon(
          onPressed: (_file == null || _loading) ? null : _upload,
          icon: _loading
              ? const SizedBox(height: 18, width: 18,
                  child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
              : const Icon(Icons.upload),
          label: const Text('Upload Document'),
        ),
      ]),
    );
  }
}

class _Pagination extends StatelessWidget {
  final int page, total;
  final VoidCallback onPrev, onNext;
  const _Pagination({required this.page, required this.total,
      required this.onPrev, required this.onNext});

  @override
  Widget build(BuildContext context) => Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          OutlinedButton(onPressed: page > 0 ? onPrev : null, child: const Text('Prev')),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            child: Text('${page + 1} / $total'),
          ),
          OutlinedButton(onPressed: page + 1 < total ? onNext : null, child: const Text('Next')),
        ],
      );
}
