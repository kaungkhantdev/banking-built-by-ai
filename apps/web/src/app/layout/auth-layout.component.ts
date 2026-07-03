import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-auth-layout',
  imports: [RouterOutlet],
  template: `
    <div class="min-h-screen bg-gradient-to-br from-slate-900 via-slate-800 to-indigo-950
                flex items-center justify-center p-4">
      <div class="w-full max-w-md">
        <div class="text-center mb-8">
          <div class="inline-flex items-center justify-center w-14 h-14 bg-indigo-600 rounded-2xl mb-4 shadow-lg">
            <span class="material-symbols-outlined text-white text-3xl">account_balance</span>
          </div>
          <h1 class="text-2xl font-bold text-white tracking-tight">BankCore</h1>
          <p class="text-slate-400 text-sm mt-1">Operations Console</p>
        </div>
        <router-outlet />
      </div>
    </div>
  `,
})
export class AuthLayoutComponent {}
