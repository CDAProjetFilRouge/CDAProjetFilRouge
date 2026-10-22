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
    status?: 'DRAFT' | 'PUBLISHED' | 'CANCELLED' | 'FINISHED';
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

export type OrganizedEventStatus = 'DRAFT' | 'PUBLISHED' | 'CANCELLED' | 'FINISHED';

// Réponse de GET /events/mine/calendar.
export interface OrganizedEventModel {
  id: string;
  title: string;
  startDateTime: string;
  endDateTime: string;
  status: OrganizedEventStatus;
  maxCapacity: number | null;
  confirmedCount: number;
  clubName?: string | null;
}

export const ORGANIZED_STATUS_LABELS: Record<OrganizedEventStatus, string> = {
  DRAFT: 'Brouillon',
  PUBLISHED: 'Publié',
  CANCELLED: 'Annulé',
  FINISHED: 'Terminé',
};

export const ORGANIZED_ID_PREFIX = 'org:';

export function fillLabel(event: OrganizedEventModel): string {
  return event.maxCapacity ? `${event.confirmedCount}/${event.maxCapacity}` : `${event.confirmedCount}`;
}

// Un évènement organisé vu comme un évènement du calendrier. Si l'organisateur est aussi inscrit à son propre
// évènement, `inscription` ajoute son badge : l'évènement n'apparaît qu'une fois, avec les deux informations.
export function toOrganizedCalendarEvent(event: OrganizedEventModel, inscription?: InscriptionModel): CalendarEvent {
  const badges = [`${ORGANIZED_STATUS_LABELS[event.status]} · ${fillLabel(event)}`];
  if (inscription) {
    badges.push(inscription.status === 'WAITING_LIST' ? 'Liste d’attente' : 'Inscrit·e');
  }
  return {
    id: `${ORGANIZED_ID_PREFIX}${event.id}`,
    title: event.title,
    start: event.startDateTime,
    end: event.endDateTime,
    badge: badges.join(' · '),
    club: event.clubName,
    organized: true,
    status: event.status,
  };
}
