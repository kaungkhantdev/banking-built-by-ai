import { Component, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { toSignal } from '@angular/core/rxjs-interop';
import { catchError, of } from 'rxjs';
import { AdminService } from '../../core/services/admin.service';
import { HealthSummary, PlatformStats } from '../../core/models/api.models';
import { SpinnerComponent } from '../../shared/spinner.component';

interface StatCard {
  label: string;
  key: keyof PlatformStats;
  icon: string;
  color: string;
}

const STAT_CARDS: StatCard[] = [
  { label: 'Total Users',        key: 'totalUsers',        icon: 'group',            color: 'text-indigo-600 bg-indigo-50' },
  { label: 'Total Accounts',     key: 'totalAccounts',     icon: 'account_circle',   color: 'text-blue-600 bg-blue-50'    },
  { label: 'Total Wallets',      key: 'totalWallets',      icon: 'wallet',           color: 'text-emerald-600 bg-emerald-50' },
  { label: 'Pending KYC',        key: 'pendingKycCount',   icon: 'pending_actions',  color: 'text-amber-600 bg-amber-50'  },
  { label: 'Total Transactions', key: 'totalTransactions', icon: 'swap_horiz',       color: 'text-violet-600 bg-violet-50' },
];

@Component({
  selector: 'app-dashboard',
  imports: [DecimalPipe, SpinnerComponent],
  template: `
    <div class="space-y-6">

      <!-- Stats grid -->
      <div>
        <h2 class="text-sm font-semibold text-slate-500 uppercase tracking-wide mb-3">Platform Overview</h2>
        @if (statsLoading()) {
          <div class="flex justify-center py-10"><app-spinner /></div>
        } @else if (stats()) {
          <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5 gap-4">
            @for (card of statCards; track card.key) {
              <div class="bg-white rounded-xl border border-slate-200 p-5 hover:shadow-md transition-shadow">
                <div class="flex items-center justify-between mb-3">
                  <span class="text-xs font-medium text-slate-500 uppercase tracking-wide">{{ card.label }}</span>
                  <div class="w-9 h-9 rounded-lg flex items-center justify-center" [class]="card.color">
                    <span class="material-symbols-outlined" style="font-size:18px">{{ card.icon }}</span>
                  </div>
                </div>
                <p class="text-3xl font-bold text-slate-900">
                  {{ stats()![card.key] | number }}
                </p>
              </div>
            }
          </div>
        }
      </div>

      <!-- System Health -->
      <div>
        <h2 class="text-sm font-semibold text-slate-500 uppercase tracking-wide mb-3">System Health</h2>
        @if (healthLoading()) {
          <div class="flex justify-center py-10"><app-spinner /></div>
        } @else if (health()) {
          <div class="bg-white rounded-xl border border-slate-200 p-5">
            <div class="flex items-center gap-3 mb-5">
              <div class="w-3 h-3 rounded-full" [class]="statusDot(health()!.status)"></div>
              <span class="font-semibold text-slate-900">
                System {{ health()!.status }}
              </span>
            </div>
            <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
              <div class="flex items-center gap-3 p-3 rounded-lg bg-slate-50">
                <span class="material-symbols-outlined text-slate-400" style="font-size:20px">database</span>
                <div>
                  <p class="text-xs text-slate-500 font-medium">Database</p>
                  <p class="text-sm font-semibold" [class]="statusText(health()!.db)">
                    {{ health()!.db }}
                  </p>
                </div>
              </div>
              <div class="flex items-center gap-3 p-3 rounded-lg bg-slate-50">
                <span class="material-symbols-outlined text-slate-400" style="font-size:20px">hub</span>
                <div>
                  <p class="text-xs text-slate-500 font-medium">Message Broker</p>
                  <p class="text-sm font-semibold" [class]="statusText(health()!.broker)">
                    {{ health()!.broker }}
                  </p>
                </div>
              </div>
              <div class="flex items-center gap-3 p-3 rounded-lg bg-slate-50">
                <span class="material-symbols-outlined text-slate-400" style="font-size:20px">schedule</span>
                <div>
                  <p class="text-xs text-slate-500 font-medium">Outbox Lag</p>
                  <p class="text-sm font-semibold" [class]="health()!.outboxLagSeconds > 30 ? 'text-amber-600' : 'text-emerald-600'">
                    {{ health()!.outboxLagSeconds }}s
                  </p>
                </div>
              </div>
            </div>
          </div>
        }
      </div>

      <!-- Quick links -->
      <div>
        <h2 class="text-sm font-semibold text-slate-500 uppercase tracking-wide mb-3">Quick Actions</h2>
        <div class="grid grid-cols-2 sm:grid-cols-4 gap-3">
          @for (action of quickActions; track action.label) {
            <a [href]="action.href"
               class="bg-white rounded-xl border border-slate-200 p-4 hover:border-indigo-300
                      hover:shadow-md transition-all flex items-center gap-3 text-sm font-medium text-slate-700">
              <span class="material-symbols-outlined text-indigo-500" style="font-size:20px">{{ action.icon }}</span>
              {{ action.label }}
            </a>
          }
        </div>
      </div>

    </div>
  `,
})
export class DashboardComponent {
  private readonly admin = inject(AdminService);

  readonly statCards    = STAT_CARDS;
  readonly statsLoading = signal(true);
  readonly healthLoading = signal(true);

  readonly stats = toSignal(
    this.admin.stats().pipe(catchError(() => of(null))),
    { initialValue: null }
  );

  readonly health = toSignal(
    this.admin.health().pipe(catchError(() => of(null))),
    { initialValue: null }
  );

  readonly quickActions = [
    { label: 'New Account',  href: '/accounts',  icon: 'add_circle'    },
    { label: 'New Transfer', href: '/transfers',  icon: 'swap_horiz'    },
    { label: 'KYC Queue',    href: '/kyc',        icon: 'pending_actions' },
    { label: 'Audit Trail',  href: '/audit',      icon: 'history'       },
  ];

  constructor() {
    this.admin.stats().subscribe({ complete: () => this.statsLoading.set(false), error: () => this.statsLoading.set(false) });
    this.admin.health().subscribe({ complete: () => this.healthLoading.set(false), error: () => this.healthLoading.set(false) });
  }

  statusDot(s: string) {
    return { UP: 'bg-emerald-500', DOWN: 'bg-red-500', DEGRADED: 'bg-amber-500' }[s] ?? 'bg-slate-400';
  }

  statusText(s: string) {
    return s === 'UP' ? 'text-emerald-600' : 'text-red-600';
  }
}
