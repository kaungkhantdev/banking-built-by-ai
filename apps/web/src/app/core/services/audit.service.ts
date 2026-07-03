import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { AuditRecord, Page } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class AuditService {
  private readonly http = inject(HttpClient);

  search(actor?: string, action?: string, page = 0, size = 20) {
    let params = new HttpParams().set('page', page).set('size', size);
    if (actor?.trim()) params = params.set('actor', actor.trim());
    if (action?.trim()) params = params.set('action', action.trim());
    return this.http.get<Page<AuditRecord>>('/v1/audit', { params });
  }
}
