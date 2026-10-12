import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { EventDetailsModel } from './event-details-model';
import { Observable } from 'rxjs';
import { URL_BACKEND } from '../../../core/api/api.config';
import { CommentModel } from './event-details-comments-model';

@Service()
export class EventDetailsService {
  private readonly http = inject(HttpClient)

  getEventDetail(id: number): Observable<EventDetailsModel>{
    return this.http.get<EventDetailsModel>(`${URL_BACKEND}/events/${id}`);
  }

  getComments(eventId: number): Observable<CommentModel[]>{
    return this.http.get<CommentModel[]>(`${URL_BACKEND}/events/${eventId}/comments`)
  }

  publishComment(idEvent: number, content: string): Observable<CommentModel>{
    return this.http.post<CommentModel>(`${URL_BACKEND}/events/${idEvent}/comments`, { content });
  }

  deleteComment(idComment: number, idEvent: number): void{
    this.http.delete<void>(`${URL_BACKEND}/events/${idEvent}/comments/${idComment}`);
  }
}
