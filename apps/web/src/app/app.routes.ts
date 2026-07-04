import { Routes } from '@angular/router';
import { ShellComponent } from './layout/shell.component';
import { AuthLayoutComponent } from './layout/auth-layout.component';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },

  {
    path: '',
    component: AuthLayoutComponent,
    children: [
      {
        path: 'login',
        loadComponent: () =>
          import('./features/auth/login.component').then(m => m.LoginComponent),
        title: 'Sign in — BankCore',
      },
      {
        path: 'register',
        loadComponent: () =>
          import('./features/auth/register.component').then(m => m.RegisterComponent),
        title: 'Register — BankCore',
      },
    ],
  },

  {
    path: '',
    component: ShellComponent,
    canActivate: [authGuard],
    children: [
      // ── Core ──────────────────────────────────────────────────────────────
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent),
        title: 'Dashboard — BankCore',
      },
      {
        path: 'customers',
        loadComponent: () =>
          import('./features/customers/customers.component').then(m => m.CustomersComponent),
        title: 'Customers — BankCore',
      },
      {
        path: 'accounts',
        loadComponent: () =>
          import('./features/accounts/accounts.component').then(m => m.AccountsComponent),
        title: 'Accounts — BankCore',
      },
      {
        path: 'wallets',
        loadComponent: () =>
          import('./features/wallets/wallets.component').then(m => m.WalletsComponent),
        title: 'Wallets — BankCore',
      },
      {
        path: 'transfers',
        loadComponent: () =>
          import('./features/transfers/transfers.component').then(m => m.TransfersComponent),
        title: 'Transfers — BankCore',
      },
      {
        path: 'scheduled-transfers',
        loadComponent: () =>
          import('./features/scheduled-transfers/scheduled-transfers.component').then(m => m.ScheduledTransfersComponent),
        title: 'Scheduled Transfers — BankCore',
      },
      {
        path: 'transactions',
        loadComponent: () =>
          import('./features/history/transactions.component').then(m => m.TransactionsComponent),
        title: 'Transactions — BankCore',
      },
      {
        path: 'kyc',
        loadComponent: () =>
          import('./features/kyc/kyc.component').then(m => m.KycComponent),
        title: 'KYC — BankCore',
      },
      {
        path: 'audit',
        loadComponent: () =>
          import('./features/audit/audit.component').then(m => m.AuditComponent),
        title: 'Audit Trail — BankCore',
      },

      // ── Finance ───────────────────────────────────────────────────────────
      {
        path: 'exchange',
        loadComponent: () =>
          import('./features/exchange/exchange.component').then(m => m.ExchangeComponent),
        title: 'Exchange Rates — BankCore',
      },
      {
        path: 'reports',
        loadComponent: () =>
          import('./features/reports/reports.component').then(m => m.ReportsComponent),
        title: 'Reports — BankCore',
      },

      // ── Admin ─────────────────────────────────────────────────────────────
      {
        path: 'admin/users',
        loadComponent: () =>
          import('./features/admin/users.component').then(m => m.UsersComponent),
        title: 'Admin Users — BankCore',
      },
      {
        path: 'admin/webhooks',
        loadComponent: () =>
          import('./features/webhooks/webhooks.component').then(m => m.WebhooksComponent),
        title: 'Webhooks — BankCore',
      },
      {
        path: 'fraud',
        loadComponent: () =>
          import('./features/fraud/fraud.component').then(m => m.FraudComponent),
        title: 'Fraud Alerts — BankCore',
      },
    ],
  },

  { path: '**', redirectTo: 'dashboard' },
];
