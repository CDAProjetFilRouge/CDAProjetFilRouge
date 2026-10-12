import { HttpClient } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { Observable } from 'rxjs';
import { URL_BACKEND } from '../../../core/api/api.config';
import { EventDetail, EventImage, EventRequest, EventSaved } from './event-form.models';

@Service()
export class EventFormService {
  private readonly http = inject(HttpClient);

  getById(id: number): Observable<EventDetail> {
    return this.http.get<EventDetail>(`${URL_BACKEND}/events/${id}`);
  }

  create(request: EventRequest): Observable<EventSaved> {
    return this.http.post<EventSaved>(`${URL_BACKEND}/events`, request);
  }

  update(id: number, request: EventRequest): Observable<EventSaved> {
    return this.http.put<EventSaved>(`${URL_BACKEND}/events/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${URL_BACKEND}/events/${id}`);
  }

  uploadImage(eventId: number, file: File): Observable<EventImage> {
    const body = new FormData();
    body.append('file', file);
    return this.http.post<EventImage>(`${URL_BACKEND}/events/${eventId}/images`, body);
  }

  deleteImage(eventId: number, imageId: number): Observable<void> {
    return this.http.delete<void>(`${URL_BACKEND}/events/${eventId}/images/${imageId}`);
  }
}
