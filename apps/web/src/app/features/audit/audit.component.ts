import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { DatePipe, SlicePipe } from '@angular/common';
import { AuditService } from '../../core/services/audit.service';
import { AuditRecord, Page } from '../../core/models/api.models';
import { PaginationComponent } from '../../shared/pagination.component';
import { SpinnerComponent } from '../../shared/spinner.component';

@Component({
  selector: 'app-audit',
  imports: [ReactiveFormsModule, DatePipe, SlicePipe, PaginationComponent, SpinnerComponent],
  template: `
    <div class="space-y-4">

      <!-- Search bar -->
      <div class="bg-white rounded-xl border border-slate-200 p-5">
        <form [formGroup]="searchForm" (ngSubmit)="search()" class="flex flex-wrap gap-3">
          <div class="flex-1 min-w-40">
            <label class="block text-xs font-medium text-slate-500 mb-1.5">Actor (email)</label>
            <input type="text" formControlName="actor"
                   placeholder="actor@bank.com"
                   class="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm outline-none
                          focus:ring-2 focus:ring-indigo-500 focus:border-transparent placeholder:text-slate-400">
          </div>
          <div class="flex-1 min-w-40">
            <label class="block text-xs font-medium text-slate-500 mb-1.5">Action</label>
            <input type="text" formControlName="action"
                   placeholder="ACCOUNT_ACTIVATED"
                   class="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm outline-none
                          focus:ring-2 focus:ring-indigo-500 focus:border-transparent placeholder:text-slate-400">
          </div>
          <div class="flex items-end gap-2">
            <button type="submit" [disabled]="loading()"
                    class="flex items-center gap-2 bg-slate-800 hover:bg-slate-900 disabled:opacity-60
                           text-white text-sm font-medium px-4 py-2 rounded-lg transition-colors">
              @if (loading()) { <app-spinner size="xs" /> }
              <span class="material-symbols-outlined" style="font-size:16px">search</span>
              Search
            </button>
            <button type="button" (click)="clear()"
                    class="px-4 py-2 border border-slate-300 text-slate-600 hover:bg-slate-50
                           text-sm font-medium rounded-lg transition-colors">
              Clear
            </button>
          </div>
        </form>
      </div>

      <!-- Results table -->
      <div class="bg-white rounded-xl border border-slate-200 overflow-hidden">
        @if (loading()) {
          <div class="flex justify-center py-16"><app-spinner /></div>
        } @else if (!page()) {
          <div class="text-center py-16 text-slate-400">
            <span class="material-symbols-outlined text-4xl mb-2 block">history</span>
            <p class="text-sm">Use the search above to query the audit trail</p>
          </div>
        } @else if (page()!.content.length === 0) {
          <div class="text-center py-16 text-slate-400">
            <span class="material-symbols-outlined text-4xl mb-2 block">search_off</span>
            <p class="text-sm">No records matched your search</p>
          </div>
        } @else {
          <div class="overflow-x-auto">
            <table class="w-full text-sm">
              <thead class="bg-slate-50 border-b border-slate-200">
                <tr>
                  <th class="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Timestamp</th>
                  <th class="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Actor</th>
                  <th class="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Action</th>
                  <th class="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Trace ID</th>
                  <th class="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Delta</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-slate-100">
                @for (r of page()!.content; track r.id) {
                  <tr class="hover:bg-slate-50 transition-colors">
                    <td class="px-4 py-3 text-xs text-slate-500 whitespace-nowrap">
                      {{ r.at | date:'dd MMM yyyy, HH:mm:ss' }}
                    </td>
                    <td class="px-4 py-3 text-xs font-medium text-slate-700">{{ r.actor }}</td>
                    <td class="px-4 py-3">
                      <span class="inline-flex items-center rounded-md bg-slate-100 px-2 py-0.5
                                   text-xs font-mono font-medium text-slate-700">
                        {{ r.action }}
                      </span>
                    </td>
                    <td class="px-4 py-3 font-mono text-xs text-slate-400">{{ r.traceId | slice:0:12 }}…</td>
                    <td class="px-4 py-3">
                      @if (r.beforeJson || r.afterJson) {
                        <button (click)="toggleDelta(r.id)"
                                class="text-xs text-indigo-600 hover:underline">
                          {{ expanded() === r.id ? 'Hide' : 'View' }}
                        </button>
                      } @else {
                        <span class="text-xs text-slate-300">—</span>
                      }
                    </td>
                  </tr>
                  @if (expanded() === r.id) {
                    <tr class="bg-slate-50 border-b border-slate-100">
                      <td colspan="5" class="px-4 py-4">
                        <div class="grid grid-cols-2 gap-4 text-xs">
                          <div>
                            <p class="font-semibold text-slate-500 mb-1">Before</p>
                            <pre class="bg-white border border-slate-200 rounded-lg p-3 overflow-auto text-slate-700
                                        max-h-32 text-xs leading-relaxed">{{ r.beforeJson || 'null' }}</pre>
                          </div>
                          <div>
                            <p class="font-semibold text-slate-500 mb-1">After</p>
                            <pre class="bg-white border border-slate-200 rounded-lg p-3 overflow-auto text-slate-700
                                        max-h-32 text-xs leading-relaxed">{{ r.afterJson || 'null' }}</pre>
                          </div>
                        </div>
                      </td>
                    </tr>
                  }
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
export class AuditComponent {
  private readonly auditSvc = inject(AuditService);
  private readonly fb       = inject(FormBuilder);

  readonly loading     = signal(false);
  readonly currentPage = signal(0);
  readonly page        = signal<Page<AuditRecord> | null>(null);
  readonly expanded    = signal<string | null>(null);

  readonly searchForm = this.fb.group({
    actor:  [''],
    action: [''],
  });

  search(): void {
    this.currentPage.set(0);
    this.fetch();
  }

  clear(): void {
    this.searchForm.reset();
    this.currentPage.set(0);
    this.page.set(null);
  }

  onPageChange(p: number): void { this.currentPage.set(p); this.fetch(); }

  toggleDelta(id: string): void {
    this.expanded.set(this.expanded() === id ? null : id);
  }

  private fetch(): void {
    this.loading.set(true);
    const { actor, action } = this.searchForm.value;
    this.auditSvc.search(actor || undefined, action || undefined, this.currentPage()).subscribe({
      next: p => { this.page.set(p); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }
}
