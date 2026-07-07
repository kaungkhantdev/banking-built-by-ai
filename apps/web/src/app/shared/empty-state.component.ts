import { Component, input } from '@angular/core';

/**
 * Consistent empty-state placeholder for lists/tables with no rows.
 * Usage: <app-empty-state icon="receipt_long" message="No transactions found"
 *                         hint="Try a different wallet or filter." />
 */
@Component({
  selector: 'app-empty-state',
  template: `
    <div class="flex flex-col items-center justify-center text-center py-12 px-6">
      <span class="material-symbols-outlined text-slate-300" style="font-size:44px">{{ icon() }}</span>
      <p class="mt-3 text-sm font-medium text-slate-500">{{ message() }}</p>
      @if (hint()) {
        <p class="mt-1 text-xs text-slate-400 max-w-sm">{{ hint() }}</p>
      }
    </div>
  `,
})
export class EmptyStateComponent {
  readonly icon = input('inbox');
  readonly message = input('Nothing here yet');
  readonly hint = input<string | null>(null);
}
