import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BalanceView, WalletListItem, WalletView } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class WalletService {
  private readonly http = inject(HttpClient);

  list() {
    return this.http.get<WalletListItem[]>('/v1/wallets');
  }

  open(accountId: string, currency: string) {
    return this.http.post<WalletView>('/v1/wallets', { accountId, currency });
  }

  balance(id: string) {
    return this.http.get<BalanceView>(`/v1/wallets/${id}/balance`);
  }

  freeze(id: string) {
    return this.http.post<WalletView>(`/v1/wallets/${id}/freeze`, {});
  }
}
