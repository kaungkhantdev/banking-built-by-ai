import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TransferService } from '../../core/services/transfer.service';
import { ToastService } from '../../core/services/toast.service';
import { TransferResult } from '../../core/models/api.models';
import { SpinnerComponent } from '../../shared/spinner.component';
import { BadgeComponent } from '../../shared/badge.component';

@Component({
  selector: 'app-transfers',
  imports: [ReactiveFormsModule, SpinnerComponent, BadgeComponent],
  template: `
    <div class="space-y-6">

      <!-- New transfer -->
      <section class="bg-white rounded-xl border border-slate-200 p-6">
        <h2 class="text-sm font-semibold text-slate-900 mb-4 flex items-center gap-2">
          <span class="material-symbols-outlined text-indigo-500" style="font-size:18px">swap_horiz</span>
          New Transfer
        </h2>
        <form [formGroup]="transferForm" (ngSubmit)="submit()" class="space-y-4" novalidate>
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label class="block text-xs font-medium text-slate-500 mb-1.5">From Wallet ID</label>
              <input type="text" formControlName="fromWalletId"
                     placeholder="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
                     class="w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm font-mono outline-none
                            focus:ring-2 focus:ring-indigo-500 focus:border-transparent placeholder:font-sans placeholder:text-slate-400">
              @if (f['fromWalletId'].touched && f['fromWalletId'].invalid) {
                <p class="mt-1 text-xs text-red-600">Valid UUID required</p>
              }
            </div>
            <div>
              <label class="block text-xs font-medium text-slate-500 mb-1.5">To Wallet ID</label>
              <input type="text" formControlName="toWalletId"
                     placeholder="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
                     class="w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm font-mono outline-none
                            focus:ring-2 focus:ring-indigo-500 focus:border-transparent placeholder:font-sans placeholder:text-slate-400">
              @if (f['toWalletId'].touched && f['toWalletId'].invalid) {
                <p class="mt-1 text-xs text-red-600">Valid UUID required</p>
              }
            </div>
            <div>
              <label class="block text-xs font-medium text-slate-500 mb-1.5">Amount</label>
              <input type="number" formControlName="amount" min="0.01" step="0.01"
                     placeholder="0.00"
                     class="w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm outline-none
                            focus:ring-2 focus:ring-indigo-500 focus:border-transparent placeholder:text-slate-400">
              @if (f['amount'].touched && f['amount'].invalid) {
                <p class="mt-1 text-xs text-red-600">Positive amount required</p>
              }
            </div>
            <div>
              <label class="block text-xs font-medium text-slate-500 mb-1.5">Memo <span class="text-slate-400">(optional)</span></label>
              <input type="text" formControlName="memo"
                     placeholder="Purpose of transfer"
                     class="w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm outline-none
                            focus:ring-2 focus:ring-indigo-500 focus:border-transparent placeholder:text-slate-400">
            </div>
          </div>
          <div class="flex justify-end pt-2">
            <button type="submit" [disabled]="loading() || transferForm.invalid"
                    class="flex items-center gap-2 bg-indigo-600 hover:bg-indigo-700 disabled:opacity-60
                           text-white text-sm font-medium px-6 py-2.5 rounded-lg transition-colors">
              @if (loading()) { <app-spinner size="xs" /> }
              <span class="material-symbols-outlined" style="font-size:16px">send</span>
              Submit Transfer
            </button>
          </div>
        </form>

        @if (lastResult()) {
          <div class="mt-5 p-4 bg-emerald-50 border border-emerald-200 rounded-lg">
            <div class="flex items-center gap-2 mb-2">
              <span class="material-symbols-outlined text-emerald-600" style="font-size:18px">check_circle</span>
              <p class="text-sm font-semibold text-emerald-800">
                Transfer {{ lastResult()!.replayed ? 'replayed (idempotent)' : 'posted' }}
              </p>
              <app-badge [label]="lastResult()!.status" variant="success" />
            </div>
            <p class="text-xs text-emerald-700 font-mono">
              Transaction ID: {{ lastResult()!.transactionId }}
            </p>
          </div>
        }
      </section>

      <!-- Reverse transfer -->
      <section class="bg-white rounded-xl border border-slate-200 p-6">
        <h2 class="text-sm font-semibold text-slate-900 mb-4 flex items-center gap-2">
          <span class="material-symbols-outlined text-amber-500" style="font-size:18px">undo</span>
          Reverse Transfer
        </h2>
        <form [formGroup]="reverseForm" (ngSubmit)="reverse()" class="flex flex-wrap gap-3" novalidate>
          <div class="flex-1 min-w-64">
            <label class="block text-xs font-medium text-slate-500 mb-1.5">Transaction ID</label>
            <input type="text" formControlName="transactionId"
                   placeholder="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
                   class="w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm font-mono outline-none
                          focus:ring-2 focus:ring-indigo-500 focus:border-transparent placeholder:font-sans placeholder:text-slate-400">
          </div>
          <div class="flex-1 min-w-48">
            <label class="block text-xs font-medium text-slate-500 mb-1.5">Reason</label>
            <input type="text" formControlName="reason"
                   placeholder="operator-initiated"
                   class="w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm outline-none
                          focus:ring-2 focus:ring-indigo-500 focus:border-transparent placeholder:text-slate-400">
          </div>
          <div class="flex items-end">
            <button type="submit" [disabled]="reverseLoading() || reverseForm.invalid"
                    class="flex items-center gap-2 bg-amber-600 hover:bg-amber-700 disabled:opacity-60
                           text-white text-sm font-medium px-4 py-2.5 rounded-lg transition-colors">
              @if (reverseLoading()) { <app-spinner size="xs" /> }
              Reverse
            </button>
          </div>
        </form>

        @if (reversalResult()) {
          <div class="mt-4 p-4 bg-amber-50 border border-amber-200 rounded-lg">
            <p class="text-xs font-semibold text-amber-800 mb-1">Transfer reversed</p>
            <p class="text-xs text-amber-700 font-mono">{{ reversalResult()!.transactionId }}</p>
          </div>
        }
      </section>

    </div>
  `,
})
export class TransfersComponent {
  private readonly transferSvc = inject(TransferService);
  private readonly toast       = inject(ToastService);
  private readonly fb          = inject(FormBuilder);

  readonly loading        = signal(false);
  readonly reverseLoading = signal(false);
  readonly lastResult     = signal<TransferResult | null>(null);
  readonly reversalResult = signal<TransferResult | null>(null);

  private readonly UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

  readonly transferForm = this.fb.group({
    fromWalletId: ['', [Validators.required, Validators.pattern(this.UUID_PATTERN)]],
    toWalletId:   ['', [Validators.required, Validators.pattern(this.UUID_PATTERN)]],
    amount:       [null as number | null, [Validators.required, Validators.min(0.01)]],
    memo:         [''],
  });

  readonly reverseForm = this.fb.group({
    transactionId: ['', [Validators.required, Validators.pattern(this.UUID_PATTERN)]],
    reason:        ['operator-initiated'],
  });

  get f() { return this.transferForm.controls; }

  submit(): void {
    if (this.transferForm.invalid) { this.transferForm.markAllAsTouched(); return; }
    this.loading.set(true);
    const v = this.transferForm.value;
    this.transferSvc.transfer(v.fromWalletId!, v.toWalletId!, v.amount!, v.memo || undefined).subscribe({
      next: r => { this.lastResult.set(r); this.toast.success('Transfer posted'); this.transferForm.reset(); },
      error: () => this.loading.set(false),
      complete: () => this.loading.set(false),
    });
  }

  reverse(): void {
    if (this.reverseForm.invalid) return;
    this.reverseLoading.set(true);
    const { transactionId, reason } = this.reverseForm.value;
    this.transferSvc.reverse(transactionId!, reason || 'operator-initiated').subscribe({
      next: r => { this.reversalResult.set(r); this.toast.warning('Transfer reversed'); this.reverseForm.reset({ reason: 'operator-initiated' }); },
      error: () => this.reverseLoading.set(false),
      complete: () => this.reverseLoading.set(false),
    });
  }
}
