import { vi } from 'vitest';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AbstractControl, FormGroup } from '@angular/forms';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';
import { URL_BACKEND } from '../../../core/api/api.config';
import { AuthService } from '../../../core/auth/auth.service';
import { AppUser } from '../../../core/models/user.models';
import { EventForm } from './event-form';
import { EventDetail } from './event-form.models';

const FUTURE_START = '2099-06-01T10:00';
const FUTURE_END = '2099-06-01T12:00';

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
    clubs: [{ id: '3', name: 'Tennis Club' }],
    ...overrides,
  } as AppUser;
}

function makeDetail(overrides: Partial<EventDetail> = {}): EventDetail {
  return {
    id: '5',
    title: 'Tournoi',
    description: 'Un tournoi',
    street1: '12 rue de la Paix',
    street2: null,
    postalCode: '31000',
    city: 'Toulouse',
    country: 'France',
    category: 'SPORT',
    startDate: FUTURE_START,
    endDate: FUTURE_END,
    affiliatePrice: 10,
    nonAffiliatePrice: 20,
    maxCapacity: 30,
    status: 'DRAFT',
    organizerId: '8',
    imageGallery: [],
    ...overrides,
  };
}

function validValues() {
  return {
    title: 'Tournoi',
    description: 'Un tournoi',
    location: { street1: '12 rue de la Paix', street2: '', postalCode: '31000', city: 'Toulouse', country: 'France' },
    category: 'SPORT' as const,
    clubId: '3',
    startDateTime: FUTURE_START,
    endDateTime: FUTURE_END,
    maxCapacity: 30,
    affiliatePrice: 10,
    nonAffiliatePrice: 20,
  };
}

describe('EventForm', () => {
  let fixture: ComponentFixture<EventForm>;
  let component: EventForm;
  let http: HttpTestingController;
  let router: Router;
  let auth: AuthService;

  async function setup(eventId: string | null, user: AppUser | null = makeUser()) {
    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [EventForm],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap(eventId ? { id: eventId } : {}) } },
        },
      ],
    }).compileComponents();

    auth = TestBed.inject(AuthService);
    auth.currentUser.set(user);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(EventForm);
    component = fixture.componentInstance;
    await fixture.whenStable();
  }

  const form = () => (component as unknown as { form: FormGroup }).form;
  const control = (path: string): AbstractControl => form().get(path) as AbstractControl;

  afterEach(() => {
    http.verify();
  });

  describe('création', () => {
    beforeEach(async () => {
      await setup(null);
    });

    it('préremplit le club quand l’organisateur n’en a qu’un', () => {
      expect(control('clubId').value).toBe('3');
    });

    it('refuse d’envoyer un formulaire vide et affiche les erreurs', () => {
      (component as unknown as { saveDraft(): void }).saveDraft();

      http.expectNone(`${URL_BACKEND}/events`);
      expect(control('title').touched).toBe(true);
      expect(form().invalid).toBe(true);
    });

    it('accepte des valeurs valides', () => {
      form().patchValue(validValues());
      expect(form().valid).toBe(true);
    });

    it.each(['123', '123456', 'abcde'])('refuse le code postal « %s »', (postalCode) => {
      form().patchValue(validValues());
      control('location.postalCode').setValue(postalCode);
      expect(control('location.postalCode').invalid).toBe(true);
    });

    it.each(['1234', '31000'])('accepte le code postal « %s »', (postalCode) => {
      form().patchValue(validValues());
      control('location.postalCode').setValue(postalCode);
      expect(control('location.postalCode').valid).toBe(true);
    });

    it('refuse un nombre de places nul ou trop grand', () => {
      form().patchValue(validValues());
      control('maxCapacity').setValue(0);
      expect(control('maxCapacity').invalid).toBe(true);
      control('maxCapacity').setValue(100001);
      expect(control('maxCapacity').invalid).toBe(true);
      control('maxCapacity').setValue(100000);
      expect(control('maxCapacity').valid).toBe(true);
    });

    it('refuse un tarif négatif ou trop grand, accepte la gratuité', () => {
      form().patchValue(validValues());
      control('affiliatePrice').setValue(-1);
      expect(control('affiliatePrice').invalid).toBe(true);
      control('nonAffiliatePrice').setValue(100000000);
      expect(control('nonAffiliatePrice').invalid).toBe(true);
      control('affiliatePrice').setValue(0);
      expect(control('affiliatePrice').valid).toBe(true);
    });

    it('refuse un début dans le passé', () => {
      form().patchValue(validValues());
      control('startDateTime').setValue('2020-01-01T10:00');
      expect(control('startDateTime').errors).toEqual({ startInPast: true });
    });

    it('refuse une année hors de 1900-2100', () => {
      form().patchValue(validValues());
      control('endDateTime').setValue('2200-01-01T10:00');
      expect(control('endDateTime').errors).toEqual({ yearRange: true });
    });

    it('refuse une fin avant le début', () => {
      form().patchValue(validValues());
      control('endDateTime').setValue('2099-06-01T09:00');
      expect(form().errors).toEqual({ endBeforeStart: true });
    });

    it('enregistre un brouillon : crée l’évènement puis ouvre sa page de modification', () => {
      form().patchValue(validValues());

      (component as unknown as { saveDraft(): void }).saveDraft();

      const create = http.expectOne(`${URL_BACKEND}/events`);
      expect(create.request.method).toBe('POST');
      expect(create.request.body.idClub).toBe('3');
      expect(create.request.body.title).toBe('Tournoi');
      expect(create.request.body.status).toBeUndefined();
      create.flush({ id: '42', status: 'DRAFT' });

      expect(router.navigate).toHaveBeenCalledWith(['/events', '42', 'edit']);
    });

    it('publie : crée puis passe l’évènement en PUBLISHED', () => {
      form().patchValue(validValues());

      (component as unknown as { publish(): void }).publish();

      http.expectOne(`${URL_BACKEND}/events`).flush({ id: '42', status: 'DRAFT' });
      const update = http.expectOne(`${URL_BACKEND}/events/42`);
      expect(update.request.method).toBe('PUT');
      expect(update.request.body.status).toBe('PUBLISHED');
      update.flush({ id: '42', status: 'PUBLISHED' });

      expect(router.navigateByUrl).toHaveBeenCalledWith('/');
    });

    it('explique que le brouillon existe quand la publication échoue', () => {
      form().patchValue(validValues());

      (component as unknown as { publish(): void }).publish();

      http.expectOne(`${URL_BACKEND}/events`).flush({ id: '42', status: 'DRAFT' });
      http.expectOne(`${URL_BACKEND}/events/42`).flush('Dates invalides', { status: 400, statusText: 'Bad Request' });

      const message = (component as unknown as { errorMessage(): string }).errorMessage();
      expect(message).toContain('Le brouillon a été enregistré');
      expect(message).toContain('Dates invalides');
    });

    it('montre une erreur claire quand le serveur est injoignable', () => {
      form().patchValue(validValues());

      (component as unknown as { saveDraft(): void }).saveDraft();

      http.expectOne(`${URL_BACKEND}/events`).error(new ProgressEvent('error'), { status: 0 });

      expect((component as unknown as { errorMessage(): string }).errorMessage()).toContain('injoignable');
    });

    describe('images à l’avance', () => {
      const select = (file: File) => {
        const input = document.createElement('input');
        Object.defineProperty(input, 'files', { value: [file], configurable: true });
        (component as unknown as { onPendingFileSelected(i: HTMLInputElement): void }).onPendingFileSelected(input);
      };
      const pending = () => (component as unknown as { pendingImages(): unknown[] }).pendingImages();
      const error = () => (component as unknown as { errorMessage(): string }).errorMessage();

      beforeEach(() => {
        vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:test');
        vi.spyOn(URL, 'revokeObjectURL').mockReturnValue();
      });

      it('accepte une image', () => {
        select(new File(['x'], 'a.png', { type: 'image/png' }));
        expect(pending().length).toBe(1);
        expect(error()).toBe('');
      });

      it('refuse un fichier qui n’est pas une image', () => {
        select(new File(['x'], 'a.pdf', { type: 'application/pdf' }));
        expect(pending().length).toBe(0);
        expect(error()).toBe('Le fichier doit être une image.');
      });

      it('refuse une image de plus de 5 Mo', () => {
        const big = new File([new Uint8Array(5 * 1024 * 1024 + 1)], 'big.png', { type: 'image/png' });
        select(big);
        expect(pending().length).toBe(0);
        expect(error()).toBe("L'image ne peut pas dépasser 5 Mo.");
      });

      it('refuse une onzième image', () => {
        for (let i = 0; i < 10; i++) {
          select(new File(['x'], `a${i}.png`, { type: 'image/png' }));
        }
        select(new File(['x'], 'onzieme.png', { type: 'image/png' }));
        expect(pending().length).toBe(10);
        expect(error()).toBe('Un évènement ne peut pas avoir plus de 10 images.');
      });

      it('envoie les images après la création, puis publie', () => {
        select(new File(['x'], 'a.png', { type: 'image/png' }));
        form().patchValue(validValues());

        (component as unknown as { publish(): void }).publish();

        http.expectOne(`${URL_BACKEND}/events`).flush({ id: '42', status: 'DRAFT' });
        http.expectOne(`${URL_BACKEND}/events/42/images`).flush({ id: '1', path: '/uploads/a.png', displayOrder: 1 });
        http.expectOne(`${URL_BACKEND}/events/42`).flush({ id: '42', status: 'PUBLISHED' });

        expect(router.navigateByUrl).toHaveBeenCalledWith('/');
      });

      it('garde le brouillon et prévient quand une image échoue, sans publier', () => {
        select(new File(['x'], 'a.png', { type: 'image/png' }));
        form().patchValue(validValues());

        (component as unknown as { publish(): void }).publish();

        http.expectOne(`${URL_BACKEND}/events`).flush({ id: '42', status: 'DRAFT' });
        http.expectOne(`${URL_BACKEND}/events/42/images`).flush('refusée', { status: 400, statusText: 'Bad Request' });
        http.expectNone(`${URL_BACKEND}/events/42`);

        expect(router.navigate).toHaveBeenCalledWith(['/events', '42', 'edit'], {
          state: { notice: expect.stringContaining('1 image(s)') },
        });
      });
    });
  });

  describe('modification', () => {
    it('charge l’évènement dans le formulaire et verrouille le club', async () => {
      await setup('5');
      http.expectOne(`${URL_BACKEND}/events/5`).flush(makeDetail());

      expect(control('title').value).toBe('Tournoi');
      expect(control('location.city').value).toBe('Toulouse');
      expect(control('maxCapacity').value).toBe(30);
      expect(control('clubId').disabled).toBe(true);
      expect((component as unknown as { canPublish(): boolean }).canPublish()).toBe(true);
    });

    it('enregistre sans republier : le statut est conservé', async () => {
      await setup('5');
      http.expectOne(`${URL_BACKEND}/events/5`).flush(makeDetail());

      (component as unknown as { saveDraft(): void }).saveDraft();

      const update = http.expectOne(`${URL_BACKEND}/events/5`);
      expect(update.request.method).toBe('PUT');
      expect(update.request.body.status).toBe('DRAFT');
      expect(update.request.body.idClub).toBeUndefined();
      update.flush({ id: '5', status: 'DRAFT' });

      expect((component as unknown as { successMessage(): string }).successMessage()).toBe('Modifications enregistrées.');
    });

    it('publie un brouillon existant', async () => {
      await setup('5');
      http.expectOne(`${URL_BACKEND}/events/5`).flush(makeDetail());

      (component as unknown as { publish(): void }).publish();

      const update = http.expectOne(`${URL_BACKEND}/events/5`);
      expect(update.request.body.status).toBe('PUBLISHED');
      update.flush({ id: '5', status: 'PUBLISHED' });

      expect((component as unknown as { successMessage(): string }).successMessage()).toBe('Évènement publié.');
      expect((component as unknown as { status(): string }).status()).toBe('PUBLISHED');
    });

    it('verrouille le formulaire pour un évènement déjà passé', async () => {
      await setup('5');
      http.expectOne(`${URL_BACKEND}/events/5`).flush(
        makeDetail({ startDate: '2020-01-01T10:00', endDate: '2020-01-01T12:00', status: 'PUBLISHED' }),
      );

      expect(form().disabled).toBe(true);
      expect((component as unknown as { canPublish(): boolean }).canPublish()).toBe(false);
    });

    it('n’autorise pas un autre organisateur, mais autorise un administrateur', async () => {
      await setup('5', makeUser({ id: '99' }));
      http.expectOne(`${URL_BACKEND}/events/5`).flush(makeDetail({ organizerId: '8' }));
      expect((component as unknown as { isOwner(): boolean }).isOwner()).toBe(false);

      auth.currentUser.set(makeUser({ id: '99', role: 'ADMINISTRATOR' }));
      expect((component as unknown as { isOwner(): boolean }).isOwner()).toBe(true);
    });

    it('affiche « introuvable » quand l’évènement n’existe pas', async () => {
      await setup('404');
      http.expectOne(`${URL_BACKEND}/events/404`).flush('', { status: 404, statusText: 'Not Found' });

      expect((component as unknown as { loadError(): string }).loadError()).toBe('Évènement introuvable.');
    });

    it('retire une image de la galerie après suppression côté serveur', async () => {
      await setup('5');
      http
        .expectOne(`${URL_BACKEND}/events/5`)
        .flush(makeDetail({ imageGallery: [{ id: '1', path: '/a.png', displayOrder: 1 }, { id: '2', path: '/b.png', displayOrder: 2 }] }));

      (component as unknown as { removeImage(id: string): void }).removeImage('1');
      http.expectOne(`${URL_BACKEND}/events/5/images/1`).flush(null);

      const gallery = (component as unknown as { detail(): EventDetail }).detail().imageGallery;
      expect(gallery.map((image) => image.id)).toEqual(['2']);
    });
  });
});
