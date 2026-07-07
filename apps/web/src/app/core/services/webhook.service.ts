import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { WebhookDeliveryView, WebhookView } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class WebhookService {
  private readonly http = inject(HttpClient);

  list() {
    return this.http.get<WebhookView[]>('/v1/webhooks');
  }

  create(url: string, secret: string, eventTypes: string) {
    return this.http.post<WebhookView>('/v1/webhooks', { url, secret, eventTypes });
  }

  delete(id: string) {
    return this.http.delete<void>(`/v1/webhooks/${id}`);
  }

  deliveries(id: string) {
    return this.http.get<WebhookDeliveryView[]>(`/v1/webhooks/${id}/deliveries`);
  }
}
