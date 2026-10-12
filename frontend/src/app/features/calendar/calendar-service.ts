import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { Observable } from 'rxjs';
import { URL_BACKEND } from '../../core/api/api.config';
import { InscriptionModel } from './calendar.models';

@Service()
export class CalendarService {
  private readonly http = inject(HttpClient);

  getByUser(userId: number): Observable<InscriptionModel[]> {
    const params = new HttpParams().set('page', 0).set('size', 200);

    return this.http.get<InscriptionModel[]>(`${URL_BACKEND}/inscriptions/user/${userId}`, {
      params,
    });
  }
}
