import { HttpClient } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { EventDetailsModel } from './event-details-model';
import { Observable } from 'rxjs';
import { URL_BACKEND } from '../../../core/api/api.config';
import { CommentModel } from './event-details-comments-model';
import { InscriptionModel } from './event-details-inscription-model';

@Service()
export class EventDetailsService {
  private readonly http = inject(HttpClient)

  getEventDetail(id: number): Observable<EventDetailsModel>{
    return this.http.get<EventDetailsModel>(`${URL_BACKEND}/events/${id}`);
  }

  getEventPdf(id: number): Observable<Blob> {
    return this.http.get(`${URL_BACKEND}/PDF/event/${id}`, { responseType: 'blob' });
  }

  getComments(eventId: number): Observable<CommentModel[]>{
    return this.http.get<CommentModel[]>(`${URL_BACKEND}/events/${eventId}/comments`)
  }

  postComment(eventId: number, content: string): Observable<CommentModel> {
    return this.http.post<CommentModel>(`${URL_BACKEND}/events/${eventId}/comments`, null, {
      params: { content },
    });
  }

  deleteComment(eventId: number, commentId: number): Observable<void> {
    return this.http.delete<void>(`${URL_BACKEND}/events/${eventId}/comments/${commentId}`);
  }

  getUserInscriptions(userId: number): Observable<InscriptionModel[]> {
    return this.http.get<InscriptionModel[]>(`${URL_BACKEND}/inscriptions/user/${userId}`, {
      params: { size: 100 },
    });
  }

  register(userId: number, eventId: number): Observable<InscriptionModel> {
    return this.http.post<InscriptionModel>(`${URL_BACKEND}/inscriptions`, null, {
      params: { userId, eventId },
    });
  }

  cancelInscription(inscriptionId: number): Observable<void> {
    return this.http.delete<void>(`${URL_BACKEND}/inscriptions/${inscriptionId}`);
  }
}
