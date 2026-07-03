import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { DatePipe, SlicePipe } from '@angular/common';
import { AccountService } from '../../core/services/account.service';
import { ToastService } from '../../core/services/toast.service';
import { AccountListItem, AccountStatus, Page } from '../../core/models/api.models';
import { BadgeComponent, BadgeVariant } from '../../shared/badge.component';
import { PaginationComponent } from '../../shared/pagination.component';
import { SpinnerComponent } from '../../shared/spinner.component';

const STATUS_VARIANT: Record<AccountStatus, BadgeVariant> = {
  PENDING: 'warning',
  ACTIVE:  'success',
  FROZEN:  'error',
  CLOSED:  'neutral',
};

@Component({
  selector: 'app-accounts',
  imports: [ReactiveFormsModule, DatePipe, SlicePipe, BadgeComponent, PaginationComponent, SpinnerComponent],
  template: `
    <div class="space-y-4">

      <!-- Header row -->
      <div class="flex items-center justify-between">
        <div>
          <p class="text-sm text-slate-500">Manage bank accounts and their lifecycle</p>
        </div>
        <button (click)="openModal.set(true)"
                class="flex items-center gap-2 bg-indigo-600 hover:bg-indigo-700 text-white
                       text-sm font-medium px-4 py-2 rounded-lg transition-colors">
          <span class="material-symbols-outlined" style="font-size:18px">add</span>
          Open Account
        </button>
      </div>

      <!-- Table card -->
      <div class="bg-white rounded-xl border border-slate-200 overflow-hidden">
        @if (loading()) {
          <div class="flex justify-center py-16"><app-spinner /></div>
        } @else if (page()?.content?.length === 0) {
          <div class="text-center py-16 text-slate-400">
            <span class="material-symbols-outlined text-4xl mb-2 block">account_circle</span>
            <p class="text-sm">No accounts yet</p>
          </div>
        } @else {
          <div class="overflow-x-auto">
            <table class="w-full text-sm">
              <thead class="bg-slate-50 border-b border-slate-200">
                <tr>
                  <th class="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">ID</th>
                  <th class="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Owner</th>
                  <th class="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Status</th>
                  <th class="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Created</th>
                  <th class="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Actions</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-slate-100">
                @for (acc of page()!.content; track acc.id) {
                  <tr class="hover:bg-slate-50 transition-colors">
                    <td class="px-4 py-3 font-mono text-xs text-slate-500">{{ acc.id | slice:0:8 }}…</td>
                    <td class="px-4 py-3">
                      <div class="font-medium text-slate-900">{{ acc.ownerEmail }}</div>
                      <div class="text-xs text-slate-400 font-mono mt-0.5">{{ acc.ownerUserId | slice:0:8 }}…</div>
                    </td>
                    <td class="px-4 py-3">
                      <app-badge [label]="acc.status" [variant]="statusVariant(acc.status)" />
                    </td>
                    <td class="px-4 py-3 text-slate-500 text-xs">{{ acc.createdAt | date:'dd MMM yyyy, HH:mm' }}</td>
                    <td class="px-4 py-3">
                      @if (acc.status === 'PENDING') {
                        <button (click)="activate(acc)"
                                [disabled]="activating() === acc.id"
                                class="text-xs font-medium text-indigo-600 hover:text-indigo-800
                                       disabled:opacity-50 flex items-center gap-1">
                          @if (activating() === acc.id) { <app-spinner size="xs" /> }
                          Activate
                        </button>
                      }
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
          <app-pagination
            [page]="currentPage()"
            [totalElements]="page()!.totalElements"
            [totalPages]="page()!.totalPages"
            (pageChange)="onPageChange($event)" />
        }
      </div>

    </div>

    <!-- ── New Account Modal ── -->
    @if (openModal()) {
      <div class="fixed inset-0 z-50 overflow-y-auto">
        <div class="flex min-h-full items-center justify-center p-4">
          <div class="fixed inset-0 bg-slate-900/60 backdrop-blur-sm"
               (click)="closeModal()"></div>
          <div class="relative bg-white rounded-2xl shadow-2xl max-w-md w-full p-6 z-10">
            <div class="flex items-center justify-between mb-5">
              <h2 class="text-lg font-semibold text-slate-900">Open New Account</h2>
              <button (click)="closeModal()" class="text-slate-400 hover:text-slate-600">
                <span class="material-symbols-outlined">close</span>
              </button>
            </div>

            <form [formGroup]="accountForm" (ngSubmit)="createAccount()" class="space-y-4" novalidate>
              <div>
                <label class="block text-sm font-medium text-slate-700 mb-1.5">
                  Owner User ID <span class="text-slate-400 font-normal">(UUID)</span>
                </label>
                <input type="text" formControlName="ownerUserId"
                       placeholder="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
                       class="w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm font-mono
                              outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent
                              placeholder:text-slate-400 placeholder:font-sans">
                @if (accountForm.get('ownerUserId')?.invalid && accountForm.get('ownerUserId')?.touched) {
                  <p class="mt-1 text-xs text-red-600">Valid UUID required</p>
                }
              </div>

              <div class="flex gap-3 pt-2">
                <button type="button" (click)="closeModal()"
                        class="flex-1 px-4 py-2.5 border border-slate-300 text-slate-700
                               hover:bg-slate-50 rounded-lg text-sm font-medium transition-colors">
                  Cancel
                </button>
                <button type="submit" [disabled]="submitting() || accountForm.invalid"
                        class="flex-1 flex items-center justify-center gap-2 bg-indigo-600
                               hover:bg-indigo-700 disabled:opacity-60 text-white font-medium
                               py-2.5 px-4 rounded-lg text-sm transition-colors">
                  @if (submitting()) { <app-spinner size="xs" /> }
                  Open Account
                </button>
              </div>
            </form>
          </div>
        </div>
      </div>
    }
  `,
})
export class AccountsComponent {
  private readonly accountSvc = inject(AccountService);
  private readonly toast      = inject(ToastService);
  private readonly fb         = inject(FormBuilder);

  readonly loading     = signal(true);
  readonly submitting  = signal(false);
  readonly activating  = signal<string | null>(null);
  readonly openModal   = signal(false);
  readonly currentPage = signal(0);
  readonly page        = signal<Page<AccountListItem> | null>(null);

  readonly accountForm = this.fb.group({
    ownerUserId: ['', [Validators.required,
      Validators.pattern(/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i)]],
  });

  constructor() { this.load(); }

  private load(): void {
    this.loading.set(true);
    this.accountSvc.list(this.currentPage()).subscribe({
      next: p => { this.page.set(p); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  onPageChange(p: number): void { this.currentPage.set(p); this.load(); }

  createAccount(): void {
    if (this.accountForm.invalid) { this.accountForm.markAllAsTouched(); return; }
    this.submitting.set(true);
    this.accountSvc.open(this.accountForm.value.ownerUserId!).subscribe({
      next: () => {
        this.toast.success('Account opened successfully');
        this.closeModal();
        this.load();
      },
      error: () => this.submitting.set(false),
    });
  }

  activate(acc: AccountListItem): void {
    this.activating.set(acc.id);
    this.accountSvc.activate(acc.id).subscribe({
      next: () => { this.toast.success('Account activated'); this.load(); },
      error: () => this.activating.set(null),
      complete: () => this.activating.set(null),
    });
  }

  closeModal(): void {
    this.openModal.set(false);
    this.accountForm.reset();
    this.submitting.set(false);
  }

  statusVariant(s: AccountStatus): BadgeVariant { return STATUS_VARIANT[s]; }
}
