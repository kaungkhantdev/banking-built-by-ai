import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { DatePipe, SlicePipe } from '@angular/common';
import { KycService } from '../../core/services/kyc.service';
import { ToastService } from '../../core/services/toast.service';
import { KycCaseView, KycStatus, KycStatusView, Page } from '../../core/models/api.models';
import { BadgeComponent, BadgeVariant } from '../../shared/badge.component';
import { PaginationComponent } from '../../shared/pagination.component';
import { SpinnerComponent } from '../../shared/spinner.component';

const STATUS_VARIANT: Record<KycStatus, BadgeVariant> = {
  CREATED:        'neutral',
  DOCS_SUBMITTED: 'info',
  UNDER_REVIEW:   'warning',
  VERIFIED:       'success',
  REJECTED:       'error',
};

const KYC_STATUSES: (KycStatus | '')[] = ['', 'CREATED', 'DOCS_SUBMITTED', 'UNDER_REVIEW', 'VERIFIED', 'REJECTED'];

@Component({
  selector: 'app-kyc',
  imports: [ReactiveFormsModule, DatePipe, SlicePipe, BadgeComponent, PaginationComponent, SpinnerComponent],
  template: `
    <div class="space-y-5">

      <!-- Toolbar -->
      <div class="flex flex-wrap items-center justify-between gap-3">
        <div class="flex items-center gap-2">
          <label class="text-sm font-medium text-slate-600">Filter by status:</label>
          <select class="px-3 py-2 border border-slate-300 rounded-lg text-sm outline-none
                         focus:ring-2 focus:ring-indigo-500 bg-white"
                  (change)="onStatusFilter($any($event.target).value)">
            @for (s of kycStatuses; track s) {
              <option [value]="s">{{ s || 'All statuses' }}</option>
            }
          </select>
        </div>
        <button (click)="openCaseModal.set(true)"
                class="flex items-center gap-2 bg-indigo-600 hover:bg-indigo-700 text-white
                       text-sm font-medium px-4 py-2 rounded-lg transition-colors">
          <span class="material-symbols-outlined" style="font-size:18px">add</span>
          Open KYC Case
        </button>
      </div>

      <!-- Cases table -->
      <div class="bg-white rounded-xl border border-slate-200 overflow-hidden">
        @if (loading()) {
          <div class="flex justify-center py-16"><app-spinner /></div>
        } @else if (page()?.content?.length === 0) {
          <div class="text-center py-16 text-slate-400">
            <span class="material-symbols-outlined text-4xl mb-2 block">verified_user</span>
            <p class="text-sm">No KYC cases found</p>
          </div>
        } @else {
          <div class="overflow-x-auto">
            <table class="w-full text-sm">
              <thead class="bg-slate-50 border-b border-slate-200">
                <tr>
                  <th class="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Case ID</th>
                  <th class="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Account ID</th>
                  <th class="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Status</th>
                  <th class="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Vendor Ref</th>
                  <th class="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Updated</th>
                  <th class="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Actions</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-slate-100">
                @for (c of page()!.content; track c.id) {
                  <tr class="hover:bg-slate-50 transition-colors">
                    <td class="px-4 py-3 font-mono text-xs text-slate-500">{{ c.id | slice:0:8 }}…</td>
                    <td class="px-4 py-3 font-mono text-xs text-slate-500">{{ c.accountId | slice:0:8 }}…</td>
                    <td class="px-4 py-3">
                      <app-badge [label]="c.status" [variant]="statusVariant(c.status)" />
                    </td>
                    <td class="px-4 py-3 text-xs text-slate-500">{{ c.vendorRef || '—' }}</td>
                    <td class="px-4 py-3 text-xs text-slate-500">{{ c.updatedAt | date:'dd MMM, HH:mm' }}</td>
                    <td class="px-4 py-3">
                      <button (click)="selectCase(c)"
                              class="text-xs font-medium text-indigo-600 hover:text-indigo-800">
                        Upload Docs
                      </button>
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

      <!-- Check status -->
      <section class="bg-white rounded-xl border border-slate-200 p-6">
        <h2 class="text-sm font-semibold text-slate-900 mb-3 flex items-center gap-2">
          <span class="material-symbols-outlined text-indigo-500" style="font-size:18px">search</span>
          Check KYC Status
        </h2>
        <form [formGroup]="statusForm" (ngSubmit)="checkStatus()" class="flex gap-3" novalidate>
          <div class="flex-1">
            <input type="text" formControlName="accountId"
                   placeholder="Account ID (UUID)"
                   class="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm font-mono outline-none
                          focus:ring-2 focus:ring-indigo-500 focus:border-transparent placeholder:font-sans placeholder:text-slate-400">
          </div>
          <button type="submit" [disabled]="statusLoading() || statusForm.invalid"
                  class="flex items-center gap-2 bg-slate-800 hover:bg-slate-900 disabled:opacity-60
                         text-white text-sm font-medium px-4 py-2 rounded-lg transition-colors shrink-0">
            @if (statusLoading()) { <app-spinner size="xs" /> }
            Check
          </button>
        </form>
        @if (kycStatus()) {
          <div class="mt-3 flex items-center gap-3">
            <app-badge [label]="kycStatus()!.status" [variant]="statusVariant(kycStatus()!.status)" />
            <span class="text-xs text-slate-500 font-mono">{{ kycStatus()!.accountId }}</span>
          </div>
        }
      </section>

    </div>

    <!-- Open Case Modal -->
    @if (openCaseModal()) {
      <div class="fixed inset-0 z-50 overflow-y-auto">
        <div class="flex min-h-full items-center justify-center p-4">
          <div class="fixed inset-0 bg-slate-900/60 backdrop-blur-sm" (click)="openCaseModal.set(false)"></div>
          <div class="relative bg-white rounded-2xl shadow-2xl max-w-md w-full p-6 z-10">
            <div class="flex items-center justify-between mb-5">
              <h2 class="text-lg font-semibold text-slate-900">Open KYC Case</h2>
              <button (click)="openCaseModal.set(false)" class="text-slate-400 hover:text-slate-600">
                <span class="material-symbols-outlined">close</span>
              </button>
            </div>
            <form [formGroup]="openCaseForm" (ngSubmit)="openCase()" novalidate>
              <label class="block text-sm font-medium text-slate-700 mb-1.5">Account ID</label>
              <input type="text" formControlName="accountId"
                     placeholder="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
                     class="w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm font-mono outline-none
                            focus:ring-2 focus:ring-indigo-500 focus:border-transparent placeholder:font-sans placeholder:text-slate-400 mb-4">
              <div class="flex gap-3">
                <button type="button" (click)="openCaseModal.set(false)"
                        class="flex-1 px-4 py-2.5 border border-slate-300 text-slate-700 hover:bg-slate-50
                               rounded-lg text-sm font-medium transition-colors">Cancel</button>
                <button type="submit" [disabled]="caseLoading() || openCaseForm.invalid"
                        class="flex-1 flex items-center justify-center gap-2 bg-indigo-600 hover:bg-indigo-700
                               disabled:opacity-60 text-white font-medium py-2.5 px-4 rounded-lg text-sm transition-colors">
                  @if (caseLoading()) { <app-spinner size="xs" /> }
                  Open Case
                </button>
              </div>
            </form>
          </div>
        </div>
      </div>
    }

    <!-- Upload Docs Modal -->
    @if (selectedCase()) {
      <div class="fixed inset-0 z-50 overflow-y-auto">
        <div class="flex min-h-full items-center justify-center p-4">
          <div class="fixed inset-0 bg-slate-900/60 backdrop-blur-sm" (click)="selectedCase.set(null)"></div>
          <div class="relative bg-white rounded-2xl shadow-2xl max-w-md w-full p-6 z-10">
            <div class="flex items-center justify-between mb-5">
              <h2 class="text-lg font-semibold text-slate-900">Upload KYC Document</h2>
              <button (click)="selectedCase.set(null)" class="text-slate-400 hover:text-slate-600">
                <span class="material-symbols-outlined">close</span>
              </button>
            </div>
            <p class="text-xs text-slate-500 font-mono mb-4">Account: {{ selectedCase()!.accountId }}</p>
            <div class="border-2 border-dashed border-slate-300 rounded-xl p-6 text-center mb-4">
              <span class="material-symbols-outlined text-3xl text-slate-300 mb-2 block">upload_file</span>
              <p class="text-sm text-slate-500 mb-3">PDF, PNG, JPG up to 10MB</p>
              <input #fileInput type="file" accept=".pdf,.png,.jpg,.jpeg" class="hidden"
                     (change)="onFileSelect($any($event.target).files?.[0])">
              <button type="button" (click)="fileInput.click()"
                      class="text-sm font-medium text-indigo-600 hover:underline">
                Browse files
              </button>
              @if (selectedFile()) {
                <p class="mt-2 text-xs text-emerald-600 font-medium">{{ selectedFile()!.name }}</p>
              }
            </div>
            <div class="flex gap-3">
              <button type="button" (click)="selectedCase.set(null)"
                      class="flex-1 px-4 py-2.5 border border-slate-300 text-slate-700 hover:bg-slate-50
                             rounded-lg text-sm font-medium transition-colors">Cancel</button>
              <button type="button" [disabled]="uploadLoading() || !selectedFile()"
                      (click)="uploadDoc()"
                      class="flex-1 flex items-center justify-center gap-2 bg-indigo-600 hover:bg-indigo-700
                             disabled:opacity-60 text-white font-medium py-2.5 px-4 rounded-lg text-sm transition-colors">
                @if (uploadLoading()) { <app-spinner size="xs" /> }
                Upload
              </button>
            </div>
          </div>
        </div>
      </div>
    }
  `,
})
export class KycComponent {
  private readonly kycSvc = inject(KycService);
  private readonly toast  = inject(ToastService);
  private readonly fb     = inject(FormBuilder);

  readonly kycStatuses  = KYC_STATUSES;
  readonly loading      = signal(true);
  readonly caseLoading  = signal(false);
  readonly statusLoading= signal(false);
  readonly uploadLoading= signal(false);
  readonly openCaseModal= signal(false);
  readonly currentPage  = signal(0);
  readonly statusFilter = signal<KycStatus | undefined>(undefined);
  readonly page         = signal<Page<KycCaseView> | null>(null);
  readonly kycStatus    = signal<KycStatusView | null>(null);
  readonly selectedCase = signal<KycCaseView | null>(null);
  readonly selectedFile = signal<File | null>(null);

  private readonly UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

  readonly openCaseForm = this.fb.group({
    accountId: ['', [Validators.required, Validators.pattern(this.UUID_PATTERN)]],
  });

  readonly statusForm = this.fb.group({
    accountId: ['', [Validators.required, Validators.pattern(this.UUID_PATTERN)]],
  });

  constructor() { this.load(); }

  private load(): void {
    this.loading.set(true);
    this.kycSvc.list(this.statusFilter(), this.currentPage()).subscribe({
      next: p => { this.page.set(p); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  onStatusFilter(value: string): void {
    this.statusFilter.set(value ? value as KycStatus : undefined);
    this.currentPage.set(0);
    this.load();
  }

  onPageChange(p: number): void { this.currentPage.set(p); this.load(); }

  openCase(): void {
    if (this.openCaseForm.invalid) return;
    this.caseLoading.set(true);
    this.kycSvc.openCase(this.openCaseForm.value.accountId!).subscribe({
      next: () => {
        this.toast.success('KYC case opened');
        this.openCaseModal.set(false);
        this.openCaseForm.reset();
        this.load();
      },
      error: () => this.caseLoading.set(false),
      complete: () => this.caseLoading.set(false),
    });
  }

  checkStatus(): void {
    if (this.statusForm.invalid) return;
    this.statusLoading.set(true);
    this.kycSvc.status(this.statusForm.value.accountId!).subscribe({
      next: s => this.kycStatus.set(s),
      error: () => this.statusLoading.set(false),
      complete: () => this.statusLoading.set(false),
    });
  }

  selectCase(c: KycCaseView): void { this.selectedCase.set(c); this.selectedFile.set(null); }

  onFileSelect(file: File | undefined): void { if (file) this.selectedFile.set(file); }

  uploadDoc(): void {
    const kase = this.selectedCase();
    const file = this.selectedFile();
    if (!kase || !file) return;
    this.uploadLoading.set(true);
    this.kycSvc.submitDocument(kase.accountId, file).subscribe({
      next: () => {
        this.toast.success('Document uploaded');
        this.selectedCase.set(null);
        this.selectedFile.set(null);
        this.load();
      },
      error: () => this.uploadLoading.set(false),
      complete: () => this.uploadLoading.set(false),
    });
  }

  statusVariant(s: KycStatus): BadgeVariant { return STATUS_VARIANT[s]; }
}
