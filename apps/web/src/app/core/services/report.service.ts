import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ReportView } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class ReportService {
  private readonly http = inject(HttpClient);

  list() {
    return this.http.get<ReportView[]>('/v1/reports');
  }

  request(reportType: string, from: string, to: string, format: string) {
    return this.http.post<ReportView>('/v1/reports', { reportType, from, to, format });
  }

  download(id: string) {
    return this.http.get(`/v1/reports/${id}`, { responseType: 'blob' });
  }
}
