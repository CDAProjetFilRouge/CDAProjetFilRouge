import { HttpClient } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { EMPTY, Observable, expand, map, reduce } from 'rxjs';
import { URL_BACKEND } from '../../../core/api/api.config';

export type RegistrationStatus = 'CONFIRMED' | 'WAITING_LIST' | 'CANCELED';

export interface Registration {
  id: string;
  user: { id: string; firstName: string; lastName: string; email: string };
  inscriptionDate: string;
  status: RegistrationStatus;
  price: number;
}

const PAGE_SIZE = 200;

@Service()
export class EventRegistrationsService {
  private readonly http = inject(HttpClient);

  getAll(eventId: string): Observable<Registration[]> {
    return this.fetchPage(eventId, 0).pipe(
      expand(({ items, index }) => (items.length === PAGE_SIZE ? this.fetchPage(eventId, index + 1) : EMPTY)),
      map(({ items }) => items),
      reduce((all, items) => [...all, ...items], [] as Registration[]),
    );
  }

  private fetchPage(eventId: string, index: number): Observable<{ items: Registration[]; index: number }> {
    return this.http
      .get<Registration[]>(`${URL_BACKEND}/inscriptions/event/${eventId}`, {
        params: { page: index, size: PAGE_SIZE },
      })
      .pipe(map((items) => ({ items, index })));
  }
}
