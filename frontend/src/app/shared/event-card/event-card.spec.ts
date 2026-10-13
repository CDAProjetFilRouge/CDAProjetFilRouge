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
  options: { registration?: CardRegistration; affiliated?: boolean; position?: number | null } = {},
): { fixture: ComponentFixture<EventCard>; element: HTMLElement } {
  TestBed.configureTestingModule({ imports: [EventCard], providers: [provideRouter([])] });
  const fixture = TestBed.createComponent(EventCard);
  fixture.componentRef.setInput('event', event);
  fixture.componentRef.setInput('registration', options.registration ?? null);
  fixture.componentRef.setInput('affiliated', options.affiliated ?? false);
  fixture.componentRef.setInput('waitingPosition', options.position ?? null);
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

  it('montre une coche verte quand la personne est inscrite, sans le « + »', () => {
    const { element } = render(makeEvent(), { registration: 'CONFIRMED' });

    const pastille = element.querySelector('.pastille.registration');
    expect(pastille?.getAttribute('aria-label')).toBe('Inscrit·e, gérer mon inscription : Tournoi de tennis');
    expect(pastille?.classList).not.toContain('waiting');
    expect(element.querySelector('.pastille.add')).toBeNull();
  });

  it('montre un sablier ambre en liste d’attente', () => {
    const { element } = render(makeEvent(), { registration: 'WAITING_LIST' });

    const pastille = element.querySelector('.pastille.registration');
    expect(pastille?.getAttribute('aria-label')).toBe(
      'Sur liste d’attente, gérer mon inscription : Tournoi de tennis',
    );
    expect(pastille?.classList).toContain('waiting');
    expect(element.querySelector('.pastille.add')).toBeNull();
  });

  it('affiche le rang dans la pastille de liste d’attente quand il est connu', () => {
    const { element } = render(makeEvent(), { registration: 'WAITING_LIST', position: 3 });

    const pastille = element.querySelector('.pastille.registration');
    expect(text(element, '.rank')).toBe('n°3');
    expect(pastille?.getAttribute('title')).toBe('Liste d’attente : n°3');
    expect(pastille?.getAttribute('aria-label')).toBe(
      'Sur liste d’attente, n°3, gérer mon inscription : Tournoi de tennis',
    );
  });

  it('garde le sablier quand le rang n’est pas connu', () => {
    const { element } = render(makeEvent(), { registration: 'WAITING_LIST', position: null });

    expect(element.querySelector('.rank')).toBeNull();
    expect(element.querySelector('.pastille.registration svg')).not.toBeNull();
    expect(element.querySelector('.pastille.registration')?.getAttribute('title')).toBe('Liste d’attente');
  });

  it('propose le « + » quand la personne peut s’inscrire', () => {
    const { element } = render(makeEvent());

    expect(element.querySelector('.pastille.add')).not.toBeNull();
    expect(element.querySelector('.pastille.registration')).toBeNull();
  });

  it('émet registrationClick quand on clique sur le « + », sans suivre le lien de la carte', () => {
    const { fixture, element } = render(makeEvent());
    const clicks: number[] = [];
    fixture.componentInstance.registrationClick.subscribe(() => clicks.push(1));

    const add = element.querySelector('.pastille.add') as HTMLButtonElement;
    expect(add.tagName).toBe('BUTTON');
    expect(add.getAttribute('aria-label')).toBe('S’inscrire : Tournoi de tennis');
    add.click();

    expect(clicks.length).toBe(1);
  });

  it('propose la liste d’attente sur le « + » quand l’évènement est complet', () => {
    const { element } = render(makeEvent({ remainingSpots: 0 }));

    expect(element.querySelector('.pastille.add')?.getAttribute('aria-label')).toBe(
      'Rejoindre la liste d’attente : Tournoi de tennis',
    );
  });

  it('émet aussi registrationClick depuis la coche, pour gérer son inscription', () => {
    const { fixture, element } = render(makeEvent(), { registration: 'CONFIRMED' });
    const clicks: number[] = [];
    fixture.componentInstance.registrationClick.subscribe(() => clicks.push(1));

    (element.querySelector('.pastille.registration') as HTMLButtonElement).click();

    expect(clicks.length).toBe(1);
  });

  it('n’affiche aucune pastille quand l’évènement est fermé aux inscriptions', () => {
    const cancelled = render(makeEvent({ status: 'CANCELLED' }));
    expect(cancelled.element.querySelector('.pastille')).toBeNull();
    TestBed.resetTestingModule();

    const past = render(
      makeEvent({
        startDateTime: new Date(Date.now() - 3 * DAY),
        endDateTime: new Date(Date.now() - 2 * DAY),
      }),
    );
    expect(past.element.querySelector('.pastille')).toBeNull();
  });

  it('affiche le jour et le mois dans la vignette de date', () => {
    const { element } = render(makeEvent());

    expect(text(element, '.day')).toBe(String(START.getDate()).padStart(2, '0'));
    expect(text(element, '.month').length).toBeGreaterThan(0);
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
});
