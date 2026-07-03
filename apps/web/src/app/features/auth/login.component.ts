import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { ToastService } from '../../core/services/toast.service';
import { SpinnerComponent } from '../../shared/spinner.component';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink, SpinnerComponent],
  template: `
    <div class="bg-white rounded-2xl shadow-xl p-8">
      <h2 class="text-xl font-semibold text-slate-900 mb-1">Welcome back</h2>
      <p class="text-sm text-slate-500 mb-6">Sign in to the operations console</p>

      <form [formGroup]="form" (ngSubmit)="submit()" class="space-y-4" novalidate>

        <div>
          <label class="block text-sm font-medium text-slate-700 mb-1.5" for="email">
            Email address
          </label>
          <input id="email" type="email" formControlName="email" autocomplete="email"
                 placeholder="operator@bank.com"
                 class="w-full px-3 py-2.5 border rounded-lg text-sm outline-none transition
                        focus:ring-2 focus:ring-indigo-500 focus:border-transparent
                        placeholder:text-slate-400"
                 [class.border-red-400]="touched('email') && invalid('email')"
                 [class.border-slate-300]="!(touched('email') && invalid('email'))">
          @if (touched('email') && invalid('email')) {
            <p class="mt-1 text-xs text-red-600">Valid email required</p>
          }
        </div>

        <div>
          <label class="block text-sm font-medium text-slate-700 mb-1.5" for="password">
            Password
          </label>
          <input id="password" type="password" formControlName="password" autocomplete="current-password"
                 placeholder="••••••••"
                 class="w-full px-3 py-2.5 border rounded-lg text-sm outline-none transition
                        focus:ring-2 focus:ring-indigo-500 focus:border-transparent
                        placeholder:text-slate-400"
                 [class.border-red-400]="touched('password') && invalid('password')"
                 [class.border-slate-300]="!(touched('password') && invalid('password'))">
          @if (touched('password') && invalid('password')) {
            <p class="mt-1 text-xs text-red-600">Password is required</p>
          }
        </div>

        <button type="submit"
                [disabled]="loading() || form.invalid"
                class="w-full flex items-center justify-center gap-2 bg-indigo-600 hover:bg-indigo-700
                       disabled:opacity-60 disabled:cursor-not-allowed text-white font-medium
                       py-2.5 px-4 rounded-lg text-sm transition-colors mt-2">
          @if (loading()) { <app-spinner size="xs" /> }
          Sign in
        </button>

      </form>

      <p class="mt-6 text-center text-sm text-slate-500">
        No account?
        <a routerLink="/register" class="text-indigo-600 font-medium hover:underline ml-1">Register here</a>
      </p>
    </div>
  `,
})
export class LoginComponent {
  private readonly auth   = inject(AuthService);
  private readonly router = inject(Router);
  private readonly toast  = inject(ToastService);
  private readonly fb     = inject(FormBuilder);

  readonly loading = signal(false);

  readonly form = this.fb.group({
    email:    ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });

  touched(field: string) { return this.form.get(field)?.touched; }
  invalid(field: string) { return this.form.get(field)?.invalid; }

  submit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.loading.set(true);
    const { email, password } = this.form.value;
    this.auth.login(email!, password!).subscribe({
      next: () => { this.toast.success('Logged in'); this.router.navigate(['/dashboard']); },
      error: () => this.loading.set(false),
    });
  }
}
