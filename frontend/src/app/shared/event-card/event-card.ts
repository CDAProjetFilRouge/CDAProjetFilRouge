import {
  Component,
  DestroyRef,
  ElementRef,
  HostListener,
  computed,
  effect,
  inject,
  input,
  output,
  signal,
  untracked,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { URL_IMAGES } from '../../core/api/api.config';
import { EventModel } from '../../features/events/event/event-model';

export type CardRegistration = 'CONFIRMED' | 'WAITING_LIST' | null;
export type SpotsLevel = 'full' | 'low' | 'medium' | 'high';

// Étapes de l'inscription directe depuis la souche du billet :
// idle -> tearing (la souche se détache) -> pending (attente du serveur) -> confirm (message)
// -> returning (la souche revient en fondu) -> idle.
export type StubPhase = 'idle' | 'tearing' | 'pending' | 'confirm' | 'returning';

const CATEGORY_LABELS: Record<string, string> = {
  CULTURE: 'Culture',
  SPORT: 'Sport',
  DIVERTISSEMENT: 'Divertissement',
};

const LOW_RATIO = 0.15;
const MEDIUM_RATIO = 0.4;

const TEAR_MS = 680;
const CONFIRM_MS = 2600;
const RETURN_MS = 600;
const NO_ANSWER_MS = 8000;

// Inclinaison maximale du billet qui suit le curseur, en degrés.
const MAX_TILT = 3;

const euro = new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR', maximumFractionDigits: 2 });
const dayFormat = new Intl.DateTimeFormat('fr-FR', { day: '2-digit' });
const monthFormat = new Intl.DateTimeFormat('fr-FR', { month: 'short' });
const weekdayFormat = new Intl.DateTimeFormat('fr-FR', { weekday: 'short' });
const untilFormat = new Intl.DateTimeFormat('fr-FR', { day: 'numeric', month: 'short' });
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

  // Inscription directe : un clic sur la souche la détache et demande l'inscription.
  readonly quickRegisterEnabled = input(false);
  readonly registerError = input<string | null>(null);
  readonly quickRegister = output<void>();

  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly timers = new Set<ReturnType<typeof setTimeout>>();
  private tornAt = 0;
  private settling = false;

  protected readonly phase = signal<StubPhase>('idle');
  protected readonly resultKind = signal<'ok' | 'error' | null>(null);

  constructor() {
    inject(DestroyRef).onDestroy(() => this.clearTimers());

    // Le résultat arrive de la page : l'inscription apparaît (succès) ou une erreur est signalée.
    effect(() => {
      const registration = this.registration();
      const error = this.registerError();
      const phase = untracked(this.phase);
      if (phase !== 'tearing' && phase !== 'pending') {
        return;
      }
      if (error) {
        this.settle('error');
      } else if (registration) {
        this.settle('ok');
      }
    });
  }

  protected readonly stubInteractive = computed(
    () =>
      this.quickRegisterEnabled() && this.canRegister() && this.registration() === null && this.phase() === 'idle',
  );

  // En liste d'attente, l'inscription n'est pas validée : la souche reste en place, mais se soulève
  // au survol pour montrer ce qu'il y a derrière. Un clic ouvre la gestion de l'inscription.
  protected readonly peelable = computed(() => this.registration() === 'WAITING_LIST' && this.phase() === 'idle');

  protected readonly waitingTitle = computed(() => {
    const position = this.waitingPosition();
    return position ? `Liste d’attente · n°${position}` : 'Liste d’attente';
  });

  protected readonly waitingLabel = computed(
    () => `${this.waitingTitle()}, gérer mon inscription : ${this.event().title}`,
  );

  protected readonly stubLabel = computed(() =>
    this.full()
      ? `Détacher pour rejoindre la liste d’attente : ${this.event().title}`
      : `Détacher pour s’inscrire : ${this.event().title}`,
  );

  protected readonly confirmTitle = computed(() => {
    if (this.resultKind() === 'error') {
      return 'Inscription impossible';
    }
    if (this.registration() === 'WAITING_LIST') {
      const position = this.waitingPosition();
      return position ? `Liste d’attente · n°${position}` : 'Liste d’attente';
    }
    return 'Vous êtes inscrit·e';
  });

  protected readonly confirmDetail = computed(() => {
    if (this.resultKind() === 'error') {
      return this.registerError() ?? 'Réessayez dans un instant.';
    }
    return this.registration() === 'WAITING_LIST' ? 'Dans l’ordre d’arrivée' : 'Votre place est réservée';
  });

  protected onStubClick(event: Event): void {
    if (this.peelable()) {
      event.preventDefault();
      event.stopPropagation();
      this.registrationClick.emit();
      return;
    }
    if (!this.stubInteractive()) {
      return;
    }
    event.preventDefault();
    event.stopPropagation();
    this.phase.set('tearing');
    this.resultKind.set(null);
    this.settling = false;
    this.tornAt = Date.now();
    this.quickRegister.emit();
    this.later(TEAR_MS, () => {
      if (this.phase() === 'tearing') {
        this.phase.set('pending');
      }
    });
    // Sécurité : sans réponse de la page, on ne laisse pas la souche absente indéfiniment.
    this.later(NO_ANSWER_MS, () => {
      if (this.phase() === 'tearing' || this.phase() === 'pending') {
        this.settle('error');
      }
    });
  }

  private settle(kind: 'ok' | 'error'): void {
    if (this.settling) {
      return;
    }
    this.settling = true;
    const wait = Math.max(0, TEAR_MS - (Date.now() - this.tornAt));
    this.later(wait, () => {
      this.resultKind.set(kind);
      this.phase.set('confirm');
      this.later(CONFIRM_MS, () => {
        if (kind === 'ok' && this.registration() === 'CONFIRMED') {
          // Inscription validée : la souche est définitivement détachée, un reçu prend sa place.
          this.phase.set('idle');
          this.resultKind.set(null);
          this.settling = false;
          return;
        }
        this.phase.set('returning');
        this.later(RETURN_MS, () => {
          this.phase.set('idle');
          this.resultKind.set(null);
          this.settling = false;
        });
      });
    });
  }

  private later(ms: number, action: () => void): void {
    const handle = setTimeout(() => {
      this.timers.delete(handle);
      action();
    }, ms);
    this.timers.add(handle);
  }

  private clearTimers(): void {
    this.timers.forEach((handle) => clearTimeout(handle));
    this.timers.clear();
  }

  // Le billet s'incline vers le curseur et la brillance holographique le suit. Les mesures sont regroupées en
  // une écriture par image affichée, et chaque variable n'est posée que sur l'élément qui s'en sert : une
  // variable posée sur l'hôte force le navigateur à recalculer le style de tout le billet à chaque mouvement.
  private frame = 0;
  private pointer: { x: number; y: number } | null = null;

  @HostListener('pointermove', ['$event'])
  protected onPointerMove(event: PointerEvent): void {
    if (event.pointerType === 'touch') {
      return;
    }
    const rect = this.host.nativeElement.getBoundingClientRect();
    this.pointer = {
      x: (event.clientX - rect.left) / rect.width,
      y: (event.clientY - rect.top) / rect.height,
    };
    if (!this.frame) {
      this.frame = requestAnimationFrame(() => this.applyPointer());
    }
  }

  @HostListener('pointerleave')
  protected onPointerLeave(): void {
    cancelAnimationFrame(this.frame);
    this.frame = 0;
    this.pointer = null;
    for (const el of this.tracked()) {
      for (const name of ['--mx', '--my', '--rx', '--ry']) {
        el.style.removeProperty(name);
      }
    }
  }

  private tracked(): HTMLElement[] {
    const root = this.host.nativeElement;
    return [root.querySelector('.card'), root.querySelector('.holo'), root.querySelector('.foil')].filter(
      (el): el is HTMLElement => el !== null,
    );
  }

  private applyPointer(): void {
    this.frame = 0;
    const pointer = this.pointer;
    if (!pointer) {
      return;
    }
    const root = this.host.nativeElement;
    const card = root.querySelector<HTMLElement>('.card');
    card?.style.setProperty('--ry', `${((pointer.x - 0.5) * 2 * MAX_TILT).toFixed(2)}deg`);
    card?.style.setProperty('--rx', `${((0.5 - pointer.y) * 2 * MAX_TILT).toFixed(2)}deg`);
    for (const el of [root.querySelector<HTMLElement>('.holo'), root.querySelector<HTMLElement>('.foil')]) {
      el?.style.setProperty('--mx', `${(pointer.x * 100).toFixed(1)}%`);
      el?.style.setProperty('--my', `${(pointer.y * 100).toFixed(1)}%`);
    }
  }

  protected readonly imageUrl = computed(() => {
    const first = [...this.event().imageGallery].sort((a, b) => a.displayOrder - b.displayOrder)[0];
    return first ? `${URL_IMAGES}${first.path}` : null;
  });

  protected readonly categoryLabel = computed(
    () => CATEGORY_LABELS[this.event().category] ?? this.event().category,
  );

  // Souche du billet : jour de la semaine, jour et mois de début ; pour une plage de plusieurs jours,
  // une petite étiquette « → 13 déc. » donne la fin.
  protected readonly dateTile = computed(() => {
    const start = new Date(this.event().startDateTime);
    const end = new Date(this.event().endDateTime);
    const multiDay = start.toDateString() !== end.toDateString();
    return {
      weekday: weekdayFormat.format(start).replace('.', ''),
      day: dayFormat.format(start),
      month: monthFormat.format(start).replace('.', ''),
      until: multiDay ? `→ ${untilFormat.format(end).replace('.', '')}` : '',
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
