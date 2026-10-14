import { InscriptionModel, toCalendarEvent } from './calendar.models';

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
