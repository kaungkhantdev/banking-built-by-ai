import { Component, inject } from '@angular/core';
import { ToastService } from '../core/services/toast.service';

const TOAST_STYLES: Record<string, string> = {
  success: 'bg-emerald-700',
  error:   'bg-red-700',
  warning: 'bg-amber-600',
  info:    'bg-slate-800',
};

const TOAST_ICONS: Record<string, string> = {
  success: 'check_circle',
  error:   'error',
  warning: 'warning',
  info:    'info',
};

@Component({
  selector: 'app-toast',
  template: `
    <div class="fixed top-4 right-4 z-[9999] flex flex-col gap-2 pointer-events-none w-80">
      @for (t of toasts.toasts(); track t.id) {
        <div class="pointer-events-auto flex items-start gap-2.5 rounded-xl px-4 py-3 shadow-lg text-sm text-white"
             [class]="style(t.type)">
          <span class="material-symbols-outlined text-base shrink-0 mt-0.5">{{ icon(t.type) }}</span>
          <span class="flex-1 leading-snug">{{ t.message }}</span>
          <button class="opacity-70 hover:opacity-100 shrink-0" (click)="toasts.dismiss(t.id)">
            <span class="material-symbols-outlined text-sm">close</span>
          </button>
        </div>
      }
    </div>
  `,
})
export class ToastComponent {
  protected readonly toasts = inject(ToastService);
  style(type: string) { return TOAST_STYLES[type] ?? TOAST_STYLES['info']; }
  icon(type: string)  { return TOAST_ICONS[type]  ?? TOAST_ICONS['info'];  }
}
