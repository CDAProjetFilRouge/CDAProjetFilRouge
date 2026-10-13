import { vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { EventModel } from '../../features/events/event/event-model';
import { CardRegistration, EventCard } from './event-card';

const DAY = 24 * 60 * 60 * 1000;
const START = new Date(Date.now() + 10 * DAY);
START.setHours(18, 0, 0, 0);
const END = new Date(START.getTime() + 2 * 60 * 60 * 1000);

function makeEvent(overrides: Partial<EventModel> = {}): EventModel {
  return {
    id: 7,
    title: 'Tournoi de tennis',
    description: '',
    location: { id: 1, street1: '12 rue de la Paix', street2: '', postalCode: '31000', city: 'Toulouse', country: 'France' },
    category: 'SPORT',
    startDateTime: START,
    endDateTime: END,
    affiliatePrice: 10,
    nonAffiliatePrice: 20,
    maxCapacity: 10,
    remainingSpots: 8,
    imageGallery: [],
    status: 'PUBLISHED',
    clubName: 'Tennis Club',
    ...overrides,
  };
}

function render(
  event: EventModel,
  options: {
    registration?: CardRegistration;
    affiliated?: boolean;
    position?: number | null;
    quick?: boolean;
  } = {},
): { fixture: ComponentFixture<EventCard>; element: HTMLElement } {
  TestBed.configureTestingModule({ imports: [EventCard], providers: [provideRouter([])] });
  const fixture = TestBed.createComponent(EventCard);
  fixture.componentRef.setInput('event', event);
  fixture.componentRef.setInput('registration', options.registration ?? null);
  fixture.componentRef.setInput('affiliated', options.affiliated ?? false);
  fixture.componentRef.setInput('waitingPosition', options.position ?? null);
  fixture.componentRef.setInput('quickRegisterEnabled', options.quick ?? false);
  fixture.detectChanges();
  return { fixture, element: fixture.nativeElement as HTMLElement };
}

const text = (element: HTMLElement, selector: string): string =>
  element.querySelector(selector)?.textContent?.replace(/\s+/g, ' ').trim() ?? '';

describe('EventCard', () => {
  it('affiche le titre, la ville, le club et la catégorie, avec un lien vers le détail', () => {
    const { element } = render(makeEvent());

    expect(text(element, '.title')).toBe('Tournoi de tennis');
    expect(text(element, '.facts')).toContain('Toulouse');
    expect(text(element, '.facts')).toContain('Tennis Club');
    expect(text(element, '.category')).toBe('Sport');
    expect(element.querySelector('.title a')?.getAttribute('href')).toBe('/event-details/7');
  });

  it('porte sa catégorie pour appliquer la couleur d’accent correspondante', () => {
    const { element } = render(makeEvent({ category: 'CULTURE' }));

    expect(element.querySelector('.card')?.getAttribute('data-category')).toBe('CULTURE');
  });

  it('affiche le tarif non affilié et rappelle le tarif affilié à un visiteur ou un non-affilié', () => {
    const { element } = render(makeEvent(), { affiliated: false });

    expect(text(element, '.amount')).toContain('20');
    expect(text(element, '.hint')).toContain('10');
    expect(text(element, '.hint')).toContain('affiliés');
  });

  it('affiche le tarif affilié à un membre affilié', () => {
    const { element } = render(makeEvent(), { affiliated: true });

    expect(text(element, '.amount')).toContain('10');
    expect(text(element, '.hint')).toBe('Tarif affilié');
  });

  it('affiche « Gratuit » quand le tarif applicable est nul', () => {
    const { element } = render(makeEvent({ affiliatePrice: 0, nonAffiliatePrice: 0 }));

    expect(text(element, '.amount')).toBe('Gratuit');
    expect(element.querySelector('.hint')).toBeNull();
  });

  it('ne répète pas le tarif quand il est identique pour tous', () => {
    const { element } = render(makeEvent({ affiliatePrice: 15, nonAffiliatePrice: 15 }));

    expect(element.querySelector('.hint')).toBeNull();
  });

  it('indique les places restantes avec une jauge proportionnelle', () => {
    const { element } = render(makeEvent({ remainingSpots: 8, maxCapacity: 10 }));

    expect(text(element, '.spots-label')).toBe('8 places restantes');
    expect(text(element, '.spots-total')).toBe('8 / 10');
    expect((element.querySelector('.fill') as HTMLElement).style.width).toBe('80%');
    expect(element.querySelector('.spots')?.getAttribute('data-level')).toBe('high');
  });

  it('passe la jauge en alerte quand il reste peu de places', () => {
    const medium = render(makeEvent({ remainingSpots: 3, maxCapacity: 10 }));
    expect(medium.element.querySelector('.spots')?.getAttribute('data-level')).toBe('medium');
    TestBed.resetTestingModule();

    const low = render(makeEvent({ remainingSpots: 1, maxCapacity: 20 }));
    expect(text(low.element, '.spots-label')).toBe('1 place restante');
    expect(low.element.querySelector('.spots')?.getAttribute('data-level')).toBe('low');
  });

  it('signale un évènement complet avec la liste d’attente', () => {
    const { element } = render(makeEvent({ remainingSpots: 0, maxCapacity: 10 }));

    expect(text(element, '.spots-label')).toContain('Complet');
    expect(element.querySelector('.spots')?.getAttribute('data-level')).toBe('full');
    expect((element.querySelector('.fill') as HTMLElement).style.width).toBe('0%');
  });

  it('n’a plus de pastille sur la photo : l’inscription vit sur la souche', () => {
    expect(render(makeEvent(), { quick: true }).element.querySelector('.pastille')).toBeNull();
    TestBed.resetTestingModule();
    expect(render(makeEvent(), { registration: 'CONFIRMED' }).element.querySelector('.pastille')).toBeNull();
    TestBed.resetTestingModule();
    expect(render(makeEvent(), { registration: 'WAITING_LIST', position: 3 }).element.querySelector('.pastille')).toBeNull();
  });

  describe('inscrit·e : la souche est arrachée, un reçu la remplace', () => {
    it('affiche un reçu avec le statut, la date et le prix', () => {
      const { element } = render(makeEvent(), { registration: 'CONFIRMED', affiliated: true });

      const receipt = element.querySelector('.receipt');
      expect(receipt).not.toBeNull();
      expect(text(element, '.receipt-badge')).toContain('Inscrit·e');
      expect(text(element, '.receipt-date')).toContain(String(START.getDate()).padStart(2, '0'));
      expect(text(element, '.receipt-price')).toContain('10');
      expect(element.querySelector('.card')?.getAttribute('data-registration')).toBe('CONFIRMED');
    });

    it('ne rend plus la souche cliquable', () => {
      const { element } = render(makeEvent(), { registration: 'CONFIRMED', quick: true });

      const stub = element.querySelector('.stub') as HTMLElement;
      expect(stub.classList).not.toContain('interactive');
      expect(stub.classList).not.toContain('peelable');
      expect(stub.getAttribute('role')).toBeNull();
    });

    it('ouvre la gestion de l’inscription depuis le reçu', () => {
      const { fixture, element } = render(makeEvent(), { registration: 'CONFIRMED' });
      const clicks: number[] = [];
      fixture.componentInstance.registrationClick.subscribe(() => clicks.push(1));

      (element.querySelector('.manage') as HTMLButtonElement).click();

      expect(clicks.length).toBe(1);
    });
  });

  describe('en liste d’attente : pas encore validé·e', () => {
    it('garde la souche en place mais la rend soulevable, avec le rang derrière', () => {
      const { element } = render(makeEvent({ remainingSpots: 0 }), { registration: 'WAITING_LIST', position: 3 });

      const stub = element.querySelector('.stub') as HTMLElement;
      expect(stub.classList).toContain('peelable');
      expect(stub.classList).not.toContain('interactive');
      expect(stub.getAttribute('aria-label')).toBe(
        'Liste d’attente · n°3, gérer mon inscription : Tournoi de tennis',
      );
      expect(text(element, '.slot')).toContain('Liste d’attente · n°3');
      expect(text(element, '.slot')).toContain('Pas encore inscrit·e');
      expect(text(element, '.cta')).toContain('En liste d’attente');
    });

    it('n’annonce pas de rang tant qu’il est inconnu', () => {
      const { element } = render(makeEvent({ remainingSpots: 0 }), { registration: 'WAITING_LIST', position: null });

      expect(text(element, '.slot-title')).toBe('Liste d’attente');
    });

    it('ouvre la gestion de l’inscription au clic sur la souche, sans déclencher d’inscription', () => {
      const { fixture, element } = render(makeEvent({ remainingSpots: 0 }), {
        registration: 'WAITING_LIST',
        quick: true,
      });
      const manage: number[] = [];
      const quick: number[] = [];
      fixture.componentInstance.registrationClick.subscribe(() => manage.push(1));
      fixture.componentInstance.quickRegister.subscribe(() => quick.push(1));

      (element.querySelector('.stub') as HTMLElement).click();

      expect(manage.length).toBe(1);
      expect(quick.length).toBe(0);
    });
  });

  it('affiche le jour et le mois dans la vignette de date', () => {
    const { element } = render(makeEvent());

    expect(text(element, '.day')).toBe(String(START.getDate()).padStart(2, '0'));
    expect(text(element, '.month').length).toBeGreaterThan(0);
  });

  it('affiche le jour de la semaine dans la souche, et la fin seulement pour plusieurs jours', () => {
    const oneDay = render(makeEvent());
    expect(text(oneDay.element, '.weekday').length).toBeGreaterThan(0);
    expect(oneDay.element.querySelector('.until')).toBeNull();
    TestBed.resetTestingModule();

    const end = new Date(START.getTime() + 2 * DAY);
    const multi = render(makeEvent({ endDateTime: end }));
    expect(text(multi.element, '.until')).toContain(String(end.getDate()));
    expect(text(multi.element, '.day')).toBe(String(START.getDate()).padStart(2, '0'));
  });

  it('détaille un évènement d’un jour sur deux lignes : la date, puis l’horaire', () => {
    const { element } = render(makeEvent());

    const lines = element.querySelectorAll('.when-text > span');
    expect(lines[0].textContent?.toLowerCase()).toContain(String(START.getDate()));
    expect(lines[1].textContent?.trim()).toBe('de 18:00 à 20:00');
  });

  it('détaille un évènement de plusieurs jours : début puis fin, chacun avec sa date et son heure', () => {
    const end = new Date(START.getTime() + DAY);
    end.setHours(17, 30, 0, 0);
    const { element } = render(makeEvent({ endDateTime: end }));

    const lines = element.querySelectorAll('.when-text > span');
    expect(lines[0].textContent?.trim()).toMatch(/^Du .+ à 18:00$/);
    expect(lines[1].textContent?.trim()).toMatch(/^au .+ à 17:30$/);
    expect(lines[0].textContent).toContain(String(START.getDate()));
    expect(lines[1].textContent).toContain(String(end.getDate()));
  });

  // ---------------------------------------------------------------
  // Inscription directe : la souche se détache
  // ---------------------------------------------------------------

  describe('inscription directe depuis la souche', () => {
    beforeEach(() => vi.useFakeTimers());
    afterEach(() => vi.useRealTimers());

    const stub = (element: HTMLElement) => element.querySelector('.stub') as HTMLElement;
    const phase = (element: HTMLElement) => element.querySelector('.card')?.getAttribute('data-phase');

    it('rend la souche cliquable seulement quand l’inscription directe est possible', () => {
      const ok = render(makeEvent(), { quick: true });
      expect(stub(ok.element).classList).toContain('interactive');
      expect(stub(ok.element).getAttribute('role')).toBe('button');
      expect(stub(ok.element).getAttribute('aria-label')).toBe('Détacher pour s’inscrire : Tournoi de tennis');
      TestBed.resetTestingModule();

      expect(stub(render(makeEvent(), { quick: false }).element).classList).not.toContain('interactive');
      TestBed.resetTestingModule();
      expect(stub(render(makeEvent(), { quick: true, registration: 'CONFIRMED' }).element).classList).not.toContain(
        'interactive',
      );
      TestBed.resetTestingModule();
      expect(stub(render(makeEvent({ status: 'CANCELLED' }), { quick: true }).element).classList).not.toContain(
        'interactive',
      );
    });

    it('propose la liste d’attente sur la souche d’un évènement complet', () => {
      const { element } = render(makeEvent({ remainingSpots: 0 }), { quick: true });

      expect(stub(element).getAttribute('aria-label')).toBe(
        'Détacher pour rejoindre la liste d’attente : Tournoi de tennis',
      );
    });

    it('un clic détache la souche et demande l’inscription, une seule fois', () => {
      const { fixture, element } = render(makeEvent(), { quick: true });
      const requests: number[] = [];
      fixture.componentInstance.quickRegister.subscribe(() => requests.push(1));

      stub(element).click();
      fixture.detectChanges();
      stub(element).click();

      expect(requests.length).toBe(1);
      expect(phase(element)).toBe('tearing');
      expect(stub(element).classList).not.toContain('interactive');
    });

    it('passe en attente après le détachement, puis confirme l’inscription quand elle arrive', () => {
      const { fixture, element } = render(makeEvent(), { quick: true });
      stub(element).click();
      fixture.detectChanges();

      vi.advanceTimersByTime(700);
      fixture.detectChanges();
      expect(phase(element)).toBe('pending');
      expect(text(element, '.slot')).toContain('Inscription en cours');

      fixture.componentRef.setInput('registration', 'CONFIRMED');
      fixture.detectChanges();
      vi.advanceTimersByTime(10);
      fixture.detectChanges();

      expect(phase(element)).toBe('confirm');
      expect(text(element, '.slot')).toContain('Vous êtes inscrit·e');
    });

    it('annonce le rang quand l’inscription tombe en liste d’attente', () => {
      const { fixture, element } = render(makeEvent({ remainingSpots: 0 }), { quick: true });
      stub(element).click();
      vi.advanceTimersByTime(700);
      fixture.componentRef.setInput('waitingPosition', 3);
      fixture.componentRef.setInput('registration', 'WAITING_LIST');
      fixture.detectChanges();
      vi.advanceTimersByTime(10);
      fixture.detectChanges();

      expect(phase(element)).toBe('confirm');
      expect(text(element, '.slot')).toContain('Liste d’attente · n°3');
    });

    it('attend la fin du détachement avant d’afficher le résultat, même si le serveur répond vite', () => {
      const { fixture, element } = render(makeEvent(), { quick: true });
      stub(element).click();
      fixture.componentRef.setInput('registration', 'CONFIRMED');
      fixture.detectChanges();

      vi.advanceTimersByTime(200);
      fixture.detectChanges();
      expect(phase(element)).toBe('tearing');

      vi.advanceTimersByTime(520);
      fixture.detectChanges();
      expect(phase(element)).toBe('confirm');
    });

    it('inscription validée : la souche reste arrachée et un reçu prend sa place', () => {
      const { fixture, element } = render(makeEvent(), { quick: true });
      stub(element).click();
      vi.advanceTimersByTime(700);
      fixture.componentRef.setInput('registration', 'CONFIRMED');
      fixture.detectChanges();
      vi.advanceTimersByTime(10);
      fixture.detectChanges();
      expect(phase(element)).toBe('confirm');

      vi.advanceTimersByTime(2600);
      fixture.detectChanges();

      expect(phase(element)).toBe('idle');
      expect(element.querySelector('.receipt')).not.toBeNull();
      expect(stub(element).classList).not.toContain('interactive');
    });

    it('liste d’attente : la souche revient en fondu, soulevable, avec le rang derrière', () => {
      const { fixture, element } = render(makeEvent({ remainingSpots: 0 }), { quick: true });
      stub(element).click();
      vi.advanceTimersByTime(700);
      fixture.componentRef.setInput('waitingPosition', 2);
      fixture.componentRef.setInput('registration', 'WAITING_LIST');
      fixture.detectChanges();
      vi.advanceTimersByTime(10);
      fixture.detectChanges();
      expect(phase(element)).toBe('confirm');

      vi.advanceTimersByTime(2600);
      fixture.detectChanges();
      expect(phase(element)).toBe('returning');

      vi.advanceTimersByTime(700);
      fixture.detectChanges();
      expect(phase(element)).toBe('idle');
      expect(stub(element).classList).toContain('peelable');
      expect(text(element, '.slot')).toContain('Liste d’attente · n°2');
      expect(text(element, '.wait')).toContain(String(START.getDate()).padStart(2, '0'));
      expect(text(element, '.wait')).toContain('n°2');
    });

    it('affiche l’erreur du serveur puis rend la souche', () => {
      const { fixture, element } = render(makeEvent(), { quick: true });
      stub(element).click();
      vi.advanceTimersByTime(700);
      fixture.componentRef.setInput('registerError', 'Les inscriptions sont closes.');
      fixture.detectChanges();
      vi.advanceTimersByTime(10);
      fixture.detectChanges();

      expect(phase(element)).toBe('confirm');
      expect(text(element, '.slot')).toContain('Inscription impossible');
      expect(text(element, '.slot')).toContain('Les inscriptions sont closes.');

      vi.advanceTimersByTime(3300);
      fixture.detectChanges();
      expect(phase(element)).toBe('idle');
      expect(stub(element).classList).toContain('interactive');
    });

    it('ne reste pas bloquée si le serveur ne répond jamais', () => {
      const { fixture, element } = render(makeEvent(), { quick: true });
      stub(element).click();
      vi.advanceTimersByTime(8100);
      fixture.detectChanges();

      expect(phase(element)).toBe('confirm');
      expect(text(element, '.slot')).toContain('Inscription impossible');
    });

    it('s’active aussi au clavier (Entrée)', () => {
      const { fixture, element } = render(makeEvent(), { quick: true });
      const requests: number[] = [];
      fixture.componentInstance.quickRegister.subscribe(() => requests.push(1));

      stub(element).dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', bubbles: true }));

      expect(requests.length).toBe(1);
    });
  });
});
