import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { DatePipe, TitleCasePipe } from '@angular/common';
import { ReportService } from '../../core/services/report.service';
import { ToastService } from '../../core/services/toast.service';
import { ReportView } from '../../core/models/api.models';
import { BadgeComponent, BadgeVariant } from '../../shared/badge.component';
import { SpinnerComponent } from '../../shared/spinner.component';

const STATUS_VARIANT: Record<string, BadgeVariant> = {
  PENDING: 'warning',
  READY:   'success',
  FAILED:  'error',
};

const REPORT_TYPES = ['TRANSACTION_SUMMARY', 'KYC_STATUS', 'FRAUD_SUMMARY'];

@Component({
  selector: 'app-reports',
  imports: [ReactiveFormsModule, DatePipe, TitleCasePipe, BadgeComponent, SpinnerComponent],
  template: `
    <div class="space-y-6">

      <!-- Request form -->
      <section class="bg-white rounded-xl border border-slate-200 p-6">
        <h2 class="text-sm font-semibold text-slate-900 mb-4 flex items-center gap-2">
          <span class="material-symbols-outlined text-indigo-500" style="font-size:18px">add_chart</span>
          Request Report
        </h2>
        <form [formGroup]="reportForm" (ngSubmit)="requestReport()" class="space-y-4" novalidate>
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label class="block text-xs font-medium text-slate-500 mb-1.5">Report Type</label>
              <select formControlName="reportType"
                      class="w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm outline-none
                             focus:ring-2 focus:ring-indigo-500 bg-white">
                @for (t of reportTypes; track t) { <option [value]="t">{{ t | titlecase }}</option> }
              </select>
            </div>
            <div>
              <label class="block text-xs font-medium text-slate-500 mb-1.5">Format</label>
              <select formControlName="format"
                      class="w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm outline-none
                             focus:ring-2 focus:ring-indigo-500 bg-white">
                <option value="CSV">CSV</option>
                <option value="PDF">PDF</option>
              </select>
            </div>
            <div>
              <label class="block text-xs font-medium text-slate-500 mb-1.5">From Date</label>
              <input type="date" formControlName="from"
                     class="w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm outline-none
                            focus:ring-2 focus:ring-indigo-500">
            </div>
            <div>
              <label class="block text-xs font-medium text-slate-500 mb-1.5">To Date</label>
              <input type="date" formControlName="to"
                     class="w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm outline-none
                            focus:ring-2 focus:ring-indigo-500">
            </div>
          </div>
          <div class="flex justify-end">
            <button type="submit" [disabled]="requesting() || reportForm.invalid"
                    class="flex items-center gap-2 bg-indigo-600 hover:bg-indigo-700 disabled:opacity-60
                           text-white text-sm font-medium px-5 py-2.5 rounded-lg transition-colors">
              @if (requesting()) { <app-spinner size="xs" /> }
              Request Report
            </button>
          </div>
        </form>
      </section>

      <!-- Reports list -->
      <div class="bg-white rounded-xl border border-slate-200 overflow-hidden">
        <div class="px-5 py-3.5 border-b border-slate-200 flex items-center justify-between">
          <h2 class="text-sm font-semibold text-slate-900">My Reports</h2>
          <button (click)="load()"
                  class="text-xs text-indigo-600 hover:text-indigo-800 font-medium flex items-center gap-1">
            <span class="material-symbols-outlined" style="font-size:14px">refresh</span>
            Refresh
          </button>
        </div>
        @if (loading()) {
          <div class="flex justify-center py-12"><app-spinner /></div>
        } @else if (!reports().length) {
          <div class="text-center py-12 text-slate-400">
            <span class="material-symbols-outlined text-4xl mb-2 block">description</span>
            <p class="text-sm">No reports yet</p>
          </div>
        } @else {
          <div class="overflow-x-auto">
            <table class="w-full text-sm">
              <thead class="bg-slate-50 border-b border-slate-200">
                <tr>
                  <th class="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Type</th>
                  <th class="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Period</th>
                  <th class="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Format</th>
                  <th class="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Status</th>
                  <th class="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Requested</th>
                  <th class="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Actions</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-slate-100">
                @for (r of reports(); track r.id) {
                  <tr class="hover:bg-slate-50 transition-colors">
                    <td class="px-4 py-3 font-medium text-slate-800 text-xs">{{ r.reportType }}</td>
                    <td class="px-4 py-3 text-xs text-slate-500">{{ r.fromDate }} → {{ r.toDate }}</td>
                    <td class="px-4 py-3 text-xs text-slate-500">{{ r.format }}</td>
                    <td class="px-4 py-3">
                      <app-badge [label]="r.status" [variant]="statusVariant(r.status)" />
                    </td>
                    <td class="px-4 py-3 text-xs text-slate-500">{{ r.createdAt | date:'dd MMM, HH:mm' }}</td>
                    <td class="px-4 py-3">
                      @if (r.status === 'READY') {
                        <button (click)="download(r)"
                                class="text-xs font-medium text-indigo-600 hover:text-indigo-800 flex items-center gap-1">
                          <span class="material-symbols-outlined" style="font-size:14px">download</span>
                          Download
                        </button>
                      }
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }
      </div>

    </div>
  `,
})
export class ReportsComponent {
  private readonly reportSvc = inject(ReportService);
  private readonly toast     = inject(ToastService);
  private readonly fb        = inject(FormBuilder);

  readonly reportTypes = REPORT_TYPES;
  readonly loading     = signal(true);
  readonly requesting  = signal(false);
  readonly reports     = signal<ReportView[]>([]);

  readonly reportForm = this.fb.group({
    reportType: ['TRANSACTION_SUMMARY', Validators.required],
    format:     ['CSV', Validators.required],
    from:       ['', Validators.required],
    to:         ['', Validators.required],
  });

  constructor() { this.load(); }

  load(): void {
    this.loading.set(true);
    this.reportSvc.list().subscribe({
      next: rs => { this.reports.set(rs); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  requestReport(): void {
    if (this.reportForm.invalid) { this.reportForm.markAllAsTouched(); return; }
    this.requesting.set(true);
    const { reportType, from, to, format } = this.reportForm.value;
    this.reportSvc.request(reportType!, from!, to!, format!).subscribe({
      next: () => { this.toast.success('Report requested — check back soon'); this.load(); },
      error: () => this.requesting.set(false),
      complete: () => this.requesting.set(false),
    });
  }

  download(r: ReportView): void {
    this.reportSvc.download(r.id).subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `report-${r.id}.${r.format.toLowerCase()}`;
        a.click();
        URL.revokeObjectURL(url);
      },
    });
  }

  statusVariant(s: string): BadgeVariant { return STATUS_VARIANT[s] ?? 'neutral'; }
}
