import { HttpClient } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { Observable } from 'rxjs';
import { URL_BACKEND } from '../../core/api/api.config';
import { Kpi } from './dashboard.models';

@Service()
export class DashboardService {
  private readonly http = inject(HttpClient);

  getKpis(): Observable<Kpi[]> {
    return this.http.get<Kpi[]>(`${URL_BACKEND}/dashboard/kpis`);
  }
}
