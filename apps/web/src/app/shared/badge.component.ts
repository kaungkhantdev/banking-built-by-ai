import { Component, computed, input } from '@angular/core';

export type BadgeVariant = 'success' | 'warning' | 'error' | 'info' | 'neutral';

const VARIANTS: Record<BadgeVariant, string> = {
  success: 'bg-emerald-50 text-emerald-700 ring-emerald-600/20',
  warning: 'bg-amber-50  text-amber-700  ring-amber-500/20',
  error:   'bg-red-50    text-red-700    ring-red-600/20',
  info:    'bg-blue-50   text-blue-700   ring-blue-600/20',
  neutral: 'bg-slate-50  text-slate-600  ring-slate-500/10',
};

@Component({
  selector: 'app-badge',
  template: `
    <span class="inline-flex items-center rounded-md px-2 py-0.5 text-xs font-medium ring-1 ring-inset"
          [class]="cls()">
      {{ label() }}
    </span>
  `,
})
export class BadgeComponent {
  readonly label   = input.required<string>();
  readonly variant = input<BadgeVariant>('neutral');
  readonly cls     = computed(() => VARIANTS[this.variant()]);
}
