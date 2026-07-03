import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { HealthSummary, Page, PlatformStats, UserWithRolesView } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly http = inject(HttpClient);

  stats() {
    return this.http.get<PlatformStats>('/v1/admin/stats');
  }

  health() {
    return this.http.get<HealthSummary>('/v1/admin/health');
  }

  listUsers(page = 0, size = 20) {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<Page<UserWithRolesView>>('/v1/admin/users', { params });
  }

  assignRole(userId: string, role: string) {
    return this.http.post<void>(`/v1/admin/users/${userId}/roles`, { role });
  }
}
