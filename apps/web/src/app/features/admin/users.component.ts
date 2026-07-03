import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { DatePipe, SlicePipe } from '@angular/common';
import { AdminService } from '../../core/services/admin.service';
import { ToastService } from '../../core/services/toast.service';
import { UserWithRolesView, Page } from '../../core/models/api.models';
import { BadgeComponent } from '../../shared/badge.component';
import { PaginationComponent } from '../../shared/pagination.component';
import { SpinnerComponent } from '../../shared/spinner.component';

const AVAILABLE_ROLES = ['OPERATOR', 'AUDITOR', 'CUSTOMER'];

@Component({
  selector: 'app-users',
  imports: [ReactiveFormsModule, DatePipe, SlicePipe, BadgeComponent, PaginationComponent, SpinnerComponent],
  template: `
    <div class="space-y-4">

      <!-- Header -->
      <div class="flex items-center justify-between">
        <p class="text-sm text-slate-500">Manage users and their assigned roles</p>
        <span class="text-xs bg-amber-50 text-amber-700 ring-1 ring-amber-400/30 rounded-md px-2.5 py-1 font-medium">
          <span class="material-symbols-outlined align-middle" style="font-size:14px">shield</span>
          Requires user:assign-role permission
        </span>
      </div>

      <!-- Users table -->
      <div class="bg-white rounded-xl border border-slate-200 overflow-hidden">
        @if (loading()) {
          <div class="flex justify-center py-16"><app-spinner /></div>
        } @else if (page()?.content?.length === 0) {
          <div class="text-center py-16 text-slate-400">
            <span class="material-symbols-outlined text-4xl mb-2 block">group</span>
            <p class="text-sm">No users found</p>
          </div>
        } @else {
          <div class="overflow-x-auto">
            <table class="w-full text-sm">
              <thead class="bg-slate-50 border-b border-slate-200">
                <tr>
                  <th class="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">User</th>
                  <th class="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Status</th>
                  <th class="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Roles</th>
                  <th class="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Joined</th>
                  <th class="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wide">Actions</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-slate-100">
                @for (u of page()!.content; track u.id) {
                  <tr class="hover:bg-slate-50 transition-colors">
                    <td class="px-4 py-3">
                      <div class="flex items-center gap-3">
                        <div class="w-8 h-8 rounded-full bg-indigo-100 flex items-center justify-center shrink-0">
                          <span class="text-indigo-700 text-xs font-semibold">
                            {{ u.email[0].toUpperCase() }}
                          </span>
                        </div>
                        <div>
                          <p class="font-medium text-slate-900">{{ u.email }}</p>
                          <p class="text-xs font-mono text-slate-400">{{ u.id | slice:0:8 }}…</p>
                        </div>
                      </div>
                    </td>
                    <td class="px-4 py-3">
                      <app-badge [label]="u.enabled ? 'ACTIVE' : 'DISABLED'"
                                 [variant]="u.enabled ? 'success' : 'neutral'" />
                    </td>
                    <td class="px-4 py-3">
                      <div class="flex flex-wrap gap-1">
                        @if (u.roles.length === 0) {
                          <span class="text-xs text-slate-400">No roles</span>
                        }
                        @for (role of u.roles; track role) {
                          <span class="inline-flex items-center rounded-md bg-indigo-50 px-2 py-0.5
                                       text-xs font-medium text-indigo-700 ring-1 ring-indigo-600/20">
                            {{ role }}
                          </span>
                        }
                      </div>
                    </td>
                    <td class="px-4 py-3 text-xs text-slate-500">{{ u.createdAt | date:'dd MMM yyyy' }}</td>
                    <td class="px-4 py-3">
                      <button (click)="openAssignModal(u)"
                              class="text-xs font-medium text-indigo-600 hover:text-indigo-800 flex items-center gap-1">
                        <span class="material-symbols-outlined" style="font-size:14px">add_moderator</span>
                        Assign Role
                      </button>
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

    <!-- Assign Role Modal -->
    @if (selectedUser()) {
      <div class="fixed inset-0 z-50 overflow-y-auto">
        <div class="flex min-h-full items-center justify-center p-4">
          <div class="fixed inset-0 bg-slate-900/60 backdrop-blur-sm" (click)="selectedUser.set(null)"></div>
          <div class="relative bg-white rounded-2xl shadow-2xl max-w-sm w-full p-6 z-10">
            <div class="flex items-center justify-between mb-5">
              <h2 class="text-lg font-semibold text-slate-900">Assign Role</h2>
              <button (click)="selectedUser.set(null)" class="text-slate-400 hover:text-slate-600">
                <span class="material-symbols-outlined">close</span>
              </button>
            </div>

            <p class="text-sm text-slate-600 mb-4">
              Assigning to <span class="font-semibold">{{ selectedUser()!.email }}</span>
            </p>

            <form [formGroup]="roleForm" (ngSubmit)="assignRole()" novalidate>
              <label class="block text-sm font-medium text-slate-700 mb-1.5">Role</label>
              <select formControlName="role"
                      class="w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm outline-none
                             focus:ring-2 focus:ring-indigo-500 focus:border-transparent bg-white mb-4">
                <option value="" disabled>Select a role…</option>
                @for (r of availableRoles; track r) { <option [value]="r">{{ r }}</option> }
              </select>

              <div class="flex gap-3">
                <button type="button" (click)="selectedUser.set(null)"
                        class="flex-1 px-4 py-2.5 border border-slate-300 text-slate-700 hover:bg-slate-50
                               rounded-lg text-sm font-medium transition-colors">Cancel</button>
                <button type="submit" [disabled]="assigning() || roleForm.invalid"
                        class="flex-1 flex items-center justify-center gap-2 bg-indigo-600 hover:bg-indigo-700
                               disabled:opacity-60 text-white font-medium py-2.5 px-4 rounded-lg text-sm transition-colors">
                  @if (assigning()) { <app-spinner size="xs" /> }
                  Assign
                </button>
              </div>
            </form>
          </div>
        </div>
      </div>
    }
  `,
})
export class UsersComponent {
  private readonly adminSvc = inject(AdminService);
  private readonly toast    = inject(ToastService);
  private readonly fb       = inject(FormBuilder);

  readonly availableRoles = AVAILABLE_ROLES;
  readonly loading        = signal(true);
  readonly assigning      = signal(false);
  readonly currentPage    = signal(0);
  readonly page           = signal<Page<UserWithRolesView> | null>(null);
  readonly selectedUser   = signal<UserWithRolesView | null>(null);

  readonly roleForm = this.fb.group({
    role: ['', Validators.required],
  });

  constructor() { this.load(); }

  private load(): void {
    this.loading.set(true);
    this.adminSvc.listUsers(this.currentPage()).subscribe({
      next: p => { this.page.set(p); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  onPageChange(p: number): void { this.currentPage.set(p); this.load(); }

  openAssignModal(u: UserWithRolesView): void {
    this.selectedUser.set(u);
    this.roleForm.reset();
  }

  assignRole(): void {
    if (this.roleForm.invalid || !this.selectedUser()) return;
    this.assigning.set(true);
    this.adminSvc.assignRole(this.selectedUser()!.id, this.roleForm.value.role!).subscribe({
      next: () => {
        this.toast.success(`Role assigned to ${this.selectedUser()!.email}`);
        this.selectedUser.set(null);
        this.load();
      },
      error: () => this.assigning.set(false),
      complete: () => this.assigning.set(false),
    });
  }
}
