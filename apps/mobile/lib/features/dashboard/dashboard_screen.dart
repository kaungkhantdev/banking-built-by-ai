import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../auth/auth_provider.dart';  // for logout button

class DashboardScreen extends ConsumerWidget {
  const DashboardScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final cs = Theme.of(context).colorScheme;

    return Scaffold(
      appBar: AppBar(
        title: const Text('BankCore'),
        actions: [
          IconButton(
            icon: const Icon(Icons.logout),
            tooltip: 'Sign out',
            onPressed: () => ref.read(authProvider.notifier).logout(),
          ),
        ],
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // ── Welcome card ──────────────────────────────────────────────────
          Container(
            padding: const EdgeInsets.all(20),
            decoration: BoxDecoration(
              gradient: LinearGradient(
                colors: [cs.primary, cs.primary.withValues(alpha: 0.75)],
                begin: Alignment.topLeft,
                end: Alignment.bottomRight,
              ),
              borderRadius: BorderRadius.circular(16),
            ),
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Row(children: [
                Container(
                  width: 44, height: 44,
                  decoration: BoxDecoration(
                      color: Colors.white.withValues(alpha: 0.2),
                      borderRadius: BorderRadius.circular(12)),
                  child: Icon(Icons.account_balance,
                      color: cs.onPrimary, size: 24),
                ),
                const SizedBox(width: 12),
                Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                  Text('Welcome back',
                      style: TextStyle(color: cs.onPrimary.withValues(alpha: 0.8),
                          fontSize: 12)),
                  Text('BankCore Customer',
                      style: TextStyle(
                          color: cs.onPrimary, fontWeight: FontWeight.w700,
                          fontSize: 16)),
                ]),
              ]),
              const SizedBox(height: 20),
              Text('Digital Banking',
                  style: TextStyle(color: cs.onPrimary.withValues(alpha: 0.7),
                      fontSize: 12)),
              Text('Manage your accounts, wallets & transfers',
                  style: TextStyle(color: cs.onPrimary, fontSize: 13)),
            ]),
          ),

          const SizedBox(height: 24),
          Text('Quick actions',
              style: Theme.of(context).textTheme.labelMedium?.copyWith(
                  color: cs.onSurfaceVariant,
                  fontWeight: FontWeight.w600,
                  letterSpacing: 0.8)),
          const SizedBox(height: 12),

          // ── Quick action grid ─────────────────────────────────────────────
          GridView.count(
            crossAxisCount: 2,
            shrinkWrap: true,
            physics: const NeverScrollableScrollPhysics(),
            mainAxisSpacing: 12,
            crossAxisSpacing: 12,
            childAspectRatio: 1.3,
            children: [
              _QuickCard(
                icon: Icons.account_circle_outlined,
                label: 'Accounts',
                color: const Color(0xFF4F46E5),
                onTap: () => context.push('/accounts'),
              ),
              _QuickCard(
                icon: Icons.wallet_outlined,
                label: 'Wallets',
                color: const Color(0xFF10B981),
                onTap: () => context.go('/wallets'),
              ),
              _QuickCard(
                icon: Icons.send_outlined,
                label: 'Send Money',
                color: const Color(0xFF0EA5E9),
                onTap: () => context.go('/transfers'),
              ),
              _QuickCard(
                icon: Icons.verified_user_outlined,
                label: 'KYC Status',
                color: const Color(0xFFF59E0B),
                onTap: () => context.push('/kyc'),
              ),
            ],
          ),

          const SizedBox(height: 24),
          Text('About BankCore',
              style: Theme.of(context).textTheme.labelMedium?.copyWith(
                  color: cs.onSurfaceVariant,
                  fontWeight: FontWeight.w600,
                  letterSpacing: 0.8)),
          const SizedBox(height: 12),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(children: [
                _InfoRow(Icons.security_outlined, 'RS256 JWT authentication',
                    cs.onSurfaceVariant),
                const SizedBox(height: 10),
                _InfoRow(Icons.swap_vert_outlined, 'Double-entry ledger transfers',
                    cs.onSurfaceVariant),
                const SizedBox(height: 10),
                _InfoRow(Icons.document_scanner_outlined, 'KYC document verification',
                    cs.onSurfaceVariant),
              ]),
            ),
          ),
        ],
      ),
    );
  }
}

class _QuickCard extends StatelessWidget {
  final IconData icon;
  final String label;
  final Color color;
  final VoidCallback onTap;
  const _QuickCard({required this.icon, required this.label,
      required this.color, required this.onTap});

  @override
  Widget build(BuildContext context) => Card(
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(12),
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Container(
                  width: 48, height: 48,
                  decoration: BoxDecoration(
                      color: color.withValues(alpha: 0.12),
                      borderRadius: BorderRadius.circular(12)),
                  child: Icon(icon, color: color, size: 24),
                ),
                const SizedBox(height: 10),
                Text(label,
                    style: const TextStyle(
                        fontWeight: FontWeight.w600, fontSize: 13),
                    textAlign: TextAlign.center),
              ],
            ),
          ),
        ),
      );
}

class _InfoRow extends StatelessWidget {
  final IconData icon;
  final String text;
  final Color color;
  const _InfoRow(this.icon, this.text, this.color);

  @override
  Widget build(BuildContext context) => Row(children: [
        Icon(icon, size: 18, color: color),
        const SizedBox(width: 10),
        Expanded(
          child: Text(text,
              style: TextStyle(fontSize: 13, color: color)),
        ),
      ]);
}
