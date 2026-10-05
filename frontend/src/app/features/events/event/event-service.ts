import { HttpClient } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { Observable } from 'rxjs';
import { URL_BACKEND } from '../../../core/api/api.config';
import { EventModel } from './event-model';

@Service()
export class EventService {
    private readonly http = inject(HttpClient);

  getEvents(): Observable<EventModel[]>{
    return this.http.get<EventModel[]>(`${URL_BACKEND}/events`);
  }
}
