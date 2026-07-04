import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe, SlicePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { HistoryService } from '../../core/services/history.service';
import { WalletService } from '../../core/services/wallet.service';
import { ToastService } from '../../core/services/toast.service';
import { Direction, Page, TransactionView, WalletListItem } from '../../core/models/api.models';
import { BadgeComponent, BadgeVariant } from '../../shared/badge.component';
import { SpinnerComponent } from '../../shared/spinner.component';
import { EmptyStateComponent } from '../../shared/empty-state.component';
import { PaginationComponent } from '../../shared/pagination.component';
import { SearchableSelectComponent, SelectOption } from '../../shared/searchable-select.component';

@Component({
  selector: 'app-transactions',
  imports: [
    ReactiveFormsModule, DatePipe, DecimalPipe, SlicePipe,
    BadgeComponent, SpinnerComponent, EmptyStateComponent, PaginationComponent,
    SearchableSelectComponent,
  ],
  template: `
    <div class="space-y-6">

      <section class="bg-white rounded-xl border border-slate-200">
        <div class="p-6 border-b border-slate-200">
          <h2 class="text-sm font-semibold text-slate-900 mb-4 flex items-center gap-2">
            <span class="material-symbols-outlined text-indigo-500" style="font-size:18px">receipt_long</span>
            Transactions
          </h2>

          <form [formGroup]="form" (ngSubmit)="load()" class="flex flex-wrap items-end gap-3">
            <div class="flex-1 min-w-72">
              <label class="block text-xs font-medium text-slate-500 mb-1.5">Account / Wallet</label>
              <app-searchable-select formControlName="walletId"
                                     [options]="walletOptions()"
                                     placeholder="Search by owner, currency, or ID" />
            </div>
            <div class="w-40">
              <label class="block text-xs font-medium text-slate-500 mb-1.5">Direction</label>
              <select formControlName="direction"
                      class="w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm outline-none
                             focus:ring-2 focus:ring-indigo-500 focus:border-transparent">
                <option value="">All</option>
                <option value="DEBIT">Debit</option>
                <option value="CREDIT">Credit</option>
              </select>
            </div>
            <button type="submit" [disabled]="txLoading() || !form.value.walletId"
                    class="flex items-center gap-1.5 bg-indigo-600 hover:bg-indigo-700 disabled:opacity-60
                           text-white text-sm font-medium px-4 py-2.5 rounded-lg transition-colors shrink-0">
              @if (txLoading()) { <app-spinner size="xs" /> }
              Load
            </button>
            <button type="button" (click)="exportCsv()" [disabled]="exporting() || !form.value.walletId"
                    class="flex items-center gap-1.5 border border-slate-300 hover:bg-slate-50 disabled:opacity-60
                           text-slate-700 text-sm font-medium px-4 py-2.5 rounded-lg transition-colors shrink-0">
              @if (exporting()) { <app-spinner size="xs" /> }
              @else { <span class="material-symbols-outlined" style="font-size:16px">download</span> }
              Export CSV
            </button>
          </form>
        </div>

        @if (txPage()) {
          @if (!txPage()!.content.length) {
            <app-empty-state icon="receipt_long" message="No transactions found"
                             hint="This wallet has no activity for the selected filter yet." />
          } @else {
            <div class="overflow-x-auto">
              <table class="w-full text-sm">
                <thead class="bg-slate-50 border-b border-slate-200">
                  <tr>
                    <th class="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Direction</th>
                    <th class="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Amount</th>
                    <th class="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Running Balance</th>
                    <th class="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Currency</th>
                    <th class="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Memo</th>
                    <th class="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Posted At</th>
                    <th class="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Tx ID</th>
                  </tr>
                </thead>
                <tbody class="divide-y divide-slate-100">
                  @for (t of txPage()!.content; track t.id) {
                    <tr class="hover:bg-slate-50 transition-colors">
                      <td class="px-4 py-3">
                        <app-badge [label]="t.direction" [variant]="dirVariant(t.direction)" />
                      </td>
                      <td class="px-4 py-3 font-medium text-slate-800">{{ t.amount | number:'1.2-4' }}</td>
                      <td class="px-4 py-3 text-slate-600">{{ t.runningBalance != null ? (t.runningBalance | number:'1.2-4') : '—' }}</td>
                      <td class="px-4 py-3 text-xs text-slate-500">{{ t.currency }}</td>
                      <td class="px-4 py-3 text-xs text-slate-500">{{ t.memo || '—' }}</td>
                      <td class="px-4 py-3 text-xs text-slate-500">{{ t.postedAt | date:'dd MMM, HH:mm' }}</td>
                      <td class="px-4 py-3 font-mono text-xs text-slate-400">{{ t.transactionId | slice:0:8 }}…</td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
            <app-pagination [page]="txCurrentPage()" [totalElements]="txPage()!.totalElements"
                            [totalPages]="txPage()!.totalPages" (pageChange)="onTxPageChange($event)" />
          }
        } @else {
          <app-empty-state icon="account_balance_wallet" message="Select an account to view transactions"
                           hint="Pick a wallet above, then Load or Export CSV." />
        }
      </section>

    </div>
  `,
})
export class TransactionsComponent implements OnInit {
  private readonly historySvc = inject(HistoryService);
  private readonly walletSvc  = inject(WalletService);
  private readonly toast      = inject(ToastService);
  private readonly fb         = inject(FormBuilder);

  readonly txLoading     = signal(false);
  readonly exporting      = signal(false);
  readonly txPage         = signal<Page<TransactionView> | null>(null);
  readonly txCurrentPage  = signal(0);

  private readonly wallets = signal<WalletListItem[]>([]);
  readonly walletOptions = computed<SelectOption[]>(() =>
    this.wallets().map(w => ({
      value: w.id,
      label: `${w.ownerEmail} · ${w.currency}${w.status !== 'ACTIVE' ? ` (${w.status})` : ''}`,
      sublabel: w.id,
    })));

  readonly form = this.fb.group({
    walletId:  [''],
    direction: [''],
  });

  ngOnInit(): void {
    this.walletSvc.list().subscribe({ next: ws => this.wallets.set(ws) });
  }

  load(): void {
    const walletId = this.form.value.walletId;
    if (!walletId) return;
    this.txLoading.set(true);
    this.historySvc.list(walletId, this.txCurrentPage(), 20, this.form.value.direction || undefined).subscribe({
      next: p => { this.txPage.set(p); this.txLoading.set(false); },
      error: () => this.txLoading.set(false),
    });
  }

  onTxPageChange(p: number): void { this.txCurrentPage.set(p); this.load(); }

  /** One-click synchronous CSV download for the selected wallet + filter. */
  exportCsv(): void {
    const walletId = this.form.value.walletId;
    if (!walletId || this.exporting()) return;
    this.exporting.set(true);
    this.historySvc.export(walletId, this.form.value.direction || undefined).subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `transactions-${walletId}.csv`;
        a.click();
        URL.revokeObjectURL(url);
        this.toast.success('Transactions exported');
        this.exporting.set(false);
      },
      error: () => { this.toast.error('Export failed'); this.exporting.set(false); },
    });
  }

  dirVariant(d: Direction): BadgeVariant { return d === 'CREDIT' ? 'success' : 'warning'; }
}
