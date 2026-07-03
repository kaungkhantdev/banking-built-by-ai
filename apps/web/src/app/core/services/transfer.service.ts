import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { TransferResult } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class TransferService {
  private readonly http = inject(HttpClient);

  transfer(
    fromWalletId: string,
    toWalletId: string,
    amount: number,
    memo?: string
  ) {
    const headers = new HttpHeaders({
      'Idempotency-Key': crypto.randomUUID(),
    });
    return this.http.post<TransferResult>(
      '/v1/transfers',
      { fromWalletId, toWalletId, amount, memo },
      { headers }
    );
  }

  reverse(transactionId: string, reason = 'operator-initiated') {
    const headers = new HttpHeaders({ 'X-Reason': reason });
    return this.http.post<TransferResult>(
      `/v1/transfers/${transactionId}/reverse`,
      {},
      { headers }
    );
  }
}
