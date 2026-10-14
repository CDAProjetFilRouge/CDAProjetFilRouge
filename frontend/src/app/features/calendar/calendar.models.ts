import { CalendarEvent } from '../../shared/event-calendar/event-calendar.models';

export type InscriptionStatus = 'CONFIRMED' | 'WAITING_LIST' | 'CANCELED';

export interface InscriptionModel {
  id: string;
  event: {
    id: string;
    title: string;
    startDateTime: string;
    endDateTime: string;
    clubName?: string | null;
  };
  inscriptionDate: string;
  status: InscriptionStatus;
  price: number;
  waitingListPosition?: number | null;
}

export const INSCRIPTION_STATUS_LABELS: Record<InscriptionStatus, string> = {
  CONFIRMED: 'Confirmée',
  WAITING_LIST: "Liste d'attente",
  CANCELED: 'Annulée',
};

// Une inscription vue comme un évènement du calendrier partagé. Toutes les pages qui montrent des inscriptions
// dans ce calendrier passent par ici : la bulle de survol (titre, dates, club, badge) reste donc la même partout.
export interface InscriptionEventOptions {
  // Identifiant de l'évènement du calendrier : celui de l'inscription, ou celui de l'évènement.
  id: string;
  // Estompé (évènement qui n'est pas la page courante). Par défaut : seulement en liste d'attente.
  muted?: boolean;
  // Affiche aussi « Inscrit·e » sur les inscriptions confirmées (utile quand le calendrier mélange les états).
  confirmedBadge?: boolean;
}

export function toCalendarEvent(inscription: InscriptionModel, options: InscriptionEventOptions): CalendarEvent {
  const waiting = inscription.status === 'WAITING_LIST';
  let badge: string | undefined;
  if (waiting) {
    badge = 'Liste d’attente';
  } else if (options.confirmedBadge) {
    badge = 'Inscrit·e';
  }
  return {
    id: options.id,
    title: inscription.event.title,
    start: inscription.event.startDateTime,
    end: inscription.event.endDateTime,
    muted: options.muted ?? waiting,
    badge,
    club: inscription.event.clubName,
  };
}
