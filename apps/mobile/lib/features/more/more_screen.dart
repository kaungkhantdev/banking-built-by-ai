import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

class MoreScreen extends StatelessWidget {
  const MoreScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('More')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [

          _SectionHeader('Finances'),
          _MoreTile(
            icon: Icons.receipt_long_outlined,
            color: const Color(0xFF0EA5E9),
            title: 'Transaction History',
            subtitle: 'View wallet transactions',
            onTap: () => context.push('/transactions'),
          ),
          _MoreTile(
            icon: Icons.event_repeat_outlined,
            color: const Color(0xFF8B5CF6),
            title: 'Scheduled Transfers',
            subtitle: 'Manage recurring payments',
            onTap: () => context.push('/scheduled'),
          ),
          _MoreTile(
            icon: Icons.description_outlined,
            color: const Color(0xFF10B981),
            title: 'Statements',
            subtitle: 'Monthly account statements',
            onTap: () => context.push('/statements'),
          ),
          _MoreTile(
            icon: Icons.currency_exchange_outlined,
            color: const Color(0xFFF59E0B),
            title: 'Exchange Rates',
            subtitle: 'Live currency rates',
            onTap: () => context.push('/exchange'),
          ),

          const SizedBox(height: 16),
          _SectionHeader('Account'),
          _MoreTile(
            icon: Icons.verified_user_outlined,
            color: const Color(0xFF4F46E5),
            title: 'KYC Verification',
            subtitle: 'Identity verification status',
            onTap: () => context.push('/kyc'),
          ),
          _MoreTile(
            icon: Icons.person_outline,
            color: const Color(0xFF6366F1),
            title: 'My Profile',
            subtitle: 'Account settings & password',
            onTap: () => context.push('/profile'),
          ),
        ],
      ),
    );
  }
}

class _SectionHeader extends StatelessWidget {
  final String title;
  const _SectionHeader(this.title);

  @override
  Widget build(BuildContext context) => Padding(
        padding: const EdgeInsets.only(bottom: 8, top: 4),
        child: Text(title.toUpperCase(),
            style: TextStyle(
                fontSize: 11,
                fontWeight: FontWeight.w700,
                letterSpacing: 1.0,
                color: Theme.of(context).colorScheme.onSurfaceVariant)),
      );
}

class _MoreTile extends StatelessWidget {
  final IconData icon;
  final Color color;
  final String title, subtitle;
  final VoidCallback onTap;
  const _MoreTile({required this.icon, required this.color,
      required this.title, required this.subtitle, required this.onTap});

  @override
  Widget build(BuildContext context) => Card(
        margin: const EdgeInsets.only(bottom: 8),
        child: ListTile(
          leading: Container(
            width: 44, height: 44,
            decoration: BoxDecoration(
                color: color.withValues(alpha: 0.12),
                borderRadius: BorderRadius.circular(10)),
            child: Icon(icon, color: color, size: 22),
          ),
          title: Text(title,
              style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 14)),
          subtitle: Text(subtitle,
              style: const TextStyle(fontSize: 12)),
          trailing: const Icon(Icons.chevron_right),
          onTap: onTap,
        ),
      );
}
