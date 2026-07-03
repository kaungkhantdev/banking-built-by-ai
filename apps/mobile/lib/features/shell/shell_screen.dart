import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

class ShellScreen extends StatelessWidget {
  final StatefulNavigationShell shell;
  const ShellScreen({super.key, required this.shell});

  static const _tabs = [
    (icon: Icons.home_outlined,        activeIcon: Icons.home,              label: 'Home'),
    (icon: Icons.wallet_outlined,      activeIcon: Icons.wallet,            label: 'Wallets'),
    (icon: Icons.send_outlined,        activeIcon: Icons.send,              label: 'Send'),
    (icon: Icons.people_outline,       activeIcon: Icons.people,            label: 'Contacts'),
    (icon: Icons.grid_view_outlined,   activeIcon: Icons.grid_view,         label: 'More'),
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: shell,
      bottomNavigationBar: NavigationBar(
        selectedIndex: shell.currentIndex,
        onDestinationSelected: (i) => shell.goBranch(
          i,
          initialLocation: i == shell.currentIndex,
        ),
        destinations: _tabs.map((t) => NavigationDestination(
          icon: Icon(t.icon),
          selectedIcon: Icon(t.activeIcon),
          label: t.label,
        )).toList(),
      ),
    );
  }
}
