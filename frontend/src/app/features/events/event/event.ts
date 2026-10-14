import { Component, computed, effect, inject, signal } from '@angular/core';
import { EventFilter, EventService } from './event-service';
import { EventModel } from './event-model';
import { HttpErrorResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { DatePicker } from '../../../shared/date-picker/date-picker';
import { AuthService } from '../../../core/auth/auth.service';
import { CalendarService } from '../../calendar/calendar-service';
import { CardRegistration, EventCard } from '../../../shared/event-card/event-card';
import { ActivatedRoute } from '@angular/router';
import { EventDetailsService } from '../event-details/event-details-service';
import {
  InscriptionDialog,
  InscriptionDialogState,
} from '../event-details/inscription-dialog/inscription-dialog';
import { scrollToTop } from '../../../shared/scroll-to-top';

const QUICK_ERROR_MS = 4000;

// Statut par défaut du catalogue : les évènements annulés restent visibles, avec leur indicateur.
const CATALOGUE_STATUSES = 'PUBLISHED,CANCELLED';

interface MyRegistration {
  id: string;
  status: Exclude<CardRegistration, null>;
  position: number | null;
}

@Component({
  imports: [ReactiveFormsModule, DatePicker, EventCard, InscriptionDialog],
  selector: 'app-event',
  styleUrl: './event.scss',
  templateUrl: './event.html',
})
export class Event {
  private readonly eventService = inject(EventService);
  private readonly route = inject(ActivatedRoute);
  private readonly authService = inject(AuthService);
  private readonly calendarService = inject(CalendarService);
  private readonly eventDetailsService = inject(EventDetailsService);

  private readonly user = this.authService.currentUser;
  private readonly registrations = signal<Map<string, MyRegistration>>(new Map());
  protected readonly affiliated = computed(() => (this.user()?.clubs.length ?? 0) > 0);

  protected readonly signedIn = computed(() => this.user() !== null);
  private readonly quickErrors = signal<Map<string, string>>(new Map());

  protected readonly dialogEvent = signal<EventModel | null>(null);
  protected readonly dialogBusy = signal(false);
  protected readonly dialogError = signal<string | null>(null);

  protected readonly dialogState = computed<InscriptionDialogState | null>(() => {
    const event = this.dialogEvent();
    if (!event) {
      return null;
    }
    if (!this.user()) {
      return { kind: 'login' };
    }
    const mine = this.registrations().get(event.id);
    if (mine) {
      return { kind: 'registered', waiting: mine.status === 'WAITING_LIST', position: mine.position };
    }
    return { kind: 'open', waiting: event.remainingSpots <= 0 };
  });

  constructor() {
    effect(() => {
      if (this.user()) {
        this.loadRegistrations();
      } else {
        this.registrations.set(new Map());
      }
    });
  }

  protected registrationOf(eventId: string): CardRegistration {
    return this.registrations().get(eventId)?.status ?? null;
  }

  protected positionOf(eventId: string): number | null {
    return this.registrations().get(eventId)?.position ?? null;
  }

  protected quickErrorOf(eventId: string): string | null {
    return this.quickErrors().get(eventId) ?? null;
  }

  // Inscription directe depuis la souche du billet : pas de fenêtre, le billet montre lui-même le résultat.
  protected quickRegister(event: EventModel): void {
    const user = this.user();
    if (!user) {
      return;
    }
    this.eventDetailsService.register(user.id, event.id).subscribe({
      next: () => {
        this.loadRegistrations();
        this.loadEvents();
      },
      error: (err: HttpErrorResponse) =>
        this.showQuickError(
          event.id,
          typeof err.error === 'string' && err.error ? err.error : 'Réessayez dans un instant.',
        ),
    });
  }

  private showQuickError(eventId: string, message: string): void {
    this.quickErrors.update((map) => new Map(map).set(eventId, message));
    setTimeout(
      () =>
        this.quickErrors.update((map) => {
          const next = new Map(map);
          next.delete(eventId);
          return next;
        }),
      QUICK_ERROR_MS,
    );
  }

  protected openRegistration(event: EventModel): void {
    this.dialogError.set(null);
    this.dialogEvent.set(event);
  }

  protected closeRegistration(): void {
    this.dialogEvent.set(null);
  }

  protected register(): void {
    const event = this.dialogEvent();
    const user = this.user();
    if (!event || !user || this.dialogBusy()) {
      return;
    }
    this.runInscription(
      this.eventDetailsService.register(user.id, event.id),
      'L’inscription a échoué. Réessayez dans un instant.',
    );
  }

  protected unregister(): void {
    const event = this.dialogEvent();
    const mine = event ? this.registrations().get(event.id) : undefined;
    if (!mine || this.dialogBusy()) {
      return;
    }
    this.runInscription(
      this.eventDetailsService.cancelInscription(mine.id),
      'L’annulation a échoué. Réessayez dans un instant.',
    );
  }

  private runInscription(call: Observable<unknown>, fallback: string): void {
    this.dialogBusy.set(true);
    this.dialogError.set(null);
    call.subscribe({
      next: () => {
        this.dialogBusy.set(false);
        this.dialogEvent.set(null);
        this.loadRegistrations();
        this.loadEvents();
      },
      error: (err: HttpErrorResponse) => {
        this.dialogBusy.set(false);
        this.dialogError.set(typeof err.error === 'string' && err.error ? err.error : fallback);
      },
    });
  }

  private loadRegistrations(): void {
    const user = this.user();
    if (!user) {
      return;
    }
    this.calendarService.getByUser(user.id).subscribe({
      next: (list) => {
        const map = new Map<string, MyRegistration>();
        for (const inscription of list) {
          if (inscription.status !== 'CANCELED') {
            map.set(inscription.event.id, {
              id: inscription.id,
              status: inscription.status,
              position: inscription.waitingListPosition ?? null,
            });
          }
        }
        this.registrations.set(map);
      },
      error: () => this.registrations.set(new Map()),
    });
  }

  protected readonly events = signal<EventModel[]>([]);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly sortOrder = signal<'asc' | 'desc'>('asc');

  currentPage = 1;
  eventsPerPage = 20;
  totalElements = 0;
  totalPages = 0

  filters = new FormGroup({
    keyword: new FormControl('', { nonNullable: true}),
    category: new FormControl('', { nonNullable: true}),
    club: new FormControl('', {nonNullable: true}),
    city: new FormControl('', {nonNullable: true}),
    startDate: new FormControl('', {nonNullable: true}),
    endDate: new FormControl('', {nonNullable: true}),
    status: new FormControl(CATALOGUE_STATUSES, {nonNullable: true})
  })

  private handleError(err: HttpErrorResponse): void {
    this.errorMessage.set(typeof err.error === 'string' ? err.error : 'Une erreur est survenue.');
  }

  ngOnInit(): void {
    // Pré-filtre possible par lien : /?status=CANCELLED. Les brouillons n'ont pas leur place dans le catalogue :
    // ils se trouvent dans « Mes évènements ».
    const status = this.route.snapshot.queryParamMap.get('status');
    if (status && ['PUBLISHED', 'CANCELLED'].includes(status)) {
      this.filters.patchValue({ status });
    }
    this.loadEvents();
  }

  loadEvents(): void {
    const filters: EventFilter = { ...this.filters.getRawValue(), hideFinished: true, sort: this.sortOrder() };

    this.eventService.search(this.currentPage - 1, this.eventsPerPage, filters).subscribe({
      next: (page) => {
        this.events.set(page.content);
        this.totalElements = page.totalElements;
        this.totalPages = page.totalPages;
      },
      error: (err) => this.handleError(err)
    });
  }

  search(): void {
    this.currentPage = 1;
    this.loadEvents();
  }

  resetFilter(): void {
    this.filters.reset({
      keyword: '',
      category: '',
      club: '',
      city: '',
      startDate: '',
      endDate: '',
      status: CATALOGUE_STATUSES
    });

    this.search();
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages) {
      this.currentPage++;
      this.loadEvents();
      scrollToTop();
    }
  }

  previousPage(): void {
    if (this.currentPage > 1) {
      this.currentPage--;
      this.loadEvents();
      scrollToTop();
    }
  }

  toggleSort(): void {
    this.sortOrder.update(order => order === 'asc' ? 'desc' : 'asc');
    this.currentPage = 1;
    this.loadEvents();
  }
}
