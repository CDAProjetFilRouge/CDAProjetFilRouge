import { inject, Service } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { AnonymizationDemand, AnonymizationDemandStatus } from '../../../core/models/anonymisation-demande.models';
import { Observable } from 'rxjs';
import { URL_BACKEND } from '../../../core/api/api.config';


@Service()
export class AnonymisationDemandService {

  private readonly http = inject(HttpClient);

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
}
