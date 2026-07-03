import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { AccountListItem, AccountView, Page } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class AccountService {
  private readonly http = inject(HttpClient);

  list(page = 0, size = 20) {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<Page<AccountListItem>>('/v1/accounts', { params });
  }

  open(ownerUserId: string) {
    return this.http.post<AccountView>('/v1/accounts', { ownerUserId });
  }

  activate(id: string) {
    return this.http.post<AccountView>(`/v1/accounts/${id}/activate`, {});
  }
}
