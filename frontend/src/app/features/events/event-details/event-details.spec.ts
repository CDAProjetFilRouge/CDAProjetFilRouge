import { vi } from 'vitest';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting, TestRequest } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';
import { URL_BACKEND } from '../../../core/api/api.config';
import { AuthService } from '../../../core/auth/auth.service';
import { AppUser } from '../../../core/models/user.models';
import { CommentModel } from './event-details-comments-model';
import { EventDetailsModel } from './event-details-model';
import { EventDetails } from './event-details';

const DAY = 24 * 60 * 60 * 1000;

function makeUser(overrides: Partial<AppUser> = {}): AppUser {
  return {
    id: '8',
    firstName: 'Claire',
    lastName: 'Membre',
    email: 'claire@example.com',
    phone: null,
    role: 'MEMBER',
    status: 'ACTIVE',
    suspensionEndDate: null,
    creationDate: '2026-01-01',
    address: null,
    clubs: [],
    ...overrides,
  } as AppUser;
}

function makeEvent(overrides: Partial<EventDetailsModel> = {}): EventDetailsModel {
  const start = new Date(Date.now() + 10 * DAY);
  start.setHours(18, 0, 0, 0);
  const end = new Date(start.getTime() + 2 * 60 * 60 * 1000);
  return {
    id: EVENT_ID,
    title: 'Tournoi de tennis',
    description: 'Un tournoi',
    street1: '12 rue de la Paix',
    street2: null,
    city: 'Toulouse',
    postalCode: '31000',
    country: 'France',
    category: 'SPORT',
    startDate: start.toISOString(),
    endDate: end.toISOString(),
    affiliatePrice: 10,
    nonAffiliatePrice: 0,
    maxCapacity: 10,
    remainingSpots: 4,
    imageGallery: [],
    status: 'PUBLISHED',
    clubName: 'Tennis Club',
    organizerFirstName: 'David',
    organizerLastname: 'Orga',
    organizerId: '42',
    ...overrides,
  };
}

function makeComment(id: string, authorId: string, content = 'Super'): CommentModel {
  return {
    id,
    author: { id: authorId, firstName: 'Léa', lastName: 'Martin', email: 'lea@example.com' },
    content,
    creationDate: '2026-10-01T10:00:00',
  };
}

const EVENT_ID = '3f2b8c1e-4d5a-4b6c-8d7e-9f0a1b2c3d4e';
const MISSING_EVENT_ID = '7a1d9e20-5b3c-4f6a-9c8d-1e2f3a4b5c6d';

describe('EventDetails', () => {
  let fixture: ComponentFixture<EventDetails>;
  let component: EventDetails;
  let http: HttpTestingController;
  let router: Router;
  let auth: AuthService;

  const val = <T>(name: string): T => (component as unknown as Record<string, () => unknown>)[name]() as T;
  const call = (name: string, ...args: unknown[]): unknown =>
    (component as unknown as Record<string, (...a: unknown[]) => unknown>)[name](...args);

  interface Backend {
    event?: EventDetailsModel;
    comments?: CommentModel[];
    inscriptions?: unknown[];
  }

  const flushAll = (requests: TestRequest[], body: object | null) => requests.forEach((request) => request.flush(body));
  const inscriptionLists = () => http.match((r) => r.method === 'GET' && r.url.startsWith(`${URL_BACKEND}/inscriptions/user/`));

  async function setup(user: AppUser | null, backend: Backend = {}, idParam: string | null = EVENT_ID) {
    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [EventDetails],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ActivatedRoute, useValue: { paramMap: of(convertToParamMap(idParam === null ? {} : { id: idParam })) } },
      ],
    }).compileComponents();

    auth = TestBed.inject(AuthService);
    auth.currentUser.set(user);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    http = TestBed.inject(HttpTestingController);

    fixture = TestBed.createComponent(EventDetails);
    component = fixture.componentInstance;
    fixture.detectChanges();

    if (idParam === null || Number.isNaN(parseInt(idParam, 10))) {
      return;
    }
    http.expectOne(`${URL_BACKEND}/events/${idParam}`).flush(backend.event ?? makeEvent());
    http.expectOne(`${URL_BACKEND}/events/${idParam}/comments`).flush(backend.comments ?? []);
    if (user) {
      flushAll(inscriptionLists(), backend.inscriptions ?? []);
    }
    await fixture.whenStable();
  }

  afterEach(() => {
    http.verify();
  });

  describe('chargement', () => {
    it('affiche l’évènement chargé', async () => {
      await setup(makeUser());

      expect(val<EventDetailsModel>('eventDetail').title).toBe('Tournoi de tennis');
      expect(val<boolean>('loading')).toBe(false);
      expect(val<string>('categoryLabel')).toBe('Sport');
      expect(val<string>('statusLabel')).toBe('Publié');
    });

    it('affiche une erreur quand l’évènement est introuvable', async () => {
      TestBed.resetTestingModule();
      await TestBed.configureTestingModule({
        imports: [EventDetails],
        providers: [
          provideRouter([]),
          provideHttpClient(),
          provideHttpClientTesting(),
          { provide: ActivatedRoute, useValue: { paramMap: of(convertToParamMap({ id: MISSING_EVENT_ID })) } },
        ],
      }).compileComponents();
      http = TestBed.inject(HttpTestingController);
      fixture = TestBed.createComponent(EventDetails);
      component = fixture.componentInstance;
      fixture.detectChanges();

      http.expectOne(`${URL_BACKEND}/events/${MISSING_EVENT_ID}`).flush('Évènement introuvable', { status: 404, statusText: 'Not Found' });
      http.expectOne(`${URL_BACKEND}/events/${MISSING_EVENT_ID}/comments`).flush([]);

      expect(val<string>('loadError')).toBe('Évènement introuvable');
      expect(val<boolean>('loading')).toBe(false);
    });

    it('renvoie à l’accueil quand l’identifiant n’est pas un UUID', async () => {
      await setup(null, {}, 'abc');

      expect(router.navigate).toHaveBeenCalledWith(['/']);
    });
  });

  describe('droits', () => {
    it('autorise la modification à l’organisateur et à l’administrateur seulement', async () => {
      await setup(makeUser({ id: '42', role: 'ORGANIZER' }));
      expect(val<boolean>('canEdit')).toBe(true);

      auth.currentUser.set(makeUser({ id: '99', role: 'ADMINISTRATOR' }));
      expect(val<boolean>('canEdit')).toBe(true);

      auth.currentUser.set(makeUser({ id: '99', role: 'ORGANIZER' }));
      expect(val<boolean>('canEdit')).toBe(false);

      auth.currentUser.set(null);
      expect(val<boolean>('canEdit')).toBe(false);
    });

    it('autorise la suppression d’un commentaire à son auteur et à l’administrateur', async () => {
      await setup(makeUser({ id: '8' }));
      expect(call('canDelete', makeComment('1', '8'))).toBe(true);
      expect(call('canDelete', makeComment('2', '99'))).toBe(false);

      auth.currentUser.set(makeUser({ id: '1', role: 'ADMINISTRATOR' }));
      expect(call('canDelete', makeComment('2', '99'))).toBe(true);
    });
  });

  describe('carte d’inscription', () => {
    it('invite à se connecter quand on est anonyme, sans demander d’inscriptions', async () => {
      await setup(null);

      expect(val<{ kind: string }>('registration').kind).toBe('login');
      http.expectNone(`${URL_BACKEND}/inscriptions/user/8`);
    });

    it('propose de s’inscrire quand il reste des places', async () => {
      await setup(makeUser());

      expect(val('registration')).toEqual({ kind: 'open', waiting: false });
      expect(val<string>('calendarMark')).toBe('plus');
    });

    it('propose la liste d’attente quand l’évènement est complet', async () => {
      await setup(makeUser(), { event: makeEvent({ remainingSpots: 0 }) });

      expect(val('registration')).toEqual({ kind: 'open', waiting: true });
    });

    it('reconnaît une inscription confirmée', async () => {
      await setup(makeUser(), {
        inscriptions: [{ id: '3', event: { id: EVENT_ID, title: 'x', startDateTime: '', endDateTime: '' }, status: 'CONFIRMED' }],
      });

      expect(val('registration')).toEqual({ kind: 'registered', waiting: false, position: null });
      expect(val<string>('calendarMark')).toBe('check');
    });

    it('reconnaît une place en liste d’attente avec son rang', async () => {
      await setup(makeUser(), {
        inscriptions: [
          { id: '3', event: { id: EVENT_ID, title: 'x', startDateTime: '', endDateTime: '' }, status: 'WAITING_LIST', waitingListPosition: 2 },
        ],
      });

      expect(val('registration')).toEqual({ kind: 'registered', waiting: true, position: 2 });
    });

    it('ignore une inscription annulée et les inscriptions à d’autres évènements', async () => {
      await setup(makeUser(), {
        inscriptions: [
          { id: '3', event: { id: EVENT_ID, title: 'x', startDateTime: '', endDateTime: '' }, status: 'CANCELED' },
          { id: '4', event: { id: '6', title: 'y', startDateTime: '', endDateTime: '' }, status: 'CONFIRMED' },
        ],
      });

      expect(val<{ kind: string }>('registration').kind).toBe('open');
    });

    it.each([
      ['CANCELLED', 'Cet évènement a été annulé.'],
      ['DRAFT', 'Cet évènement n’est pas encore publié.'],
      ['FINISHED', 'Cet évènement est terminé.'],
    ] as const)('bloque l’inscription à un évènement %s', async (status, reason) => {
      await setup(makeUser(), { event: makeEvent({ status }) });

      expect(val('registration')).toEqual({ kind: 'blocked', reason });
    });

    it.each([
      ['CANCELLED', 'Cet évènement a été annulé.'],
      ['FINISHED', 'Cet évènement est terminé : vous y étiez inscrit·e.'],
    ] as const)('ne propose plus de se désinscrire d’un évènement %s', async (status, reason) => {
      await setup(makeUser(), {
        event: makeEvent({ status }),
        inscriptions: [{ id: '3', event: { id: EVENT_ID, title: 'x', startDateTime: '', endDateTime: '' }, status: 'CONFIRMED' }],
      });

      expect(val('registration')).toEqual({ kind: 'blocked', reason });
    });

    it('bloque l’inscription à un évènement dont la fin est passée', async () => {
      const past = new Date(Date.now() - 2 * DAY).toISOString();
      await setup(makeUser(), { event: makeEvent({ startDate: past, endDate: past }) });

      expect(val('registration')).toEqual({ kind: 'blocked', reason: 'Cet évènement est terminé.' });
    });
  });

  describe('inscription', () => {
    it('inscrit l’utilisateur puis recharge l’évènement et l’agenda', async () => {
      await setup(makeUser());

      call('register');

      const post = http.expectOne((r) => r.method === 'POST' && r.url === `${URL_BACKEND}/inscriptions`);
      expect(post.request.params.get('userId')).toBe('8');
      expect(post.request.params.get('eventId')).toBe(EVENT_ID);
      post.flush({ id: '3', event: { id: EVENT_ID }, status: 'CONFIRMED', price: 10 });

      http.expectOne(`${URL_BACKEND}/events/${EVENT_ID}`).flush(makeEvent({ remainingSpots: 3 }));
      flushAll(inscriptionLists(), []);

      expect(val<{ kind: string }>('registration').kind).toBe('registered');
      expect(val<EventDetailsModel>('eventDetail').remainingSpots).toBe(3);
      expect(val<boolean>('inscriptionBusy')).toBe(false);
    });

    it('n’envoie rien deux fois pendant qu’une inscription est en cours', async () => {
      await setup(makeUser());

      call('register');
      call('register');

      const posts = http.match((r) => r.method === 'POST' && r.url === `${URL_BACKEND}/inscriptions`);
      expect(posts.length).toBe(1);
      posts[0].flush('Évènement complet', { status: 409, statusText: 'Conflict' });
    });

    it('montre le message du serveur quand l’inscription est refusée', async () => {
      await setup(makeUser());

      call('register');
      http.expectOne((r) => r.method === 'POST').flush('Vous êtes déjà inscrit', { status: 409, statusText: 'Conflict' });

      expect(val<string>('inscriptionError')).toBe('Vous êtes déjà inscrit');
      expect(val<boolean>('inscriptionBusy')).toBe(false);
    });

    it('montre un message par défaut quand le serveur ne dit rien', async () => {
      await setup(makeUser());

      call('register');
      http.expectOne((r) => r.method === 'POST').flush(null, { status: 500, statusText: 'Server Error' });

      expect(val<string>('inscriptionError')).toContain('L’inscription a échoué');
    });

    it('annule l’inscription après confirmation et la retire de la carte', async () => {
      await setup(makeUser(), {
        inscriptions: [{ id: '3', event: { id: EVENT_ID, title: 'x', startDateTime: '', endDateTime: '' }, status: 'CONFIRMED' }],
      });

      call('askCancel');
      expect(val<boolean>('confirmingCancel')).toBe(true);

      call('cancelInscription');
      const del = http.expectOne((r) => r.method === 'DELETE' && r.url === `${URL_BACKEND}/inscriptions/3`);
      del.flush(null);
      http.expectOne(`${URL_BACKEND}/events/${EVENT_ID}`).flush(makeEvent({ remainingSpots: 5 }));
      flushAll(inscriptionLists(), []);

      expect(val<{ kind: string }>('registration').kind).toBe('open');
      expect(val<boolean>('confirmingCancel')).toBe(false);
    });

    it('garde l’inscription quand on renonce à l’annulation', async () => {
      await setup(makeUser());

      call('askCancel');
      call('keepInscription');

      expect(val<boolean>('confirmingCancel')).toBe(false);
    });

    it('garde l’inscription et prévient quand l’annulation échoue', async () => {
      await setup(makeUser(), {
        inscriptions: [{ id: '3', event: { id: EVENT_ID, title: 'x', startDateTime: '', endDateTime: '' }, status: 'CONFIRMED' }],
      });

      call('cancelInscription');
      http.expectOne((r) => r.method === 'DELETE').flush(null, { status: 500, statusText: 'Server Error' });

      expect(val<{ kind: string }>('registration').kind).toBe('registered');
      expect(val<string>('inscriptionError')).toContain('L’annulation a échoué');
    });
  });

  describe('calendrier', () => {
    it('affiche les autres inscriptions estompées et l’évènement courant', async () => {
      await setup(makeUser(), {
        inscriptions: [
          { id: '9', event: { id: '6', title: 'Soirée jeux', startDateTime: '2030-01-01T18:00:00', endDateTime: '2030-01-01T20:00:00' }, status: 'WAITING_LIST' },
          { id: '10', event: { id: '7', title: 'Annulée', startDateTime: '2030-02-01T18:00:00', endDateTime: '2030-02-01T20:00:00' }, status: 'CANCELED' },
        ],
      });

      const events = val<{ id: string; title: string; muted?: boolean; badge?: string }[]>('calendarEvents');

      expect(events.map((e) => e.id)).toEqual(['6', EVENT_ID]);
      expect(events[0]).toMatchObject({ muted: true, badge: 'Liste d’attente' });
      expect(events[1].muted).toBeUndefined();
    });

    it('ouvre la fenêtre d’inscription au clic sur l’évènement courant', async () => {
      await setup(makeUser());

      call('onCalendarSelect', EVENT_ID);

      expect(val<boolean>('dialogOpen')).toBe(true);
      expect(router.navigate).not.toHaveBeenCalled();
    });

    it('va sur l’autre évènement quand on clique dessus', async () => {
      await setup(makeUser());

      call('onCalendarSelect', '6');

      expect(router.navigate).toHaveBeenCalledWith(['/event-details', '6']);
      expect(val<boolean>('dialogOpen')).toBe(false);
    });
  });

  describe('affichage', () => {
    it('calcule le taux de remplissage entre 0 et 100', async () => {
      await setup(makeUser(), { event: makeEvent({ maxCapacity: 10, remainingSpots: 4 }) });
      expect(val<number>('takenRatio')).toBe(60);
      expect(val<boolean>('isFull')).toBe(false);

      await setup(makeUser(), { event: makeEvent({ maxCapacity: 10, remainingSpots: 0 }) });
      expect(val<number>('takenRatio')).toBe(100);
      expect(val<boolean>('isFull')).toBe(true);

      await setup(makeUser(), { event: makeEvent({ maxCapacity: 0, remainingSpots: 0 }) });
      expect(val<number>('takenRatio')).toBe(0);
    });

    it('écrit « Gratuit » pour un tarif nul', async () => {
      await setup(makeUser());

      expect(call('formatPrice', 0)).toBe('Gratuit');
      expect(String(call('formatPrice', 10))).toContain('10');
    });

    it('met les initiales en majuscules', async () => {
      await setup(makeUser());

      expect(call('initials', 'léa', 'martin')).toBe('LM');
    });

    it('résume l’horaire sur une seule ligne quand l’évènement tient sur un jour', async () => {
      await setup(makeUser());

      const parts = val<{ time: string; day: string }>('dateParts');

      expect(parts.time).toMatch(/18:00\s–\s20:00/);
    });

    it('précise la date de fin quand l’évènement dure plusieurs jours', async () => {
      const start = new Date(Date.now() + 10 * DAY);
      const end = new Date(start.getTime() + 2 * DAY);
      await setup(makeUser(), { event: makeEvent({ startDate: start.toISOString(), endDate: end.toISOString() }) });

      expect(val<{ time: string }>('dateParts').time).toContain('jusqu’au');
    });
  });

  describe('galerie', () => {
    const gallery = [
      { id: '1', path: '/b.png', displayOrder: 2 },
      { id: '2', path: '/a.png', displayOrder: 1 },
      { id: '3', path: '/c.png', displayOrder: 3 },
    ];

    it('trie les images par ordre d’affichage', async () => {
      await setup(makeUser(), { event: makeEvent({ imageGallery: gallery }) });

      expect(val<{ id: string }[]>('galleryOrdered').map((i) => i.id)).toEqual(['2', '1', '3']);
    });

    it('fait le tour de la galerie dans les deux sens', async () => {
      await setup(makeUser(), { event: makeEvent({ imageGallery: gallery }) });

      call('previousImage');
      expect(val<number>('selectedImageIndex')).toBe(2);
      call('nextImage');
      expect(val<number>('selectedImageIndex')).toBe(0);
      call('selectImage', 1);
      expect(val<number>('selectedImageIndex')).toBe(1);
    });

    it('ne fait rien quand il n’y a pas d’image', async () => {
      await setup(makeUser());

      call('nextImage');

      expect(val<number>('selectedImageIndex')).toBe(0);
    });
  });

  describe('commentaires', () => {
    it('publie un commentaire en tête de liste et vide le brouillon', async () => {
      await setup(makeUser(), { comments: [makeComment('1', '99', 'Ancien')] });
      call('onDraftInput', { target: { value: '  Bravo  ' } } as unknown as Event);

      call('publishComment');
      const post = http.expectOne((r) => r.method === 'POST' && r.url === `${URL_BACKEND}/events/${EVENT_ID}/comments`);
      expect(post.request.params.get('content')).toBe('Bravo');
      post.flush(makeComment('2', '8', 'Bravo'));

      expect(val<CommentModel[]>('comments').map((c) => c.id)).toEqual(['2', '1']);
      expect(val<string>('commentDraft')).toBe('');
    });

    it('n’envoie pas un commentaire vide', async () => {
      await setup(makeUser());
      call('onDraftInput', { target: { value: '   ' } } as unknown as Event);

      call('publishComment');

      http.expectNone((r) => r.method === 'POST');
    });

    it('garde le brouillon et montre l’erreur quand la publication échoue', async () => {
      await setup(makeUser());
      call('onDraftInput', { target: { value: 'Bravo' } } as unknown as Event);

      call('publishComment');
      http.expectOne((r) => r.method === 'POST').flush('Commentaire trop long', { status: 400, statusText: 'Bad Request' });

      expect(val<string>('commentError')).toBe('Commentaire trop long');
      expect(val<string>('commentDraft')).toBe('Bravo');
    });

    it('supprime un commentaire de la liste', async () => {
      await setup(makeUser(), { comments: [makeComment('1', '8'), makeComment('2', '99')] });

      call('deleteComment', makeComment('1', '8'));
      http.expectOne((r) => r.method === 'DELETE' && r.url === `${URL_BACKEND}/events/${EVENT_ID}/comments/1`).flush(null);

      expect(val<CommentModel[]>('comments').map((c) => c.id)).toEqual(['2']);
    });

    it('garde le commentaire et prévient quand la suppression échoue', async () => {
      await setup(makeUser(), { comments: [makeComment('1', '8')] });

      call('deleteComment', makeComment('1', '8'));
      http.expectOne((r) => r.method === 'DELETE').flush(null, { status: 403, statusText: 'Forbidden' });

      expect(val<CommentModel[]>('comments').length).toBe(1);
      expect(val<string>('commentError')).toContain('pas pu être supprimé');
    });
  });
});
