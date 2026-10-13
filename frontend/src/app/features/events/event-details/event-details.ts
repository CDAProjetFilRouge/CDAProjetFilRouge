import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { EventDetailsModel, EventStatus } from './event-details-model';
import { EventDetailsService } from './event-details-service';
import { CommentModel } from './event-details-comments-model';
import { InscriptionModel } from './event-details-inscription-model';
import { TimeAgoPipe } from '../../../shared/pipes/time-ago-pipe';
import { CalendarService } from '../../calendar/calendar-service';
import { InscriptionModel as AgendaInscription } from '../../calendar/calendar.models';
import { CalendarEvent } from '../../../shared/event-calendar/event-calendar.models';
import { EventCalendar } from '../../../shared/event-calendar/event-calendar';
import { InscriptionDialog } from './inscription-dialog/inscription-dialog';
import { AuthService } from '../../../core/auth/auth.service';
import { URL_IMAGES } from '../../../core/api/api.config';

const CATEGORY_LABELS: Record<string, string> = {
  CULTURE: 'Culture',
  SPORT: 'Sport',
  DIVERTISSEMENT: 'Divertissement',
};

const STATUS_LABELS: Record<EventStatus, string> = {
  DRAFT: 'Brouillon',
  PUBLISHED: 'Publié',
  CANCELLED: 'Annulé',
  FINISHED: 'Terminé',
};

const dayFormat = new Intl.DateTimeFormat('fr-FR', { day: 'numeric' });
const monthFormat = new Intl.DateTimeFormat('fr-FR', { month: 'short' });
const weekdayFormat = new Intl.DateTimeFormat('fr-FR', { weekday: 'long' });
const fullDayFormat = new Intl.DateTimeFormat('fr-FR', { day: 'numeric', month: 'long' });
const timeFormat = new Intl.DateTimeFormat('fr-FR', { hour: '2-digit', minute: '2-digit' });
const priceFormat = new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR' });

@Component({
  imports: [RouterLink, TimeAgoPipe, EventCalendar, InscriptionDialog],
  selector: 'app-event-details',
  styleUrl: './event-details.scss',
  templateUrl: './event-details.html',
})
export class EventDetails implements OnInit {
  private readonly eventDetailsService = inject(EventDetailsService);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly calendarService = inject(CalendarService);

  protected readonly user = this.authService.currentUser;
  protected readonly loading = signal(true);
  protected readonly loadError = signal<string | null>(null);
  protected readonly eventDetail = signal<EventDetailsModel | null>(null);
  protected readonly selectedImageIndex = signal(0);
  protected readonly comments = signal<CommentModel[]>([]);
  protected readonly myInscription = signal<InscriptionModel | null>(null);

  protected readonly inscriptionBusy = signal(false);
  protected readonly inscriptionError = signal<string | null>(null);
  protected readonly confirmingCancel = signal(false);
  protected readonly dialogOpen = signal(false);
  protected readonly agenda = signal<AgendaInscription[]>([]);

  protected readonly calendarMark = computed<'plus' | 'check' | 'none'>(() => {
    const kind = this.registration()?.kind;
    if (kind === 'open') {
      return 'plus';
    }
    return kind === 'registered' ? 'check' : 'none';
  });

  protected readonly calendarEvents = computed<CalendarEvent[]>(() => {
    const event = this.eventDetail();
    if (!event) {
      return [];
    }
    const others = this.agenda()
      .filter((i) => i.status !== 'CANCELED' && i.event.id !== event.id)
      .map((i) => ({
        id: String(i.event.id),
        title: i.event.title,
        start: i.event.startDateTime,
        end: i.event.endDateTime,
        muted: true,
        badge: i.status === 'WAITING_LIST' ? 'Liste d’attente' : 'Inscrit·e',
      }));
    return [
      ...others,
      { id: String(event.id), title: event.title, start: event.startDate, end: event.endDate },
    ];
  });

  protected readonly commentDraft = signal('');
  protected readonly commentBusy = signal(false);
  protected readonly commentError = signal<string | null>(null);

  protected readonly pdfBusy = signal(false);

  // Fetched through HttpClient (not a plain link) so the auth header is sent and the PDF only shows
  // the price that applies to the logged-in user, then shown in the browser's PDF viewer in a new tab.
  protected openPdf(event: EventDetailsModel): void {
    if (this.pdfBusy()) {
      return;
    }
    // The tab must be opened synchronously in the click handler, otherwise popup blockers refuse it.
    const tab = window.open('', '_blank');
    this.pdfBusy.set(true);
    this.eventDetailsService.getEventPdf(event.id).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        if (tab) {
          tab.location.href = url;
        } else {
          window.location.href = url;
        }
        setTimeout(() => URL.revokeObjectURL(url), 60_000);
        this.pdfBusy.set(false);
      },
      error: () => {
        tab?.close();
        this.pdfBusy.set(false);
      },
    });
  }

  protected readonly canEdit = computed(() => {
    const event = this.eventDetail();
    const user = this.user();
    return (
      event !== null && user !== null && (user.role === 'ADMINISTRATOR' || user.id === event.organizerId)
    );
  });

  protected readonly categoryLabel = computed(() => {
    const category = this.eventDetail()?.category ?? '';
    return CATEGORY_LABELS[category] ?? category;
  });

  protected readonly statusLabel = computed(() => {
    const status = this.eventDetail()?.status;
    return status ? STATUS_LABELS[status] : '';
  });

  protected readonly isFull = computed(() => (this.eventDetail()?.remainingSpots ?? 0) <= 0);

  protected readonly isEnded = computed(() => {
    const event = this.eventDetail();
    return event !== null && new Date(event.endDate).getTime() < Date.now();
  });

  protected readonly takenRatio = computed(() => {
    const event = this.eventDetail();
    if (!event || event.maxCapacity <= 0) {
      return 0;
    }
    const taken = event.maxCapacity - event.remainingSpots;
    return Math.min(100, Math.max(0, Math.round((taken / event.maxCapacity) * 100)));
  });

  /** Bloc date du « billet » : jour en grand, mois, jour de la semaine, horaires. */
  protected readonly dateParts = computed(() => {
    const event = this.eventDetail();
    if (!event) {
      return null;
    }
    const start = new Date(event.startDate);
    const end = new Date(event.endDate);
    const sameDay = start.toDateString() === end.toDateString();
    return {
      day: dayFormat.format(start),
      month: monthFormat.format(start).replace('.', ''),
      weekday: weekdayFormat.format(start),
      time: sameDay
        ? `${timeFormat.format(start)} – ${timeFormat.format(end)}`
        : `${timeFormat.format(start)}, jusqu’au ${fullDayFormat.format(end)} à ${timeFormat.format(end)}`,
    };
  });

  protected readonly galleryOrdered = computed(() => {
    const gallery = this.eventDetail()?.imageGallery ?? [];
    return [...gallery].sort((a, b) => a.displayOrder - b.displayOrder);
  });

  /** État du bouton principal de la carte d'inscription. */
  protected readonly registration = computed<
    | { kind: 'login' }
    | { kind: 'blocked'; reason: string }
    | { kind: 'registered'; waiting: boolean; position: number | null }
    | { kind: 'open'; waiting: boolean }
    | null
  >(() => {
    const event = this.eventDetail();
    if (!event) {
      return null;
    }
    if (this.user() === null) {
      return { kind: 'login' };
    }
    const mine = this.myInscription();
    if (mine) {
      return {
        kind: 'registered',
        waiting: mine.status === 'WAITING_LIST',
        position: mine.waitingListPosition ?? null,
      };
    }
    if (event.status === 'CANCELLED') {
      return { kind: 'blocked', reason: 'Cet évènement a été annulé.' };
    }
    if (event.status === 'DRAFT') {
      return { kind: 'blocked', reason: 'Cet évènement n’est pas encore publié.' };
    }
    if (event.status === 'FINISHED' || this.isEnded()) {
      return { kind: 'blocked', reason: 'Cet évènement est terminé.' };
    }
    return { kind: 'open', waiting: this.isFull() };
  });

  ngOnInit(): void {
    this.activatedRoute.paramMap.subscribe((params) => this.load(params.get('id')));
    this.loadAgenda();
  }

  private load(idParam: string | null): void {
    const id = idParam === null ? NaN : parseInt(idParam, 10);

    if (Number.isNaN(id)) {
      this.router.navigate(['/']);
      return;
    }

    this.loading.set(true);
    this.loadError.set(null);
    this.myInscription.set(null);
    this.selectedImageIndex.set(0);
    this.dialogOpen.set(false);

    this.eventDetailsService.getEventDetail(id).subscribe({
      next: (event) => {
        this.eventDetail.set(event);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.loadError.set(this.messageFrom(err, 'Impossible de charger cet évènement.'));
        this.loading.set(false);
      },
    });

    this.eventDetailsService.getComments(id).subscribe({
      next: (comments) => this.comments.set(comments),
      error: () => this.comments.set([]),
    });

    this.loadMyInscription(id);
  }

  protected imageUrl(path: string): string {
    return `${URL_IMAGES}${path}`;
  }

  protected formatPrice(value: number): string {
    return value === 0 ? 'Gratuit' : priceFormat.format(value);
  }

  protected initials(firstName: string, lastName: string): string {
    return `${firstName?.[0] ?? ''}${lastName?.[0] ?? ''}`.toUpperCase();
  }

  protected canDelete(comment: CommentModel): boolean {
    const user = this.user();
    return user !== null && (user.id === comment.author.id || user.role === 'ADMINISTRATOR');
  }

  // --- Galerie ---

  protected selectImage(index: number): void {
    this.selectedImageIndex.set(index);
  }

  protected previousImage(): void {
    this.shiftImage(-1);
  }

  protected nextImage(): void {
    this.shiftImage(1);
  }

  private shiftImage(step: number): void {
    const count = this.galleryOrdered().length;
    if (count === 0) {
      return;
    }
    this.selectedImageIndex.update((index) => (index + step + count) % count);
  }

  // --- Inscription ---

  protected register(): void {
    const event = this.eventDetail();
    const user = this.user();
    if (!event || !user || this.inscriptionBusy()) {
      return;
    }

    this.inscriptionBusy.set(true);
    this.inscriptionError.set(null);

    this.eventDetailsService.register(user.id, event.id).subscribe({
      next: (inscription) => {
        this.myInscription.set(inscription);
        this.inscriptionBusy.set(false);
        this.dialogOpen.set(false);
        this.refreshEvent(event.id);
        this.loadAgenda();
      },
      error: (err: HttpErrorResponse) => {
        this.inscriptionError.set(this.messageFrom(err, 'L’inscription a échoué. Réessayez dans un instant.'));
        this.inscriptionBusy.set(false);
      },
    });
  }

  protected askCancel(): void {
    this.confirmingCancel.set(true);
  }

  protected keepInscription(): void {
    this.confirmingCancel.set(false);
  }

  protected cancelInscription(): void {
    const event = this.eventDetail();
    const mine = this.myInscription();
    if (!event || !mine || this.inscriptionBusy()) {
      return;
    }

    this.inscriptionBusy.set(true);
    this.inscriptionError.set(null);

    this.eventDetailsService.cancelInscription(mine.id).subscribe({
      next: () => {
        this.myInscription.set(null);
        this.confirmingCancel.set(false);
        this.inscriptionBusy.set(false);
        this.dialogOpen.set(false);
        this.refreshEvent(event.id);
        this.loadAgenda();
      },
      error: (err: HttpErrorResponse) => {
        this.inscriptionError.set(this.messageFrom(err, 'L’annulation a échoué. Réessayez dans un instant.'));
        this.inscriptionBusy.set(false);
      },
    });
  }

  protected onCalendarSelect(eventId: string): void {
    const current = this.eventDetail();
    if (current && String(current.id) === eventId) {
      this.inscriptionError.set(null);
      this.dialogOpen.set(true);
      return;
    }
    this.router.navigate(['/event-details', eventId]);
  }

  protected closeDialog(): void {
    this.dialogOpen.set(false);
  }

  private loadAgenda(): void {
    const user = this.user();
    if (!user) {
      this.agenda.set([]);
      return;
    }
    this.calendarService.getByUser(user.id).subscribe({
      next: (list) => this.agenda.set(list),
      error: () => this.agenda.set([]),
    });
  }

  private loadMyInscription(eventId: number): void {
    const user = this.user();
    if (!user) {
      return;
    }
    this.eventDetailsService.getUserInscriptions(user.id).subscribe({
      next: (inscriptions) => {
        const mine = inscriptions.find(
          (inscription) => inscription.event?.id === eventId && inscription.status !== 'CANCELED',
        );
        this.myInscription.set(mine ?? null);
      },
      error: () => this.myInscription.set(null),
    });
  }

  private refreshEvent(eventId: number): void {
    this.eventDetailsService.getEventDetail(eventId).subscribe({
      next: (event) => this.eventDetail.set(event),
    });
  }

  // --- Commentaires ---

  protected onDraftInput(event: Event): void {
    this.commentDraft.set((event.target as HTMLTextAreaElement).value);
  }

  protected publishComment(): void {
    const event = this.eventDetail();
    const content = this.commentDraft().trim();
    if (!event || content === '' || this.commentBusy()) {
      return;
    }

    this.commentBusy.set(true);
    this.commentError.set(null);

    this.eventDetailsService.postComment(event.id, content).subscribe({
      next: (created) => {
        this.comments.update((list) => [created, ...list]);
        this.commentDraft.set('');
        this.commentBusy.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.commentError.set(this.messageFrom(err, 'Le commentaire n’a pas pu être publié.'));
        this.commentBusy.set(false);
      },
    });
  }

  protected deleteComment(comment: CommentModel): void {
    const event = this.eventDetail();
    if (!event) {
      return;
    }
    this.eventDetailsService.deleteComment(event.id, comment.id).subscribe({
      next: () => this.comments.update((list) => list.filter((c) => c.id !== comment.id)),
      error: (err: HttpErrorResponse) =>
        this.commentError.set(this.messageFrom(err, 'Le commentaire n’a pas pu être supprimé.')),
    });
  }

  private messageFrom(err: HttpErrorResponse, fallback: string): string {
    return typeof err.error === 'string' && err.error.trim() !== '' ? err.error : fallback;
  }
}
