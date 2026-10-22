import {
  fillLabel,
  InscriptionModel,
  OrganizedEventModel,
  toCalendarEvent,
  toOrganizedCalendarEvent,
} from './calendar.models';

function inscription(overrides: Partial<InscriptionModel> = {}): InscriptionModel {
  return {
    id: '4',
    event: {
      id: '9',
      title: 'Tournoi de badminton',
      startDateTime: '2026-10-15T09:00:00',
      endDateTime: '2026-10-15T13:00:00',
      clubName: 'Club Athlétisme Rennais',
    },
    inscriptionDate: '2026-10-01T10:00:00',
    status: 'CONFIRMED',
    price: 6,
    ...overrides,
  };
}

describe('toCalendarEvent', () => {
  it('reprend le titre, les dates et le club de l’évènement', () => {
    const event = toCalendarEvent(inscription(), { id: '4' });

    expect(event).toMatchObject({
      id: '4',
      title: 'Tournoi de badminton',
      start: '2026-10-15T09:00:00',
      end: '2026-10-15T13:00:00',
      club: 'Club Athlétisme Rennais',
    });
  });

  it('estompe et signale seulement la liste d’attente par défaut', () => {
    const confirmed = toCalendarEvent(inscription(), { id: '4' });
    const waiting = toCalendarEvent(inscription({ status: 'WAITING_LIST' }), { id: '4' });

    expect(confirmed.muted).toBe(false);
    expect(confirmed.badge).toBeUndefined();
    expect(waiting.muted).toBe(true);
    expect(waiting.badge).toBe('Liste d’attente');
  });

  it('peut estomper toutes les inscriptions et signaler aussi les confirmées', () => {
    const confirmed = toCalendarEvent(inscription(), { id: '9', muted: true, confirmedBadge: true });
    const waiting = toCalendarEvent(inscription({ status: 'WAITING_LIST' }), { id: '9', muted: true, confirmedBadge: true });

    expect(confirmed.muted).toBe(true);
    expect(confirmed.badge).toBe('Inscrit·e');
    expect(waiting.badge).toBe('Liste d’attente');
  });

  it('laisse le club vide quand le serveur ne le fournit pas', () => {
    const event = toCalendarEvent(inscription({ event: { ...inscription().event, clubName: undefined } }), { id: '4' });

    expect(event.club).toBeUndefined();
  });
});

function organized(overrides: Partial<OrganizedEventModel> = {}): OrganizedEventModel {
  return {
    id: '9',
    title: 'Tournoi de badminton',
    startDateTime: '2026-10-15T09:00:00',
    endDateTime: '2026-10-15T13:00:00',
    status: 'PUBLISHED',
    maxCapacity: 30,
    confirmedCount: 18,
    clubName: 'Club Athlétisme Rennais',
    ...overrides,
  };
}

describe('toOrganizedCalendarEvent', () => {
  it('marque l’évènement comme organisé, avec son statut, son remplissage et un identifiant distinct', () => {
    const event = toOrganizedCalendarEvent(organized());

    expect(event).toMatchObject({
      id: 'org:9',
      title: 'Tournoi de badminton',
      organized: true,
      status: 'PUBLISHED',
      badge: 'Publié · 18/30',
      club: 'Club Athlétisme Rennais',
    });
  });

  it('ajoute le badge d’inscription quand l’organisateur participe à son évènement', () => {
    const confirmed = toOrganizedCalendarEvent(organized(), inscription({ event: { ...inscription().event, id: '9' } }));
    const waiting = toOrganizedCalendarEvent(
      organized(),
      inscription({ status: 'WAITING_LIST', event: { ...inscription().event, id: '9' } }),
    );

    expect(confirmed.badge).toBe('Publié · 18/30 · Inscrit·e');
    expect(waiting.badge).toBe('Publié · 18/30 · Liste d’attente');
  });

  it('montre les statuts en français, annulé et brouillon compris', () => {
    expect(toOrganizedCalendarEvent(organized({ status: 'CANCELLED' })).badge).toContain('Annulé');
    expect(toOrganizedCalendarEvent(organized({ status: 'DRAFT' })).badge).toContain('Brouillon');
    expect(toOrganizedCalendarEvent(organized({ status: 'FINISHED' })).badge).toContain('Terminé');
  });
});

describe('fillLabel', () => {
  it('affiche inscrits/capacité, ou seulement les inscrits sans capacité', () => {
    expect(fillLabel(organized())).toBe('18/30');
    expect(fillLabel(organized({ maxCapacity: null }))).toBe('18');
  });
});
