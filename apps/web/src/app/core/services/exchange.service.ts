import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { ExchangeRateView } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class ExchangeService {
  private readonly http = inject(HttpClient);

  currencies() {
    return this.http.get<string[]>('/v1/exchange/currencies');
  }

  rate(from: string, to: string) {
    const params = new HttpParams().set('from', from).set('to', to);
    return this.http.get<ExchangeRateView>('/v1/exchange/rates', { params });
  }
}
