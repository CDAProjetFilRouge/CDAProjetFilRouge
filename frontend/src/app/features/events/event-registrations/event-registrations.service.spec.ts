import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { URL_BACKEND } from '../../../core/api/api.config';
import { EventRegistrationsService, Registration } from './event-registrations.service';

function registration(id: number): Registration {
  return {
    id,
    user: { id: id + 100, firstName: 'Prénom', lastName: `Nom${id}`, email: `u${id}@example.com` },
    inscriptionDate: '2026-10-01T09:00:00',
    status: 'CONFIRMED',
    price: 10,
  };
}

describe('EventRegistrationsService', () => {
  let service: EventRegistrationsService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(EventRegistrationsService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('appelle la route des inscriptions de l’évènement avec une page de 200', () => {
    service.getAll(5).subscribe();

    const req = httpMock.expectOne((r) => r.url === `${URL_BACKEND}/inscriptions/event/5`);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('page')).toBe('0');
    expect(req.request.params.get('size')).toBe('200');
    req.flush([]);
  });

  it('s’arrête après la première page quand elle n’est pas pleine', () => {
    let result: Registration[] | undefined;
    service.getAll(5).subscribe((list) => (result = list));

    httpMock.expectOne((r) => r.params.get('page') === '0').flush([registration(1), registration(2)]);

    expect(result?.map((r) => r.id)).toEqual([1, 2]);
  });

  it('charge la page suivante tant que la page reçue est pleine, puis regroupe tout', () => {
    let result: Registration[] | undefined;
    service.getAll(5).subscribe((list) => (result = list));

    const fullPage = Array.from({ length: 200 }, (_, i) => registration(i + 1));
    httpMock.expectOne((r) => r.params.get('page') === '0').flush(fullPage);
    httpMock.expectOne((r) => r.params.get('page') === '1').flush([registration(201)]);

    expect(result?.length).toBe(201);
    expect(result?.[200].id).toBe(201);
  });
});
