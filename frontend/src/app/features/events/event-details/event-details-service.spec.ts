import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { URL_BACKEND } from '../../../core/api/api.config';
import { EventDetailsService } from './event-details-service';

describe('EventDetailsService', () => {
  let service: EventDetailsService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(EventDetailsService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
  });

  it('charge un évènement par son identifiant', () => {
    service.getEventDetail(5).subscribe();

    const request = http.expectOne(`${URL_BACKEND}/events/5`);
    expect(request.request.method).toBe('GET');
    request.flush({});
  });

  it('charge le PDF d’un évènement en binaire', () => {
    service.getEventPdf(5).subscribe();

    const request = http.expectOne(`${URL_BACKEND}/PDF/event/5`);
    expect(request.request.responseType).toBe('blob');
    request.flush(new Blob(['%PDF']));
  });

  it('charge les commentaires d’un évènement', () => {
    service.getComments(5).subscribe();

    http.expectOne(`${URL_BACKEND}/events/5/comments`).flush([]);
  });

  it('publie un commentaire avec son texte en paramètre', () => {
    service.postComment(5, 'Bravo').subscribe();

    const request = http.expectOne((r) => r.url === `${URL_BACKEND}/events/5/comments`);
    expect(request.request.method).toBe('POST');
    expect(request.request.params.get('content')).toBe('Bravo');
    request.flush({});
  });

  it('supprime un commentaire', () => {
    service.deleteComment(5, 9).subscribe();

    const request = http.expectOne(`${URL_BACKEND}/events/5/comments/9`);
    expect(request.request.method).toBe('DELETE');
    request.flush(null);
  });

  it('charge les inscriptions d’un utilisateur, jusqu’à 100 par page', () => {
    service.getUserInscriptions(8).subscribe();

    const request = http.expectOne((r) => r.url === `${URL_BACKEND}/inscriptions/user/8`);
    expect(request.request.params.get('size')).toBe('100');
    request.flush([]);
  });

  it('inscrit un utilisateur à un évènement', () => {
    service.register(8, 5).subscribe();

    const request = http.expectOne((r) => r.url === `${URL_BACKEND}/inscriptions`);
    expect(request.request.method).toBe('POST');
    expect(request.request.params.get('userId')).toBe('8');
    expect(request.request.params.get('eventId')).toBe('5');
    request.flush({});
  });

  it('annule une inscription', () => {
    service.cancelInscription(3).subscribe();

    const request = http.expectOne(`${URL_BACKEND}/inscriptions/3`);
    expect(request.request.method).toBe('DELETE');
    request.flush(null);
  });
});
