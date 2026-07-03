import 'package:flutter/material.dart';

enum BadgeVariant { success, warning, error, info, neutral }

class StatusBadge extends StatelessWidget {
  final String label;
  final BadgeVariant variant;

  const StatusBadge({super.key, required this.label, this.variant = BadgeVariant.neutral});

  factory StatusBadge.forAccountStatus(String status) => StatusBadge(
        label: status,
        variant: switch (status) {
          'ACTIVE'  => BadgeVariant.success,
          'PENDING' => BadgeVariant.warning,
          'FROZEN'  => BadgeVariant.error,
          _         => BadgeVariant.neutral,
        },
      );

  factory StatusBadge.forWalletStatus(String status) => StatusBadge(
        label: status,
        variant: switch (status) {
          'ACTIVE' => BadgeVariant.success,
          'FROZEN' => BadgeVariant.error,
          _        => BadgeVariant.neutral,
        },
      );

  factory StatusBadge.forKycStatus(String status) => StatusBadge(
        label: status,
        variant: switch (status) {
          'VERIFIED'      => BadgeVariant.success,
          'REJECTED'      => BadgeVariant.error,
          'UNDER_REVIEW'  => BadgeVariant.warning,
          'DOCS_SUBMITTED'=> BadgeVariant.info,
          _               => BadgeVariant.neutral,
        },
      );

  @override
  Widget build(BuildContext context) {
    final colors = _colors(context);
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
      decoration: BoxDecoration(
        color: colors.$1,
        borderRadius: BorderRadius.circular(6),
        border: Border.all(color: colors.$2),
      ),
      child: Text(
        label,
        style: TextStyle(
          fontSize: 11,
          fontWeight: FontWeight.w600,
          color: colors.$2,
          letterSpacing: 0.3,
        ),
      ),
    );
  }

  (Color, Color) _colors(BuildContext context) {
    return switch (variant) {
      BadgeVariant.success => (const Color(0xFFECFDF5), const Color(0xFF059669)),
      BadgeVariant.warning => (const Color(0xFFFFFBEB), const Color(0xFFD97706)),
      BadgeVariant.error   => (const Color(0xFFFEF2F2), const Color(0xFFDC2626)),
      BadgeVariant.info    => (const Color(0xFFEFF6FF), const Color(0xFF2563EB)),
      BadgeVariant.neutral => (const Color(0xFFF8FAFC), const Color(0xFF64748B)),
    };
  }
}
