import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { CustomerView, Page } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class CustomerService {
  private readonly http = inject(HttpClient);

  list(q?: string, page = 0, size = 20) {
    let params = new HttpParams().set('page', page).set('size', size);
    if (q) params = params.set('q', q);
    return this.http.get<Page<CustomerView>>('/v1/customers', { params });
  }

  suspend(id: string) {
    return this.http.post<CustomerView>(`/v1/customers/${id}/suspend`, {});
  }

  close(id: string) {
    return this.http.post<CustomerView>(`/v1/customers/${id}/close`, {});
  }
}
