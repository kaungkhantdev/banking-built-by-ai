import { Component, computed, input, output } from '@angular/core';

@Component({
  selector: 'app-pagination',
  template: `
    <div class="flex items-center justify-between border-t border-slate-200 bg-white px-4 py-3">
      <p class="text-sm text-slate-500">
        @if (totalElements() === 0) {
          No results
        } @else {
          Showing <span class="font-medium">{{ start() }}</span>–<span class="font-medium">{{ end() }}</span>
          of <span class="font-medium">{{ totalElements() }}</span> results
        }
      </p>
      <div class="flex items-center gap-1">
        <button
          class="px-3 py-1.5 text-sm rounded-lg border border-slate-200 hover:bg-slate-50 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
          [disabled]="page() === 0"
          (click)="prev()">
          Previous
        </button>
        <span class="px-3 py-1.5 text-sm text-slate-600">
          {{ page() + 1 }} / {{ totalPages() || 1 }}
        </span>
        <button
          class="px-3 py-1.5 text-sm rounded-lg border border-slate-200 hover:bg-slate-50 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
          [disabled]="page() + 1 >= totalPages()"
          (click)="next()">
          Next
        </button>
      </div>
    </div>
  `,
})
export class PaginationComponent {
  readonly page          = input.required<number>();
  readonly totalElements = input.required<number>();
  readonly totalPages    = input.required<number>();
  readonly size          = input<number>(20);
  readonly pageChange    = output<number>();

  readonly start = computed(() => this.totalElements() === 0 ? 0 : this.page() * this.size() + 1);
  readonly end   = computed(() => Math.min((this.page() + 1) * this.size(), this.totalElements()));

  prev() { this.pageChange.emit(this.page() - 1); }
  next() { this.pageChange.emit(this.page() + 1); }
}
