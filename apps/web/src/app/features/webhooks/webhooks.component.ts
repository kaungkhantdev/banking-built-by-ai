import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { WebhookService } from '../../core/services/webhook.service';
import { ToastService } from '../../core/services/toast.service';
import { WebhookDeliveryStatus, WebhookDeliveryView, WebhookView } from '../../core/models/api.models';
import { BadgeComponent, BadgeVariant } from '../../shared/badge.component';
import { SpinnerComponent } from '../../shared/spinner.component';
import { EmptyStateComponent } from '../../shared/empty-state.component';

@Component({
  selector: 'app-webhooks',
  imports: [ReactiveFormsModule, DatePipe, BadgeComponent, SpinnerComponent, EmptyStateComponent],
  template: `
    <div class="space-y-6">

      <!-- Register form -->
      <section class="bg-white rounded-xl border border-slate-200 p-6">
        <h2 class="text-sm font-semibold text-slate-900 mb-4 flex items-center gap-2">
          <span class="material-symbols-outlined text-indigo-500" style="font-size:18px">webhook</span>
          Register Webhook
        </h2>
        <form [formGroup]="registerForm" (ngSubmit)="register()" class="space-y-4" novalidate>
          <div>
            <label class="block text-xs font-medium text-slate-500 mb-1.5">Endpoint URL</label>
            <input type="url" formControlName="url" placeholder="https://your-server.com/webhook"
                   class="w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm outline-none
                          focus:ring-2 focus:ring-indigo-500 focus:border-transparent">
          </div>
          <div>
            <label class="block text-xs font-medium text-slate-500 mb-1.5">Secret</label>
            <input type="password" formControlName="secret" placeholder="Signing secret"
                   class="w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm outline-none
                          focus:ring-2 focus:ring-indigo-500 focus:border-transparent">
          </div>
          <div>
            <label class="block text-xs font-medium text-slate-500 mb-1.5">Event Types</label>
            <input type="text" formControlName="eventTypes"
                   placeholder="transfer.completed,kyc.verified (comma-separated)"
                   class="w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm outline-none
                          focus:ring-2 focus:ring-indigo-500 focus:border-transparent">
            <p class="mt-1 text-xs text-slate-400">Comma-separated event names</p>
          </div>
          <div class="flex justify-end">
            <button type="submit" [disabled]="saving() || registerForm.invalid"
                    class="flex items-center gap-2 bg-indigo-600 hover:bg-indigo-700 disabled:opacity-60
                           text-white text-sm font-medium px-5 py-2.5 rounded-lg transition-colors">
              @if (saving()) { <app-spinner size="xs" /> }
              Register
            </button>
          </div>
        </form>
      </section>

      <!-- Endpoints table -->
      <div class="bg-white rounded-xl border border-slate-200 overflow-hidden">
        <div class="px-5 py-3.5 border-b border-slate-200">
          <h2 class="text-sm font-semibold text-slate-900">Registered Endpoints</h2>
        </div>
        @if (loading()) {
          <div class="flex justify-center py-12"><app-spinner /></div>
        } @else if (!webhooks().length) {
          <div class="text-center py-12 text-slate-400">
            <span class="material-symbols-outlined text-4xl mb-2 block">webhook</span>
            <p class="text-sm">No webhooks registered yet</p>
          </div>
        } @else {
          <div class="overflow-x-auto">
            <table class="w-full text-sm">
              <thead class="bg-slate-50 border-b border-slate-200">
                <tr>
                  <th class="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">URL</th>
                  <th class="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Events</th>
                  <th class="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Active</th>
                  <th class="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Created</th>
                  <th class="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Actions</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-slate-100">
                @for (w of webhooks(); track w.id) {
                  <tr class="hover:bg-slate-50 transition-colors">
                    <td class="px-4 py-3 text-xs font-mono text-slate-700 max-w-xs truncate" [title]="w.url">
                      {{ w.url }}
                    </td>
                    <td class="px-4 py-3 text-xs text-slate-500">{{ w.eventTypes }}</td>
                    <td class="px-4 py-3">
                      <app-badge [label]="w.active ? 'Active' : 'Disabled'"
                                 [variant]="w.active ? 'success' : 'neutral'" />
                    </td>
                    <td class="px-4 py-3 text-xs text-slate-500">{{ w.createdAt | date:'dd MMM yyyy' }}</td>
                    <td class="px-4 py-3 whitespace-nowrap">
                      <button (click)="viewDeliveries(w)"
                              class="text-xs font-medium text-indigo-600 hover:text-indigo-800">
                        Deliveries
                      </button>
                      <span class="text-slate-300 mx-2">|</span>
                      <button (click)="remove(w)"
                              class="text-xs font-medium text-red-600 hover:text-red-800">
                        Delete
                      </button>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }
      </div>

      <!-- Delivery history for the selected endpoint -->
      @if (selectedId()) {
        <div class="bg-white rounded-xl border border-slate-200 overflow-hidden">
          <div class="px-5 py-3.5 border-b border-slate-200 flex items-center gap-2">
            <span class="material-symbols-outlined text-indigo-500" style="font-size:18px">history</span>
            <h2 class="text-sm font-semibold text-slate-900 flex-1">Delivery History</h2>
            <button (click)="closeDeliveries()" class="text-xs text-slate-400 hover:text-slate-600">Close</button>
          </div>
          @if (deliveriesLoading()) {
            <div class="flex justify-center py-10"><app-spinner /></div>
          } @else if (!deliveries().length) {
            <app-empty-state icon="outbox" message="No deliveries yet"
                             hint="Deliveries appear once a matching event is published." />
          } @else {
            <div class="overflow-x-auto">
              <table class="w-full text-sm">
                <thead class="bg-slate-50 border-b border-slate-200">
                  <tr>
                    <th class="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Event</th>
                    <th class="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Status</th>
                    <th class="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Attempts</th>
                    <th class="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">HTTP</th>
                    <th class="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Last Error</th>
                    <th class="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Created</th>
                  </tr>
                </thead>
                <tbody class="divide-y divide-slate-100">
                  @for (d of deliveries(); track d.id) {
                    <tr class="hover:bg-slate-50 transition-colors">
                      <td class="px-4 py-3 text-xs font-mono text-slate-600">{{ d.eventType }}</td>
                      <td class="px-4 py-3"><app-badge [label]="d.status" [variant]="deliveryVariant(d.status)" /></td>
                      <td class="px-4 py-3 text-slate-600">{{ d.attempts }}</td>
                      <td class="px-4 py-3 text-xs text-slate-500">{{ d.responseCode ?? '—' }}</td>
                      <td class="px-4 py-3 text-xs text-slate-500 max-w-xs truncate" [title]="d.lastError || ''">{{ d.lastError || '—' }}</td>
                      <td class="px-4 py-3 text-xs text-slate-500">{{ d.createdAt | date:'dd MMM, HH:mm' }}</td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          }
        </div>
      }

    </div>
  `,
})
export class WebhooksComponent {
  private readonly webhookSvc = inject(WebhookService);
  private readonly toast      = inject(ToastService);
  private readonly fb         = inject(FormBuilder);

  readonly loading  = signal(true);
  readonly saving   = signal(false);
  readonly webhooks = signal<WebhookView[]>([]);
  readonly selectedId        = signal<string | null>(null);
  readonly deliveries        = signal<WebhookDeliveryView[]>([]);
  readonly deliveriesLoading = signal(false);

  readonly registerForm = this.fb.group({
    url:        ['', [Validators.required, Validators.pattern(/^https?:\/\/.+/)]],
    secret:     ['', Validators.required],
    eventTypes: ['transfer.completed', Validators.required],
  });

  constructor() { this.load(); }

  private load(): void {
    this.loading.set(true);
    this.webhookSvc.list().subscribe({
      next: ws => { this.webhooks.set(ws); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  register(): void {
    if (this.registerForm.invalid) { this.registerForm.markAllAsTouched(); return; }
    this.saving.set(true);
    const { url, secret, eventTypes } = this.registerForm.value;
    this.webhookSvc.create(url!, secret!, eventTypes!).subscribe({
      next: () => { this.toast.success('Webhook registered'); this.registerForm.reset({ eventTypes: 'transfer.completed' }); this.load(); },
      error: () => this.saving.set(false),
      complete: () => this.saving.set(false),
    });
  }

  remove(w: WebhookView): void {
    this.webhookSvc.delete(w.id).subscribe({
      next: () => {
        this.toast.success('Webhook deleted');
        if (this.selectedId() === w.id) this.closeDeliveries();
        this.load();
      },
    });
  }

  viewDeliveries(w: WebhookView): void {
    this.selectedId.set(w.id);
    this.deliveriesLoading.set(true);
    this.deliveries.set([]);
    this.webhookSvc.deliveries(w.id).subscribe({
      next: ds => { this.deliveries.set(ds); this.deliveriesLoading.set(false); },
      error: () => this.deliveriesLoading.set(false),
    });
  }

  closeDeliveries(): void {
    this.selectedId.set(null);
    this.deliveries.set([]);
  }

  deliveryVariant(s: WebhookDeliveryStatus): BadgeVariant {
    return s === 'DELIVERED' ? 'success' : s === 'FAILED' ? 'error' : 'warning';
  }
}
