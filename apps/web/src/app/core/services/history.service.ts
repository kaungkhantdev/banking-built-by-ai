import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Page, TransactionExportView, TransactionView } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class HistoryService {
  private readonly http = inject(HttpClient);

  list(walletId: string, page = 0, size = 20, direction?: string) {
    let params = new HttpParams().set('page', page).set('size', size);
    if (direction) params = params.set('direction', direction);
    return this.http.get<Page<TransactionView>>(`/v1/wallets/${walletId}/transactions`, { params });
  }

  /** Download a bounded CSV synchronously (best for small/medium result sets). */
  export(walletId: string, direction?: string) {
    let params = new HttpParams();
    if (direction) params = params.set('direction', direction);
    return this.http.get(`/v1/wallets/${walletId}/transactions/export`,
      { params, responseType: 'blob' });
  }

  /** Queue an async CSV export (returns 202 with a QUEUED job). */
  requestExport(walletId: string, direction?: string) {
    let params = new HttpParams();
    if (direction) params = params.set('direction', direction);
    return this.http.post<TransactionExportView>(
      `/v1/wallets/${walletId}/transactions/exports`, null, { params });
  }

  exportStatus(walletId: string, exportId: string) {
    return this.http.get<TransactionExportView>(
      `/v1/wallets/${walletId}/transactions/exports/${exportId}`);
  }

  downloadExport(walletId: string, exportId: string) {
    return this.http.get(
      `/v1/wallets/${walletId}/transactions/exports/${exportId}/download`,
      { responseType: 'blob' });
  }
}
