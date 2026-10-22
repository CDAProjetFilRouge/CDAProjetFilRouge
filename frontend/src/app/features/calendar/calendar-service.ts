import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { Observable } from 'rxjs';
import { URL_BACKEND } from '../../core/api/api.config';
import { InscriptionModel, OrganizedEventModel } from './calendar.models';

@Service()
export class CalendarService {
  private readonly http = inject(HttpClient);

  getByUser(userId: string): Observable<InscriptionModel[]> {
    const params = new HttpParams().set('page', 0).set('size', 200);

    return this.http.get<InscriptionModel[]>(`${URL_BACKEND}/inscriptions/user/${userId}`, {
      params,
    });
  }

  getOrganized(from: string, to: string): Observable<OrganizedEventModel[]> {
    const params = new HttpParams().set('from', from).set('to', to);

    return this.http.get<OrganizedEventModel[]>(`${URL_BACKEND}/events/mine/calendar`, { params });
  }

  cancel(inscriptionId: string): Observable<void> {
    return this.http.delete<void>(`${URL_BACKEND}/inscriptions/${inscriptionId}`);
  }
}
