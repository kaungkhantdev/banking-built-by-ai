import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Page, ScheduledTransferView } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class ScheduledTransferService {
  private readonly http = inject(HttpClient);

  list(page = 0, size = 20) {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<Page<ScheduledTransferView>>('/v1/scheduled-transfers', { params });
  }

  cancel(id: string) {
    return this.http.delete<void>(`/v1/scheduled-transfers/${id}`);
  }
}
