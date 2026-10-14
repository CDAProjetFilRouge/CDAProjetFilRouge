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

  getEventDetail(id: string): Observable<EventDetailsModel>{
    return this.http.get<EventDetailsModel>(`${URL_BACKEND}/events/${id}`);
  }

  getEventPdf(id: string): Observable<Blob> {
    return this.http.get(`${URL_BACKEND}/PDF/event/${id}`, { responseType: 'blob' });
  }

  getComments(eventId: string): Observable<CommentModel[]>{
    return this.http.get<CommentModel[]>(`${URL_BACKEND}/events/${eventId}/comments`)
  }

  postComment(eventId: string, content: string): Observable<CommentModel> {
    return this.http.post<CommentModel>(`${URL_BACKEND}/events/${eventId}/comments`, null, {
      params: { content },
    });
  }

  deleteComment(eventId: string, commentId: string): Observable<void> {
    return this.http.delete<void>(`${URL_BACKEND}/events/${eventId}/comments/${commentId}`);
  }

  getUserInscriptions(userId: string): Observable<InscriptionModel[]> {
    return this.http.get<InscriptionModel[]>(`${URL_BACKEND}/inscriptions/user/${userId}`, {
      params: { size: 100 },
    });
  }

  register(userId: string, eventId: string): Observable<InscriptionModel> {
    return this.http.post<InscriptionModel>(`${URL_BACKEND}/inscriptions`, null, {
      params: { userId, eventId },
    });
  }

  cancelInscription(inscriptionId: string): Observable<void> {
    return this.http.delete<void>(`${URL_BACKEND}/inscriptions/${inscriptionId}`);
  }
}
