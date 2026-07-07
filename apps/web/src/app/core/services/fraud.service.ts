import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { FraudAlertView, Page } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class FraudService {
  private readonly http = inject(HttpClient);

  list(status?: string, page = 0, size = 20) {
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) params = params.set('status', status);
    return this.http.get<Page<FraudAlertView>>('/v1/fraud/alerts', { params });
  }

  approve(id: string) {
    return this.http.post<FraudAlertView>(`/v1/fraud/alerts/${id}/approve`, null);
  }

  dismiss(id: string) {
    return this.http.post<FraudAlertView>(`/v1/fraud/alerts/${id}/dismiss`, null);
  }
}
