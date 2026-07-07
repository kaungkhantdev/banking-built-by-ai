import { Component, inject, signal } from '@angular/core';
import { DatePipe, SlicePipe } from '@angular/common';
import { FraudService } from '../../core/services/fraud.service';
import { ToastService } from '../../core/services/toast.service';
import { FraudAlertStatus, FraudAlertView, Page } from '../../core/models/api.models';
import { BadgeComponent, BadgeVariant } from '../../shared/badge.component';
import { PaginationComponent } from '../../shared/pagination.component';
import { SpinnerComponent } from '../../shared/spinner.component';
import { EmptyStateComponent } from '../../shared/empty-state.component';

const STATUS_VARIANT: Record<FraudAlertStatus, BadgeVariant> = {
  BLOCKED:        'error',
  PENDING_REVIEW: 'warning',
  APPROVED:       'success',
  FALSE_POSITIVE: 'neutral',
};

@Component({
  selector: 'app-fraud',
  imports: [DatePipe, SlicePipe, BadgeComponent, PaginationComponent, SpinnerComponent, EmptyStateComponent],
  template: `
    <div class="bg-white rounded-xl border border-slate-200 overflow-hidden">
      <div class="px-6 py-4 border-b border-slate-200 flex flex-wrap items-center gap-3">
        <h2 class="text-sm font-semibold text-slate-900 flex items-center gap-2 flex-1">
          <span class="material-symbols-outlined text-red-500" style="font-size:18px">gpp_maybe</span>
          Fraud Alerts
        </h2>
        <select [value]="status()" (change)="onStatusChange($event)"
                class="px-3 py-2 border border-slate-300 rounded-lg text-sm outline-none
                       focus:ring-2 focus:ring-indigo-500 bg-white">
          <option value="">All statuses</option>
          <option value="BLOCKED">Blocked (held)</option>
          <option value="PENDING_REVIEW">Pending review</option>
          <option value="APPROVED">Approved</option>
          <option value="FALSE_POSITIVE">False positive</option>
        </select>
      </div>

      @if (loading()) {
        <div class="flex justify-center py-12"><app-spinner /></div>
      } @else if (!page() || !page()!.content.length) {
        <app-empty-state icon="verified_user" message="No fraud alerts"
                         hint="High-risk transfers that need review will appear here." />
      } @else {
        <div class="overflow-x-auto">
          <table class="w-full text-sm">
            <thead class="bg-slate-50 border-b border-slate-200">
              <tr>
                <th class="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Status</th>
                <th class="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Risk</th>
                <th class="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Transfer</th>
                <th class="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Rules</th>
                <th class="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Raised</th>
                <th class="text-right px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Actions</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-100">
              @for (a of page()!.content; track a.id) {
                <tr class="hover:bg-slate-50 transition-colors">
                  <td class="px-4 py-3"><app-badge [label]="a.status" [variant]="statusVariant(a.status)" /></td>
                  <td class="px-4 py-3 font-semibold text-slate-800">{{ a.riskScore }}</td>
                  <td class="px-4 py-3 font-mono text-xs text-slate-400">
                    {{ a.transferId ? (a.transferId | slice:0:8) + '…' : '—' }}
                  </td>
                  <td class="px-4 py-3 text-xs text-slate-500 max-w-xs truncate" [title]="a.ruleDetails || ''">
                    {{ a.ruleDetails || '—' }}
                  </td>
                  <td class="px-4 py-3 text-xs text-slate-500">{{ a.createdAt | date:'dd MMM, HH:mm' }}</td>
                  <td class="px-4 py-3 text-right whitespace-nowrap">
                    @if (isOpen(a)) {
                      <button (click)="approve(a)" [disabled]="acting()"
                              class="text-xs font-medium text-emerald-600 hover:text-emerald-800 disabled:opacity-50">
                        Approve
                      </button>
                      <span class="text-slate-300 mx-2">|</span>
                      <button (click)="dismiss(a)" [disabled]="acting()"
                              class="text-xs font-medium text-slate-500 hover:text-slate-700 disabled:opacity-50">
                        Dismiss
                      </button>
                    } @else {
                      <span class="text-xs text-slate-400">Resolved</span>
                    }
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
        <app-pagination [page]="currentPage()" [totalElements]="page()!.totalElements"
                        [totalPages]="page()!.totalPages" (pageChange)="onPageChange($event)" />
      }
    </div>
  `,
})
export class FraudComponent {
  private readonly fraudSvc = inject(FraudService);
  private readonly toast    = inject(ToastService);

  readonly loading     = signal(true);
  readonly acting      = signal(false);
  readonly page        = signal<Page<FraudAlertView> | null>(null);
  readonly status      = signal('');
  readonly currentPage = signal(0);

  constructor() { this.load(); }

  private load(): void {
    this.loading.set(true);
    this.fraudSvc.list(this.status() || undefined, this.currentPage()).subscribe({
      next: p => { this.page.set(p); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  onStatusChange(e: Event): void {
    this.status.set((e.target as HTMLSelectElement).value);
    this.currentPage.set(0);
    this.load();
  }

  onPageChange(p: number): void { this.currentPage.set(p); this.load(); }

  isOpen(a: FraudAlertView): boolean {
    return a.status === 'BLOCKED' || a.status === 'PENDING_REVIEW';
  }

  approve(a: FraudAlertView): void {
    this.acting.set(true);
    this.fraudSvc.approve(a.id).subscribe({
      next: () => { this.toast.success('Alert approved'); this.acting.set(false); this.load(); },
      error: () => this.acting.set(false),
    });
  }

  dismiss(a: FraudAlertView): void {
    this.acting.set(true);
    this.fraudSvc.dismiss(a.id).subscribe({
      next: () => { this.toast.success('Alert dismissed'); this.acting.set(false); this.load(); },
      error: () => this.acting.set(false),
    });
  }

  statusVariant(s: FraudAlertStatus): BadgeVariant { return STATUS_VARIANT[s] ?? 'neutral'; }
}
