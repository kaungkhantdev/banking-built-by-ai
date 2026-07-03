import { Component, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe, SlicePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { WalletService } from '../../core/services/wallet.service';
import { HistoryService } from '../../core/services/history.service';
import { ExchangeService } from '../../core/services/exchange.service';
import { ToastService } from '../../core/services/toast.service';
import { BalanceView, Direction, Page, TransactionView, WalletView, WalletStatus } from '../../core/models/api.models';
import { BadgeComponent, BadgeVariant } from '../../shared/badge.component';
import { PaginationComponent } from '../../shared/pagination.component';
import { SpinnerComponent } from '../../shared/spinner.component';

const STATUS_VARIANT: Record<WalletStatus, BadgeVariant> = {
  ACTIVE: 'success',
  FROZEN: 'error',
  CLOSED: 'neutral',
};


const DIR_VARIANT: Record<Direction, BadgeVariant> = {
  CREDIT: 'success',
  DEBIT:  'error',
};

@Component({
  selector: 'app-wallets',
  imports: [ReactiveFormsModule, DatePipe, DecimalPipe, SlicePipe, BadgeComponent, PaginationComponent, SpinnerComponent],
  template: `
    <div class="space-y-6">

      <!-- Open wallet -->
      <section class="bg-white rounded-xl border border-slate-200 p-6">
        <h2 class="text-sm font-semibold text-slate-900 mb-4 flex items-center gap-2">
          <span class="material-symbols-outlined text-indigo-500" style="font-size:18px">add_card</span>
          Open Wallet
        </h2>
        <form [formGroup]="openForm" (ngSubmit)="openWallet()" class="flex flex-wrap gap-3" novalidate>
          <div class="flex-1 min-w-48">
            <label class="block text-xs font-medium text-slate-500 mb-1">Account ID (UUID)</label>
            <input type="text" formControlName="accountId"
                   placeholder="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
                   class="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm font-mono outline-none
                          focus:ring-2 focus:ring-indigo-500 focus:border-transparent placeholder:font-sans placeholder:text-slate-400">
          </div>
          <div>
            <label class="block text-xs font-medium text-slate-500 mb-1">Currency</label>
            <select formControlName="currency"
                    class="px-3 py-2 border border-slate-300 rounded-lg text-sm outline-none
                           focus:ring-2 focus:ring-indigo-500 focus:border-transparent bg-white">
              @for (c of currencies(); track c) { <option [value]="c">{{ c }}</option> }
            </select>
          </div>
          <div class="flex items-end">
            <button type="submit" [disabled]="openLoading() || openForm.invalid"
                    class="flex items-center gap-2 bg-indigo-600 hover:bg-indigo-700 disabled:opacity-60
                           text-white text-sm font-medium px-4 py-2 rounded-lg transition-colors">
              @if (openLoading()) { <app-spinner size="xs" /> }
              Open Wallet
            </button>
          </div>
        </form>
        @if (newWallet()) {
          <div class="mt-4 p-4 bg-emerald-50 border border-emerald-200 rounded-lg">
            <p class="text-xs font-semibold text-emerald-700 mb-1">Wallet created</p>
            <div class="grid grid-cols-2 sm:grid-cols-4 gap-2 text-xs text-emerald-800">
              <span class="font-mono">{{ newWallet()!.id }}</span>
              <span>{{ newWallet()!.currency }}</span>
              <app-badge [label]="newWallet()!.status" [variant]="statusVariant(newWallet()!.status)" />
            </div>
          </div>
        }
      </section>

      <!-- Check balance -->
      <section class="bg-white rounded-xl border border-slate-200 p-6">
        <h2 class="text-sm font-semibold text-slate-900 mb-4 flex items-center gap-2">
          <span class="material-symbols-outlined text-indigo-500" style="font-size:18px">account_balance_wallet</span>
          Check Balance
        </h2>
        <form [formGroup]="balanceForm" (ngSubmit)="checkBalance()" class="flex gap-3" novalidate>
          <div class="flex-1">
            <input type="text" formControlName="walletId"
                   placeholder="Wallet ID (UUID)"
                   class="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm font-mono outline-none
                          focus:ring-2 focus:ring-indigo-500 focus:border-transparent placeholder:font-sans placeholder:text-slate-400">
          </div>
          <button type="submit" [disabled]="balanceLoading() || balanceForm.invalid"
                  class="flex items-center gap-2 bg-slate-800 hover:bg-slate-900 disabled:opacity-60
                         text-white text-sm font-medium px-4 py-2 rounded-lg transition-colors shrink-0">
            @if (balanceLoading()) { <app-spinner size="xs" /> }
            Get Balance
          </button>
        </form>
        @if (balance()) {
          <div class="mt-4 flex items-baseline gap-3">
            <span class="text-3xl font-bold text-slate-900">{{ balance()!.balance | number:'1.2-2' }}</span>
            <span class="text-sm text-slate-500 font-mono">{{ balance()!.walletId | slice:0:8 }}…</span>
          </div>
        }
      </section>

      <!-- Freeze wallet -->
      <section class="bg-white rounded-xl border border-slate-200 p-6">
        <h2 class="text-sm font-semibold text-slate-900 mb-4 flex items-center gap-2">
          <span class="material-symbols-outlined text-amber-500" style="font-size:18px">lock</span>
          Freeze Wallet
        </h2>
        <form [formGroup]="freezeForm" (ngSubmit)="freezeWallet()" class="flex gap-3" novalidate>
          <div class="flex-1">
            <input type="text" formControlName="walletId"
                   placeholder="Wallet ID (UUID)"
                   class="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm font-mono outline-none
                          focus:ring-2 focus:ring-indigo-500 focus:border-transparent placeholder:font-sans placeholder:text-slate-400">
          </div>
          <button type="submit" [disabled]="freezeLoading() || freezeForm.invalid"
                  class="flex items-center gap-2 bg-amber-600 hover:bg-amber-700 disabled:opacity-60
                         text-white text-sm font-medium px-4 py-2 rounded-lg transition-colors shrink-0">
            @if (freezeLoading()) { <app-spinner size="xs" /> }
            Freeze
          </button>
        </form>
        @if (frozenWallet()) {
          <div class="mt-4 p-4 bg-amber-50 border border-amber-200 rounded-lg flex items-center gap-3">
            <span class="material-symbols-outlined text-amber-600">lock</span>
            <div class="text-xs text-amber-800">
              <span class="font-semibold">Frozen: </span>
              <span class="font-mono">{{ frozenWallet()!.id }}</span>
              <span class="ml-2 text-amber-600">({{ frozenWallet()!.currency }})</span>
            </div>
          </div>
        }
      </section>

      <!-- Transaction History -->
      <section class="bg-white rounded-xl border border-slate-200 overflow-hidden">
        <div class="px-6 py-4 border-b border-slate-200 flex flex-wrap items-center gap-3">
          <h2 class="text-sm font-semibold text-slate-900 flex items-center gap-2 flex-1">
            <span class="material-symbols-outlined text-indigo-500" style="font-size:18px">receipt_long</span>
            Transaction History
          </h2>
          <form [formGroup]="txForm" (ngSubmit)="loadTx()" class="flex flex-wrap items-end gap-2">
            <div>
              <input type="text" formControlName="walletId"
                     placeholder="Wallet ID (UUID)"
                     class="px-3 py-2 border border-slate-300 rounded-lg text-sm font-mono outline-none
                            focus:ring-2 focus:ring-indigo-500 focus:border-transparent placeholder:font-sans placeholder:text-slate-400 w-64">
            </div>
            <div>
              <select formControlName="direction"
                      class="px-3 py-2 border border-slate-300 rounded-lg text-sm outline-none
                             focus:ring-2 focus:ring-indigo-500 bg-white">
                <option value="">All directions</option>
                <option value="CREDIT">Credits</option>
                <option value="DEBIT">Debits</option>
              </select>
            </div>
            <button type="submit" [disabled]="txLoading() || txForm.invalid"
                    class="flex items-center gap-1.5 bg-slate-800 hover:bg-slate-900 disabled:opacity-60
                           text-white text-sm font-medium px-4 py-2 rounded-lg transition-colors shrink-0">
              @if (txLoading()) { <app-spinner size="xs" /> }
              Load
            </button>
          </form>
        </div>

        @if (txPage()) {
          @if (!txPage()!.content.length) {
            <div class="text-center py-10 text-slate-400">
              <p class="text-sm">No transactions found</p>
            </div>
          } @else {
            <div class="overflow-x-auto">
              <table class="w-full text-sm">
                <thead class="bg-slate-50 border-b border-slate-200">
                  <tr>
                    <th class="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Direction</th>
                    <th class="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Amount</th>
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
        }
      </section>

    </div>
  `,
})
export class WalletsComponent {
  private readonly walletSvc   = inject(WalletService);
  private readonly historySvc  = inject(HistoryService);
  private readonly exchangeSvc = inject(ExchangeService);
  private readonly toast       = inject(ToastService);
  private readonly fb          = inject(FormBuilder);

  readonly currencies     = signal<string[]>([]);
  readonly openLoading    = signal(false);
  readonly balanceLoading = signal(false);
  readonly freezeLoading  = signal(false);
  readonly txLoading      = signal(false);
  readonly newWallet      = signal<WalletView | null>(null);
  readonly balance        = signal<BalanceView | null>(null);
  readonly frozenWallet   = signal<WalletView | null>(null);
  readonly txPage         = signal<Page<TransactionView> | null>(null);
  readonly txCurrentPage  = signal(0);

  private readonly UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

  readonly openForm = this.fb.group({
    accountId: ['', [Validators.required, Validators.pattern(this.UUID_PATTERN)]],
    currency:  ['USD', Validators.required],
  });

  readonly balanceForm = this.fb.group({
    walletId: ['', [Validators.required, Validators.pattern(this.UUID_PATTERN)]],
  });

  readonly freezeForm = this.fb.group({
    walletId: ['', [Validators.required, Validators.pattern(this.UUID_PATTERN)]],
  });

  readonly txForm = this.fb.group({
    walletId:  ['', [Validators.required, Validators.pattern(this.UUID_PATTERN)]],
    direction: [''],
  });

  constructor() {
    this.exchangeSvc.currencies().subscribe(cs => this.currencies.set(cs));
  }

  openWallet(): void {
    if (this.openForm.invalid) return;
    this.openLoading.set(true);
    const { accountId, currency } = this.openForm.value;
    this.walletSvc.open(accountId!, currency!).subscribe({
      next: w => { this.newWallet.set(w); this.toast.success('Wallet opened'); this.openForm.reset({ currency: 'USD' }); },
      error: () => this.openLoading.set(false),
      complete: () => this.openLoading.set(false),
    });
  }

  checkBalance(): void {
    if (this.balanceForm.invalid) return;
    this.balanceLoading.set(true);
    this.walletSvc.balance(this.balanceForm.value.walletId!).subscribe({
      next: b => this.balance.set(b),
      error: () => this.balanceLoading.set(false),
      complete: () => this.balanceLoading.set(false),
    });
  }

  freezeWallet(): void {
    if (this.freezeForm.invalid) return;
    this.freezeLoading.set(true);
    this.walletSvc.freeze(this.freezeForm.value.walletId!).subscribe({
      next: w => { this.frozenWallet.set(w); this.toast.warning(`Wallet frozen`); this.freezeForm.reset(); },
      error: () => this.freezeLoading.set(false),
      complete: () => this.freezeLoading.set(false),
    });
  }

  loadTx(): void {
    if (this.txForm.invalid) return;
    this.txLoading.set(true);
    const { walletId, direction } = this.txForm.value;
    this.historySvc.list(walletId!, this.txCurrentPage(), 20, direction || undefined).subscribe({
      next: p => { this.txPage.set(p); this.txLoading.set(false); },
      error: () => this.txLoading.set(false),
    });
  }

  onTxPageChange(p: number): void { this.txCurrentPage.set(p); this.loadTx(); }

  statusVariant(s: WalletStatus): BadgeVariant { return STATUS_VARIANT[s]; }
  dirVariant(d: Direction): BadgeVariant { return DIR_VARIANT[d]; }
}
