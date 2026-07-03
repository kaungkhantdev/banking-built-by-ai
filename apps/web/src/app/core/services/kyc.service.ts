import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { KycCaseView, KycStatus, KycStatusView, Page } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class KycService {
  private readonly http = inject(HttpClient);

  list(status?: KycStatus, page = 0, size = 20) {
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) params = params.set('status', status);
    return this.http.get<Page<KycCaseView>>('/v1/kyc', { params });
  }

  openCase(accountId: string) {
    return this.http.post<string>('/v1/kyc', { accountId });
  }

  submitDocument(accountId: string, file: File) {
    const body = new FormData();
    body.append('file', file);
    return this.http.post(`/v1/kyc/${accountId}/documents`, body);
  }

  status(accountId: string) {
    return this.http.get<KycStatusView>(`/v1/kyc/${accountId}/status`);
  }
}
