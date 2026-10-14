import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { URL_BACKEND } from '../../../core/api/api.config';
import { EventDetailsModel } from '../event-details/event-details-model';
import { EventRegistrations } from './event-registrations';
import { Registration, RegistrationStatus } from './event-registrations.service';

function registration(id: string, name: string, date: string, status: RegistrationStatus): Registration {
  return {
    id,
    user: { id: id + 100, firstName: name, lastName: 'Test', email: `${name.toLowerCase()}@example.com` },
    inscriptionDate: date,
    status,
    price: 10,
  };
}

const EVENT_ID = '3f2b8c1e-4d5a-4b6c-8d7e-9f0a1b2c3d4e';

const EVENT = {
  id: EVENT_ID,
  title: 'Tournoi de tennis',
  startDate: '2026-12-06T18:00:00',
  maxCapacity: 3,
} as EventDetailsModel;

function setup(id: string) {
  TestBed.configureTestingModule({
    imports: [EventRegistrations],
    providers: [
      provideHttpClient(),
      provideHttpClientTesting(),
      provideRouter([]),
      { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id }) } } },
    ],
  });
  const fixture: ComponentFixture<EventRegistrations> = TestBed.createComponent(EventRegistrations);
  const httpMock = TestBed.inject(HttpTestingController);
  return { fixture, httpMock, element: fixture.nativeElement as HTMLElement };
}

describe('EventRegistrations', () => {
  it('sépare les inscrits confirmés et la liste d’attente, dans l’ordre d’arrivée, sans les annulés', async () => {
    const { fixture, httpMock, element } = setup(EVENT_ID);

    httpMock.expectOne(`${URL_BACKEND}/events/${EVENT_ID}`).flush(EVENT);
    httpMock.expectOne((r) => r.url === `${URL_BACKEND}/inscriptions/event/${EVENT_ID}`).flush([
      registration('1', 'Bob', '2026-10-02T10:00:00', 'CONFIRMED'),
      registration('2', 'Alice', '2026-10-01T09:00:00', 'CONFIRMED'),
      registration('3', 'Eve', '2026-10-05T08:00:00', 'WAITING_LIST'),
      registration('4', 'David', '2026-10-04T14:00:00', 'WAITING_LIST'),
      registration('5', 'Zoe', '2026-10-06T08:00:00', 'CANCELED'),
    ]);
    await fixture.whenStable();

    const sections = element.querySelectorAll('section');
    const names = (section: Element) =>
      Array.from(section.querySelectorAll('.name')).map((cell) => cell.textContent?.trim());

    expect(names(sections[0])).toEqual(['Alice Test', 'Bob Test']);
    expect(names(sections[1])).toEqual(['David Test', 'Eve Test']);
    expect(element.textContent).not.toContain('Zoe');
    expect(element.querySelector('.count')?.textContent).toContain('2');
    expect(element.querySelector('.count')?.textContent).toContain('3');
  });

  it('affiche les états vides quand il n’y a personne', async () => {
    const { fixture, httpMock, element } = setup(EVENT_ID);

    httpMock.expectOne(`${URL_BACKEND}/events/${EVENT_ID}`).flush(EVENT);
    httpMock.expectOne((r) => r.url === `${URL_BACKEND}/inscriptions/event/${EVENT_ID}`).flush([]);
    await fixture.whenStable();

    expect(element.textContent).toContain('Aucun inscrit pour le moment.');
    expect(element.textContent).toContain('Personne en liste d’attente.');
  });

  it('affiche le message du serveur quand l’accès est refusé', async () => {
    const { fixture, httpMock, element } = setup(EVENT_ID);

    httpMock.expectOne(`${URL_BACKEND}/events/${EVENT_ID}`).flush(EVENT);
    httpMock
      .expectOne((r) => r.url === `${URL_BACKEND}/inscriptions/event/${EVENT_ID}`)
      .flush("Seul l'organisateur de cet évènement peut consulter ses inscriptions.", {
        status: 403,
        statusText: 'Forbidden',
      });
    await fixture.whenStable();

    expect(element.querySelector('[role="alert"]')?.textContent).toContain('Seul l\'organisateur');
    expect(element.querySelector('table')).toBeNull();
  });

  it('n’appelle pas le serveur quand l’identifiant est invalide', async () => {
    const { fixture, httpMock, element } = setup('abc');
    await fixture.whenStable();

    httpMock.expectNone(() => true);
    expect(element.querySelector('[role="alert"]')?.textContent).toContain('introuvable');
  });
});
