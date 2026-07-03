import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Page, TransactionView } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class HistoryService {
  private readonly http = inject(HttpClient);

  list(walletId: string, page = 0, size = 20, direction?: string) {
    let params = new HttpParams().set('page', page).set('size', size);
    if (direction) params = params.set('direction', direction);
    return this.http.get<Page<TransactionView>>(`/v1/wallets/${walletId}/transactions`, { params });
  }
}
