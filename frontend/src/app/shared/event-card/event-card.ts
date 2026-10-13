import { Component, computed, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { URL_IMAGES } from '../../core/api/api.config';
import { EventModel } from '../../features/events/event/event-model';

export type CardRegistration = 'CONFIRMED' | 'WAITING_LIST' | null;
export type SpotsLevel = 'full' | 'low' | 'medium' | 'high';

const CATEGORY_LABELS: Record<string, string> = {
  CULTURE: 'Culture',
  SPORT: 'Sport',
  DIVERTISSEMENT: 'Divertissement',
};

const LOW_RATIO = 0.15;
const MEDIUM_RATIO = 0.4;

const euro = new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR', maximumFractionDigits: 2 });
const dayFormat = new Intl.DateTimeFormat('fr-FR', { day: '2-digit' });
const monthFormat = new Intl.DateTimeFormat('fr-FR', { month: 'short' });
const longDateFormat = new Intl.DateTimeFormat('fr-FR', { weekday: 'long', day: 'numeric', month: 'long' });
const shortDateFormat = new Intl.DateTimeFormat('fr-FR', { weekday: 'short', day: 'numeric', month: 'short' });
const timeFormat = new Intl.DateTimeFormat('fr-FR', { hour: '2-digit', minute: '2-digit' });

@Component({
  imports: [RouterLink],
  selector: 'app-event-card',
  styleUrl: './event-card.scss',
  templateUrl: './event-card.html',
})
export class EventCard {
  readonly event = input.required<EventModel>();
  readonly registration = input<CardRegistration>(null);
  readonly affiliated = input(false);
  readonly waitingPosition = input<number | null>(null);
  readonly registrationClick = output<void>();

  protected readonly imageUrl = computed(() => {
    const first = [...this.event().imageGallery].sort((a, b) => a.displayOrder - b.displayOrder)[0];
    return first ? `${URL_IMAGES}${first.path}` : null;
  });

  protected readonly categoryLabel = computed(
    () => CATEGORY_LABELS[this.event().category] ?? this.event().category,
  );

  protected readonly dateTile = computed(() => {
    const start = new Date(this.event().startDateTime);
    return {
      day: dayFormat.format(start),
      month: monthFormat.format(start).replace('.', ''),
    };
  });

  // Deux lignes pour toutes les cartes, afin que les blocs restent alignés :
  // un jour -> « Dimanche 6 décembre » / « de 18:00 à 20:00 »,
  // plusieurs jours -> « Du sam. 12 déc. à 09:00 » / « au dim. 13 déc. à 17:00 ».
  protected readonly dateLines = computed(() => {
    const start = new Date(this.event().startDateTime);
    const end = new Date(this.event().endDateTime);
    if (start.toDateString() === end.toDateString()) {
      return {
        first: longDateFormat.format(start),
        second: `de ${timeFormat.format(start)} à ${timeFormat.format(end)}`,
      };
    }
    return {
      first: `Du ${shortDateFormat.format(start)} à ${timeFormat.format(start)}`,
      second: `au ${shortDateFormat.format(end)} à ${timeFormat.format(end)}`,
    };
  });

  protected readonly addLabel = computed(() =>
    this.full()
      ? `Rejoindre la liste d’attente : ${this.event().title}`
      : `S’inscrire : ${this.event().title}`,
  );

  protected readonly registeredLabel = computed(() => {
    if (this.registration() !== 'WAITING_LIST') {
      return `Inscrit·e, gérer mon inscription : ${this.event().title}`;
    }
    const position = this.waitingPosition();
    const rank = position ? `, n°${position}` : '';
    return `Sur liste d’attente${rank}, gérer mon inscription : ${this.event().title}`;
  });

  protected readonly registeredTooltip = computed(() => {
    if (this.registration() !== 'WAITING_LIST') {
      return 'Inscrit·e';
    }
    const position = this.waitingPosition();
    return position ? `Liste d’attente : n°${position}` : 'Liste d’attente';
  });

  protected readonly canRegister = computed(() => {
    const { status, endDateTime } = this.event();
    return status === 'PUBLISHED' && new Date(endDateTime).getTime() > Date.now();
  });

  protected readonly full = computed(() => this.event().remainingSpots <= 0);

  protected readonly spotsRatio = computed(() => {
    const { remainingSpots, maxCapacity } = this.event();
    return maxCapacity > 0 ? Math.min(1, Math.max(0, remainingSpots / maxCapacity)) : 0;
  });

  protected readonly spotsLevel = computed<SpotsLevel>(() => {
    if (this.full()) {
      return 'full';
    }
    const ratio = this.spotsRatio();
    if (ratio <= LOW_RATIO) {
      return 'low';
    }
    return ratio <= MEDIUM_RATIO ? 'medium' : 'high';
  });

  protected readonly spotsLabel = computed(() => {
    const remaining = this.event().remainingSpots;
    if (this.full()) {
      return 'Complet · liste d’attente';
    }
    return remaining === 1 ? '1 place restante' : `${remaining} places restantes`;
  });

  protected readonly price = computed(() => {
    const { affiliatePrice, nonAffiliatePrice } = this.event();
    const amount = this.affiliated() ? affiliatePrice : nonAffiliatePrice;
    return {
      label: amount === 0 ? 'Gratuit' : euro.format(amount),
      hint: this.priceHint(affiliatePrice, nonAffiliatePrice),
    };
  });

  private priceHint(affiliatePrice: number, nonAffiliatePrice: number): string {
    if (this.affiliated()) {
      return 'Tarif affilié';
    }
    if (affiliatePrice < nonAffiliatePrice) {
      return `${affiliatePrice === 0 ? 'Gratuit' : euro.format(affiliatePrice)} pour les affiliés`;
    }
    return '';
  }
}
