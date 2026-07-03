import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { ExchangeService } from '../../core/services/exchange.service';
import { ExchangeRateView } from '../../core/models/api.models';
import { SpinnerComponent } from '../../shared/spinner.component';

@Component({
  selector: 'app-exchange',
  imports: [ReactiveFormsModule, DatePipe, SpinnerComponent],
  template: `
    <div class="space-y-6 max-w-lg">

      <!-- Rate lookup -->
      <section class="bg-white rounded-xl border border-slate-200 p-6">
        <h2 class="text-sm font-semibold text-slate-900 mb-4 flex items-center gap-2">
          <span class="material-symbols-outlined text-indigo-500" style="font-size:18px">currency_exchange</span>
          Look Up Exchange Rate
        </h2>
        <form [formGroup]="rateForm" (ngSubmit)="lookup()" class="space-y-4" novalidate>
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-xs font-medium text-slate-500 mb-1.5">From</label>
              <select formControlName="from"
                      class="w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm outline-none
                             focus:ring-2 focus:ring-indigo-500 bg-white">
                @for (c of currencies(); track c) { <option [value]="c">{{ c }}</option> }
              </select>
            </div>
            <div>
              <label class="block text-xs font-medium text-slate-500 mb-1.5">To</label>
              <select formControlName="to"
                      class="w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm outline-none
                             focus:ring-2 focus:ring-indigo-500 bg-white">
                @for (c of currencies(); track c) { <option [value]="c">{{ c }}</option> }
              </select>
            </div>
          </div>
          <button type="submit" [disabled]="loading()"
                  class="w-full flex items-center justify-center gap-2 bg-indigo-600 hover:bg-indigo-700
                         disabled:opacity-60 text-white font-medium py-2.5 rounded-lg text-sm transition-colors">
            @if (loading()) { <app-spinner size="xs" /> }
            Get Rate
          </button>
        </form>

        @if (rate()) {
          <div class="mt-5 p-5 bg-indigo-50 border border-indigo-100 rounded-xl">
            <p class="text-xs text-indigo-500 font-medium uppercase tracking-wide mb-2">Current Rate</p>
            <p class="text-3xl font-bold text-indigo-900">
              1 {{ rate()!.fromCurrency }}
              <span class="text-indigo-400 mx-2">=</span>
              {{ rate()!.rate }} {{ rate()!.toCurrency }}
            </p>
            <p class="mt-2 text-xs text-indigo-400">
              Effective: {{ rate()!.effectiveAt | date:'dd MMM yyyy, HH:mm' }}
            </p>
          </div>
        }

        @if (error()) {
          <div class="mt-4 p-4 bg-red-50 border border-red-200 rounded-lg">
            <p class="text-sm text-red-700">{{ error() }}</p>
          </div>
        }
      </section>

    </div>
  `,
})
export class ExchangeComponent {
  private readonly exchangeSvc = inject(ExchangeService);
  private readonly fb          = inject(FormBuilder);

  readonly currencies    = signal<string[]>([]);
  readonly loading       = signal(false);
  readonly rate          = signal<ExchangeRateView | null>(null);
  readonly error         = signal<string | null>(null);

  readonly rateForm = this.fb.group({
    from: ['USD', Validators.required],
    to:   ['EUR', Validators.required],
  });

  constructor() {
    this.exchangeSvc.currencies().subscribe(cs => this.currencies.set(cs));
  }

  lookup(): void {
    if (this.rateForm.invalid) return;
    this.loading.set(true);
    this.error.set(null);
    this.rate.set(null);
    const { from, to } = this.rateForm.value;
    this.exchangeSvc.rate(from!, to!).subscribe({
      next: r => this.rate.set(r),
      error: () => { this.error.set('Rate not available for this pair.'); this.loading.set(false); },
      complete: () => this.loading.set(false),
    });
  }
}
