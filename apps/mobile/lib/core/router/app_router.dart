import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../features/auth/auth_provider.dart';
import '../../features/auth/login_screen.dart';
import '../../features/auth/register_screen.dart';
import '../../features/shell/shell_screen.dart';
import '../../features/dashboard/dashboard_screen.dart';
import '../../features/wallets/wallets_screen.dart';
import '../../features/transfers/transfers_screen.dart';
import '../../features/beneficiaries/beneficiaries_screen.dart';
import '../../features/more/more_screen.dart';
import '../../features/transactions/transactions_screen.dart';
import '../../features/exchange/exchange_screen.dart';
import '../../features/statements/statements_screen.dart';
import '../../features/scheduled/scheduled_screen.dart';
import '../../features/profile/profile_screen.dart';
import '../../features/kyc/kyc_screen.dart';

final routerProvider = Provider<GoRouter>((ref) {
  final notifier = _RouterNotifier(ref);
  return GoRouter(
    initialLocation: '/dashboard',
    refreshListenable: notifier,
    redirect: notifier.redirect,
    routes: [
      GoRoute(path: '/login',    builder: (_, __) => const LoginScreen()),
      GoRoute(path: '/register', builder: (_, __) => const RegisterScreen()),

      StatefulShellRoute.indexedStack(
        builder: (context, state, shell) => ShellScreen(shell: shell),
        branches: [
          // Tab 0 — Home
          StatefulShellBranch(routes: [
            GoRoute(path: '/dashboard', builder: (_, __) => const DashboardScreen()),
          ]),

          // Tab 1 — Wallets
          StatefulShellBranch(routes: [
            GoRoute(path: '/wallets', builder: (_, __) => const WalletsScreen()),
          ]),

          // Tab 2 — Send
          StatefulShellBranch(routes: [
            GoRoute(path: '/transfers', builder: (_, __) => const TransfersScreen()),
          ]),

          // Tab 3 — Contacts / Beneficiaries
          StatefulShellBranch(routes: [
            GoRoute(path: '/beneficiaries', builder: (_, __) => const BeneficiariesScreen()),
          ]),

          // Tab 4 — More hub + all sub-screens
          StatefulShellBranch(routes: [
            GoRoute(path: '/more', builder: (_, __) => const MoreScreen()),
          ]),
        ],
      ),

      // ── Full-screen routes pushed on top of the shell ─────────────────────
      GoRoute(path: '/transactions', builder: (_, __) => const TransactionsScreen()),
      GoRoute(path: '/exchange',     builder: (_, __) => const ExchangeScreen()),
      GoRoute(path: '/statements',   builder: (_, __) => const StatementsScreen()),
      GoRoute(path: '/scheduled',    builder: (_, __) => const ScheduledScreen()),
      GoRoute(path: '/profile',      builder: (_, __) => const ProfileScreen()),
      GoRoute(path: '/kyc',          builder: (_, __) => const KycScreen()),
    ],
  );
});

class _RouterNotifier extends ChangeNotifier {
  final Ref _ref;
  _RouterNotifier(this._ref) {
    _ref.listen(authProvider, (_, __) => notifyListeners());
  }

  String? redirect(BuildContext context, GoRouterState state) {
    final isAuth = _ref.read(authProvider).isAuthenticated;
    final loc    = state.matchedLocation;
    final onAuth = loc == '/login' || loc == '/register';
    if (!isAuth && !onAuth) return '/login';
    if (isAuth && onAuth) return '/dashboard';
    return null;
  }
}
