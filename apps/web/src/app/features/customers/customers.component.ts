import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { CustomerService } from '../../core/services/customer.service';
import { ToastService } from '../../core/services/toast.service';
import { CustomerView, CustomerStatus, Page } from '../../core/models/api.models';
import { BadgeComponent, BadgeVariant } from '../../shared/badge.component';
import { PaginationComponent } from '../../shared/pagination.component';
import { SpinnerComponent } from '../../shared/spinner.component';

const STATUS_VARIANT: Record<CustomerStatus, BadgeVariant> = {
  ACTIVE:    'success',
  SUSPENDED: 'warning',
  CLOSED:    'neutral',
};

@Component({
  selector: 'app-customers',
  imports: [ReactiveFormsModule, DatePipe, BadgeComponent, PaginationComponent, SpinnerComponent],
  template: `
    <div class="space-y-5">

      <!-- Search toolbar -->
      <div class="flex items-center gap-3">
        <div class="relative flex-1 max-w-sm">
          <span class="absolute inset-y-0 left-3 flex items-center pointer-events-none">
            <span class="material-symbols-outlined text-slate-400" style="font-size:18px">search</span>
          </span>
          <input type="text" placeholder="Search by name or phone…"
                 class="w-full pl-9 pr-3 py-2 border border-slate-300 rounded-lg text-sm outline-none
                        focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                 (input)="onSearch($any($event.target).value)">
        </div>
        @if (loading()) { <app-spinner size="sm" /> }
      </div>

      <!-- Table -->
      <div class="bg-white rounded-xl border border-slate-200 overflow-hidden">
        @if (loading()) {
          <div class="flex justify-center py-16"><app-spinner /></div>
        } @else if (!page()?.content?.length) {
          <div class="text-center py-16 text-slate-400">
            <span class="material-symbols-outlined text-4xl mb-2 block">person_search</span>
            <p class="text-sm">No customers found</p>
          </div>
        } @else {
          <div class="overflow-x-auto">
            <table class="w-full text-sm">
              <thead class="bg-slate-50 border-b border-slate-200">
                <tr>
                  <th class="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Name</th>
                  <th class="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Phone</th>
                  <th class="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">DOB</th>
                  <th class="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Status</th>
                  <th class="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Created</th>
                  <th class="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Actions</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-slate-100">
                @for (c of page()!.content; track c.id) {
                  <tr class="hover:bg-slate-50 transition-colors">
                    <td class="px-4 py-3 font-medium text-slate-900">{{ c.fullName }}</td>
                    <td class="px-4 py-3 text-slate-600">{{ c.phone }}</td>
                    <td class="px-4 py-3 text-slate-500 text-xs">{{ c.dateOfBirth }}</td>
                    <td class="px-4 py-3">
                      <app-badge [label]="c.status" [variant]="statusVariant(c.status)" />
                    </td>
                    <td class="px-4 py-3 text-xs text-slate-500">{{ c.createdAt | date:'dd MMM yyyy' }}</td>
                    <td class="px-4 py-3">
                      <div class="flex items-center gap-3">
                        @if (c.status === 'ACTIVE') {
                          <button (click)="suspend(c)"
                                  class="text-xs font-medium text-amber-600 hover:text-amber-800">
                            Suspend
                          </button>
                        }
                        @if (c.status !== 'CLOSED') {
                          <button (click)="close(c)"
                                  class="text-xs font-medium text-red-600 hover:text-red-800">
                            Close
                          </button>
                        }
                      </div>
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
export class CustomersComponent {
  private readonly customerSvc = inject(CustomerService);
  private readonly toast       = inject(ToastService);

  readonly loading     = signal(true);
  readonly currentPage = signal(0);
  readonly query       = signal('');
  readonly page        = signal<Page<CustomerView> | null>(null);

  private searchTimer: ReturnType<typeof setTimeout> | null = null;

  constructor() { this.load(); }

  private load(): void {
    this.loading.set(true);
    this.customerSvc.list(this.query() || undefined, this.currentPage()).subscribe({
      next: p => { this.page.set(p); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  onSearch(q: string): void {
    if (this.searchTimer) clearTimeout(this.searchTimer);
    this.searchTimer = setTimeout(() => {
      this.query.set(q);
      this.currentPage.set(0);
      this.load();
    }, 300);
  }

  onPageChange(p: number): void { this.currentPage.set(p); this.load(); }

  suspend(c: CustomerView): void {
    this.customerSvc.suspend(c.id).subscribe({
      next: () => { this.toast.warning(`${c.fullName} suspended`); this.load(); },
    });
  }

  close(c: CustomerView): void {
    this.customerSvc.close(c.id).subscribe({
      next: () => { this.toast.success(`${c.fullName} closed`); this.load(); },
    });
  }

  statusVariant(s: CustomerStatus): BadgeVariant { return STATUS_VARIANT[s]; }
}
