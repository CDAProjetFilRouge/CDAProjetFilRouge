import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { URL_BACKEND } from '../../core/api/api.config';
import { AuthService } from '../../core/auth/auth.service';
import { AppUser } from '../../core/models/user.models';
import { InscriptionModel } from '../calendar/calendar.models';
import { PastEvents } from './past-events';

function inscription(id: string, eventId: string, status: 'FINISHED' | 'CANCELLED'): InscriptionModel {
  return {
    id,
    event: {
      id: eventId,
      title: `Évènement ${eventId}`,
      startDateTime: '2026-09-15T09:00:00',
      endDateTime: '2026-09-15T13:00:00',
      clubName: 'Club A',
      status,
    },
    inscriptionDate: '2026-09-01T10:00:00',
    status: 'CONFIRMED',
    price: 6,
  };
}

describe('PastEvents', () => {
  let fixture: ComponentFixture<PastEvents>;
  let http: HttpTestingController;

  const root = () => fixture.nativeElement as HTMLElement;

  async function setup(list: InscriptionModel[]) {
    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [PastEvents],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    TestBed.inject(AuthService).currentUser.set({ id: '8', role: 'MEMBER', clubs: [] } as unknown as AppUser);
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(PastEvents);
    fixture.detectChanges();
    http.expectOne((r) => r.url === `${URL_BACKEND}/inscriptions/user/8/past`).flush(list);
    fixture.detectChanges();
  }

  afterEach(() => http.verify());

  it('liste l’historique avec un lien vers la page de chaque évènement et le badge des annulés', async () => {
    await setup([inscription('1', '9', 'FINISHED'), inscription('2', '10', 'CANCELLED')]);

    const rows = Array.from(root().querySelectorAll<HTMLAnchorElement>('.row'));
    expect(rows.map((r) => r.getAttribute('href'))).toEqual(['/event-details/9', '/event-details/10']);
    expect(rows[0].textContent).toContain('Évènement 9');
    expect(rows[0].textContent).toContain('15/09/2026');
    expect(rows[0].querySelector('.badge')).toBeNull();
    expect(rows[1].querySelector('.badge')?.textContent).toContain('Annulé');
  });

  it('indique quand l’historique est vide', async () => {
    await setup([]);

    expect(root().querySelector('.empty')?.textContent).toContain('Aucun évènement passé');
  });
});
