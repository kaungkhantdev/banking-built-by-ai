import { Component, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe, SlicePipe } from '@angular/common';
import { ScheduledTransferService } from '../../core/services/scheduled-transfer.service';
import { ToastService } from '../../core/services/toast.service';
import { Page, ScheduledTransferView } from '../../core/models/api.models';
import { BadgeComponent, BadgeVariant } from '../../shared/badge.component';
import { PaginationComponent } from '../../shared/pagination.component';
import { SpinnerComponent } from '../../shared/spinner.component';

const STATUS_VARIANT: Record<string, BadgeVariant> = {
  ACTIVE:    'success',
  PAUSED:    'warning',
  FAILED:    'error',
  COMPLETED: 'neutral',
};

@Component({
  selector: 'app-scheduled-transfers',
  imports: [DatePipe, DecimalPipe, SlicePipe, BadgeComponent, PaginationComponent, SpinnerComponent],
  template: `
    <div class="space-y-5">
      <div class="bg-white rounded-xl border border-slate-200 overflow-hidden">
        @if (loading()) {
          <div class="flex justify-center py-16"><app-spinner /></div>
        } @else if (!page()?.content?.length) {
          <div class="text-center py-16 text-slate-400">
            <span class="material-symbols-outlined text-4xl mb-2 block">event_repeat</span>
            <p class="text-sm">No scheduled transfers</p>
          </div>
        } @else {
          <div class="overflow-x-auto">
            <table class="w-full text-sm">
              <thead class="bg-slate-50 border-b border-slate-200">
                <tr>
                  <th class="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">From Wallet</th>
                  <th class="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">To Wallet</th>
                  <th class="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Amount</th>
                  <th class="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Recurrence</th>
                  <th class="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Next Run</th>
                  <th class="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Status</th>
                  <th class="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Actions</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-slate-100">
                @for (s of page()!.content; track s.id) {
                  <tr class="hover:bg-slate-50 transition-colors">
                    <td class="px-4 py-3 font-mono text-xs text-slate-500">{{ s.fromWalletId | slice:0:8 }}…</td>
                    <td class="px-4 py-3 font-mono text-xs text-slate-500">{{ s.toWalletId | slice:0:8 }}…</td>
                    <td class="px-4 py-3 font-medium text-slate-800">{{ s.amount | number:'1.2-2' }}</td>
                    <td class="px-4 py-3 text-xs text-slate-500">{{ s.recurrenceRule || 'ONCE' }}</td>
                    <td class="px-4 py-3 text-xs text-slate-500">{{ s.nextRunAt | date:'dd MMM, HH:mm' }}</td>
                    <td class="px-4 py-3">
                      <app-badge [label]="s.status" [variant]="statusVariant(s.status)" />
                    </td>
                    <td class="px-4 py-3">
                      @if (s.status === 'ACTIVE' || s.status === 'PAUSED') {
                        <button (click)="cancel(s)"
                                class="text-xs font-medium text-red-600 hover:text-red-800">
                          Cancel
                        </button>
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
    </div>
  `,
})
export class ScheduledTransfersComponent {
  private readonly scheduleSvc = inject(ScheduledTransferService);
  private readonly toast       = inject(ToastService);

  readonly loading     = signal(true);
  readonly currentPage = signal(0);
  readonly page        = signal<Page<ScheduledTransferView> | null>(null);

  constructor() { this.load(); }

  private load(): void {
    this.loading.set(true);
    this.scheduleSvc.list(this.currentPage()).subscribe({
      next: p => { this.page.set(p); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  onPageChange(p: number): void { this.currentPage.set(p); this.load(); }

  cancel(s: ScheduledTransferView): void {
    this.scheduleSvc.cancel(s.id).subscribe({
      next: () => { this.toast.success('Schedule cancelled'); this.load(); },
    });
  }

  statusVariant(s: string): BadgeVariant { return STATUS_VARIANT[s] ?? 'neutral'; }
}
