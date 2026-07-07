import {
  Component,
  ElementRef,
  HostListener,
  computed,
  forwardRef,
  inject,
  input,
  signal,
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

export interface SelectOption {
  value: string;
  label: string;
  sublabel?: string;
}

/**
 * A searchable single-select dropdown that plugs into reactive forms
 * (implements ControlValueAccessor). The form value is `option.value`.
 */
@Component({
  selector: 'app-searchable-select',
  providers: [
    { provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => SearchableSelectComponent), multi: true },
  ],
  template: `
    <div class="relative">
      <input type="text"
             [value]="display()"
             [placeholder]="placeholder()"
             [disabled]="disabled()"
             (focus)="onFocus($event)"
             (input)="onInput($event)"
             (keydown)="onKeydown($event)"
             (blur)="onTouched()"
             autocomplete="off"
             [class]="inputClass()">

      @if (open() && filtered().length) {
        <ul class="absolute z-20 mt-1 w-full max-h-60 overflow-auto rounded-lg border border-slate-200
                   bg-white py-1 shadow-lg">
          @for (opt of filtered(); track opt.value; let i = $index) {
            <li (mousedown)="select(opt); $event.preventDefault()"
                [class]="'cursor-pointer px-3 py-2 text-sm ' +
                         (i === highlighted() ? 'bg-indigo-50' : 'hover:bg-slate-50')">
              <div class="text-slate-800">{{ opt.label }}</div>
              @if (opt.sublabel) {
                <div class="text-xs text-slate-400 font-mono">{{ opt.sublabel }}</div>
              }
            </li>
          }
        </ul>
      }

      @if (open() && !filtered().length) {
        <div class="absolute z-20 mt-1 w-full rounded-lg border border-slate-200 bg-white
                    px-3 py-2 text-sm text-slate-400 shadow-lg">
          No matches
        </div>
      }
    </div>
  `,
})
export class SearchableSelectComponent implements ControlValueAccessor {
  private readonly host = inject(ElementRef<HTMLElement>);

  readonly options     = input<SelectOption[]>([]);
  readonly placeholder = input('Search…');
  readonly mono        = input(false);

  readonly open        = signal(false);
  readonly query       = signal('');
  readonly highlighted = signal(0);
  readonly disabled    = signal(false);

  private readonly value = signal<string | null>(null);

  private readonly selectedLabel = computed(() => {
    const v = this.value();
    return this.options().find(o => o.value === v)?.label ?? '';
  });

  /** Shows the live query while searching, the selected label otherwise. */
  readonly display = computed(() => (this.open() ? this.query() : this.selectedLabel()));

  readonly filtered = computed(() => {
    const q = this.query().trim().toLowerCase();
    if (!q) return this.options();
    return this.options().filter(o =>
      o.label.toLowerCase().includes(q) || (o.sublabel?.toLowerCase().includes(q) ?? false));
  });

  readonly inputClass = computed(() =>
    'w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm outline-none ' +
    'focus:ring-2 focus:ring-indigo-500 focus:border-transparent placeholder:text-slate-400 ' +
    'disabled:bg-slate-50 disabled:text-slate-400 ' + (this.mono() ? 'font-mono' : ''));

  onFocus(e: FocusEvent): void {
    if (this.disabled()) return;
    this.query.set('');
    this.highlighted.set(0);
    this.open.set(true);
    (e.target as HTMLInputElement).select();
  }

  onInput(e: Event): void {
    this.query.set((e.target as HTMLInputElement).value);
    this.highlighted.set(0);
    this.open.set(true);
  }

  onKeydown(e: KeyboardEvent): void {
    if (!this.open()) return;
    const items = this.filtered();
    switch (e.key) {
      case 'ArrowDown':
        e.preventDefault();
        this.highlighted.update(i => Math.min(i + 1, items.length - 1));
        break;
      case 'ArrowUp':
        e.preventDefault();
        this.highlighted.update(i => Math.max(i - 1, 0));
        break;
      case 'Enter':
        e.preventDefault();
        if (items[this.highlighted()]) this.select(items[this.highlighted()]);
        break;
      case 'Escape':
        this.open.set(false);
        break;
    }
  }

  select(opt: SelectOption): void {
    this.value.set(opt.value);
    this.query.set(opt.label);
    this.open.set(false);
    this.onChange(opt.value);
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(e: MouseEvent): void {
    if (this.open() && !this.host.nativeElement.contains(e.target)) {
      this.open.set(false);
    }
  }

  // --- ControlValueAccessor ---
  private onChange: (v: string | null) => void = () => {};
  onTouched: () => void = () => {};

  writeValue(v: string | null): void { this.value.set(v); }
  registerOnChange(fn: (v: string | null) => void): void { this.onChange = fn; }
  registerOnTouched(fn: () => void): void { this.onTouched = fn; }
  setDisabledState(isDisabled: boolean): void { this.disabled.set(isDisabled); }
}
