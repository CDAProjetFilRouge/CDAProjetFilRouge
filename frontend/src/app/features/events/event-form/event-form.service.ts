import { HttpClient } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { Observable } from 'rxjs';
import { URL_BACKEND } from '../../../core/api/api.config';
import { EventRequest, EventSaved } from './event-form.models';

@Service()
export class EventFormService {
  private readonly http = inject(HttpClient);

  create(request: EventRequest): Observable<EventSaved> {
    return this.http.post<EventSaved>(`${URL_BACKEND}/events`, request);
  }

  update(id: number, request: EventRequest): Observable<EventSaved> {
    return this.http.put<EventSaved>(`${URL_BACKEND}/events/${id}`, request);
  }
}
