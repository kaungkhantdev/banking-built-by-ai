import { Component, computed, input } from '@angular/core';

type Size = 'xs' | 'sm' | 'md' | 'lg';

const SIZES: Record<Size, string> = {
  xs: 'h-3 w-3',
  sm: 'h-4 w-4',
  md: 'h-6 w-6',
  lg: 'h-10 w-10',
};

@Component({
  selector: 'app-spinner',
  template: `
    <svg class="animate-spin text-indigo-600" [class]="cls()"
         xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
      <circle class="opacity-25" cx="12" cy="12" r="10"
              stroke="currentColor" stroke-width="4" />
      <path class="opacity-75" fill="currentColor"
            d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
    </svg>
  `,
})
export class SpinnerComponent {
  readonly size = input<Size>('md');
  readonly cls  = computed(() => SIZES[this.size()]);
}
