export interface CalendarRange {
  from: string;
  to: string;
}

export interface CalendarEvent {
  id: string;
  title: string;
  start: string;
  end?: string;
  muted?: boolean;
  badge?: string;
  club?: string | null;
  // Évènement que l'utilisateur organise (contour), par opposition à une inscription (fond plein).
  organized?: boolean;
  // Statut de l'évènement organisé, pour son style (brouillon en pointillés, annulé barré, terminé estompé).
  status?: 'DRAFT' | 'PUBLISHED' | 'CANCELLED' | 'FINISHED';
}
