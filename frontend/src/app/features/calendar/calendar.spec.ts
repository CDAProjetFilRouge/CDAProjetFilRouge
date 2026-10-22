import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { URL_BACKEND } from '../../core/api/api.config';
import { AuthService } from '../../core/auth/auth.service';
import { AppUser } from '../../core/models/user.models';
import { CalendarEvent } from '../../shared/event-calendar/event-calendar.models';
import { Calendar } from './calendar';
import { InscriptionModel, OrganizedEventModel } from './calendar.models';

const RANGE = { from: '2026-09-28T00:00:00', to: '2026-11-09T00:00:00' };

function makeUser(overrides: Partial<AppUser> = {}): AppUser {
  return {
    id: '8',
    firstName: 'David',
    lastName: 'Orga',
    email: 'david@example.com',
    phone: null,
    role: 'ORGANIZER',
    status: 'ACTIVE',
    suspensionEndDate: null,
    creationDate: '2026-01-01',
    address: null,
    clubs: [],
    ...overrides,
  } as AppUser;
}

function inscription(id: string, eventId: string, overrides: Partial<InscriptionModel> = {}): InscriptionModel {
  return {
    id,
    event: {
      id: eventId,
      title: `Inscription à ${eventId}`,
      startDateTime: '2026-10-15T09:00:00',
      endDateTime: '2026-10-15T13:00:00',
      clubName: 'Club A',
    },
    inscriptionDate: '2026-10-01T10:00:00',
    status: 'CONFIRMED',
    price: 6,
    ...overrides,
  };
}

function organized(id: string, overrides: Partial<OrganizedEventModel> = {}): OrganizedEventModel {
  return {
    id,
    title: `Évènement ${id}`,
    startDateTime: '2026-10-20T09:00:00',
    endDateTime: '2026-10-20T11:00:00',
    status: 'PUBLISHED',
    maxCapacity: 30,
    confirmedCount: 18,
    clubName: 'Club A',
    ...overrides,
  };
}

describe('Calendar', () => {
  let fixture: ComponentFixture<Calendar>;
  let http: HttpTestingController;

  const events = () => (fixture.componentInstance as unknown as { calendarEvents(): CalendarEvent[] }).calendarEvents();
  const root = () => fixture.nativeElement as HTMLElement;
  const checkbox = (label: string) =>
    Array.from(root().querySelectorAll<HTMLLabelElement>('.layer'))
      .find((l) => l.textContent?.includes(label))
      ?.querySelector('input') as HTMLInputElement;

  async function setup(user: AppUser, inscriptions: InscriptionModel[], organizedEvents: OrganizedEventModel[] = []) {
    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [Calendar],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    TestBed.inject(AuthService).currentUser.set(user);
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(Calendar);
    fixture.detectChanges();
    http.expectOne((r) => r.url === `${URL_BACKEND}/inscriptions/user/${user.id}`).flush(inscriptions);

    (fixture.componentInstance as unknown as { setRange(range: typeof RANGE): void }).setRange(RANGE);
    fixture.detectChanges();
    if (user.role === 'ORGANIZER') {
      // FullCalendar signale aussi sa propre plage au rendu : seule la dernière requête reste en vie.
      const pending = http
        .match((r) => r.url === `${URL_BACKEND}/events/mine/calendar`)
        .filter((r) => !r.cancelled);
      expect(pending).toHaveLength(1);
      expect(pending[0].request.params.get('from')).toBe(RANGE.from);
      expect(pending[0].request.params.get('to')).toBe(RANGE.to);
      pending[0].flush(organizedEvents);
    }
    fixture.detectChanges();
  }

  // jsdom ne connaît pas encore showModal() / close() sur <dialog>.
  beforeAll(() => {
    HTMLDialogElement.prototype.showModal = function (this: HTMLDialogElement) {
      this.setAttribute('open', '');
    };
    HTMLDialogElement.prototype.close = function (this: HTMLDialogElement) {
      this.removeAttribute('open');
    };
  });

  afterEach(() => http.verify());

  it('montre à un organisateur ses évènements et ses inscriptions, avec les deux cases cochées', async () => {
    await setup(makeUser(), [inscription('4', '9')], [organized('1', { status: 'DRAFT' })]);

    expect(events().map((e) => e.id)).toEqual(['4', 'org:1']);
    expect(checkbox('Mes inscriptions').checked).toBe(true);
    expect(checkbox('Mes évènements').checked).toBe(true);
  });

  it('retire un calque sans recharger la page', async () => {
    await setup(makeUser(), [inscription('4', '9')], [organized('1')]);

    checkbox('Mes évènements').click();
    fixture.detectChanges();
    expect(events().map((e) => e.id)).toEqual(['4']);

    checkbox('Mes inscriptions').click();
    fixture.detectChanges();
    expect(events()).toEqual([]);

    checkbox('Mes évènements').click();
    fixture.detectChanges();
    expect(events().map((e) => e.id)).toEqual(['org:1']);
  });

  it('n’affiche qu’une fois un évènement auquel l’organisateur est inscrit, avec les deux badges', async () => {
    await setup(makeUser(), [inscription('4', '1'), inscription('5', '9')], [organized('1')]);

    expect(events().map((e) => e.id)).toEqual(['5', 'org:1']);
    expect(events().find((e) => e.id === 'org:1')?.badge).toBe('Publié · 18/30 · Inscrit·e');
  });

  it('garde l’inscription quand le calque « Mes évènements » est décoché', async () => {
    await setup(makeUser(), [inscription('4', '1')], [organized('1')]);

    checkbox('Mes évènements').click();
    fixture.detectChanges();

    expect(events().map((e) => e.id)).toEqual(['4']);
  });

  it('ne montre ni case ni appel à /events/mine/calendar à un membre', async () => {
    await setup(makeUser({ role: 'MEMBER' }), [inscription('4', '9')]);

    expect(root().querySelector('.layers')).toBeNull();
    expect(events().map((e) => e.id)).toEqual(['4']);
  });

  it('ouvre la fenêtre d’un évènement organisé avec ses actions de gestion', async () => {
    await setup(makeUser(), [], [organized('1', { status: 'CANCELLED' })]);

    (fixture.componentInstance as unknown as { select(id: string): void }).select('org:1');
    fixture.detectChanges();

    const dialog = root().querySelector('dialog') as HTMLDialogElement;
    expect(dialog.textContent).toContain('Évènement 1');
    expect(dialog.textContent).toContain('Annulé');
    expect(dialog.textContent).toContain('18/30');
    const links = Array.from(dialog.querySelectorAll('a')).map((a) => [a.textContent?.trim(), a.getAttribute('href')]);
    expect(links).toEqual([
      ['Voir l\'évènement', '/event-details/1'],
      ['Voir les inscrits', '/events/1/registrations'],
      ['Gérer l\'évènement', '/events/1/edit'],
    ]);
    expect(dialog.textContent).not.toContain('Me désinscrire');
  });

  it('garde la fenêtre d’une inscription inchangée', async () => {
    await setup(makeUser(), [inscription('4', '9')], []);

    (fixture.componentInstance as unknown as { select(id: string): void }).select('4');
    fixture.detectChanges();

    const dialog = root().querySelector('dialog') as HTMLDialogElement;
    expect(dialog.textContent).toContain('Inscription à 9');
    expect(dialog.textContent).toContain('Confirmée');
    expect(dialog.textContent).not.toContain('Gérer l\'évènement');
  });
});
