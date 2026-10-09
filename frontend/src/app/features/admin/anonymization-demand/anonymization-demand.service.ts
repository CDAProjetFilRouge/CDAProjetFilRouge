import { inject, Service, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { AnonymizationDemand, AnonymizationDemandStatus } from '../../../core/models/anonymization-demand.models';
import { Observable } from 'rxjs';
import { URL_BACKEND } from '../../../core/api/api.config';


@Service()
export class AnonymizationDemandService {

  private readonly http = inject(HttpClient);
  private readonly _pendingCount = signal<number>(0);
  readonly pendingCount = this._pendingCount.asReadonly();

  getDemands(status?: AnonymizationDemandStatus): Observable<AnonymizationDemand[]> {
    let params = new HttpParams();
    if (status) {
      params = params.set('status', status);
    }
    return this.http.get<AnonymizationDemand[]>(`${URL_BACKEND}/anonymization-demands`, { params });
  }

  validateDemand(id: number): Observable<AnonymizationDemand> {
    return this.http.put<AnonymizationDemand>(`${URL_BACKEND}/anonymization-demands/${id}/validate`, {});
  }

  refreshPendingCount(): void {
    this.http.get<number>(`${URL_BACKEND}/anonymization-demands/count`)
    .subscribe( n => this._pendingCount.set(n));
  }

  hasPendingDemand(): Observable<boolean> {
    return this.http.get<boolean>(`${URL_BACKEND}/anonymization-demands/me/pending`);
  }

  requestAnonymization():Observable<AnonymizationDemand> {
    return this.http.post<AnonymizationDemand>(`${URL_BACKEND}/anonymization-demands`, {});
  }
}
