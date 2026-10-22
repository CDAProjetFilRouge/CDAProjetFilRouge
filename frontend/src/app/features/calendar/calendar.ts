import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, effect, ElementRef, inject, signal, viewChild } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { CalendarEvent, CalendarRange } from '../../shared/event-calendar/event-calendar.models';
import { EventCalendar } from '../../shared/event-calendar/event-calendar';
import { CalendarService } from './calendar-service';
import {
  fillLabel,
  INSCRIPTION_STATUS_LABELS,
  InscriptionModel,
  ORGANIZED_ID_PREFIX,
  ORGANIZED_STATUS_LABELS,
  OrganizedEventModel,
  toCalendarEvent,
  toOrganizedCalendarEvent,
} from './calendar.models';

@Component({
  imports: [EventCalendar, DatePipe, RouterLink],
  selector: 'app-calendar',
  styleUrl: './calendar.scss',
  templateUrl: './calendar.html',
})
export class Calendar {
  private readonly auth = inject(AuthService);
  private readonly calendarService = inject(CalendarService);

  private organizedRequest?: Subscription;
  private readonly dialog = viewChild<ElementRef<HTMLDialogElement>>('detailDialog');

  protected readonly inscriptions = signal<InscriptionModel[]>([]);
  protected readonly organized = signal<OrganizedEventModel[]>([]);
  protected readonly range = signal<CalendarRange | null>(null);
  protected readonly showInscriptions = signal(true);
  protected readonly showOrganized = signal(true);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly selected = signal<InscriptionModel | null>(null);
  protected readonly selectedOrganized = signal<OrganizedEventModel | null>(null);
  protected readonly confirming = signal(false);
  protected readonly statusLabels = INSCRIPTION_STATUS_LABELS;
  protected readonly organizedStatusLabels = ORGANIZED_STATUS_LABELS;
  protected readonly fillLabel = fillLabel;

  protected readonly isOrganizer = computed(() => this.auth.currentUser()?.role === 'ORGANIZER');

  private readonly activeInscriptions = computed(() => this.inscriptions().filter((i) => i.status !== 'CANCELED'));

  // Un organisateur inscrit à son propre évènement le voit une seule fois, avec les deux badges.
  protected readonly calendarEvents = computed<CalendarEvent[]>(() => {
    const inscriptions = this.activeInscriptions();
    const organized = this.isOrganizer() && this.showOrganized() ? this.organized() : [];
    const organizedIds = new Set(organized.map((e) => e.id));

    const organizedEvents = organized.map((event) =>
      toOrganizedCalendarEvent(
        event,
        inscriptions.find((i) => i.event.id === event.id),
      ),
    );
    const inscriptionEvents = this.showInscriptions()
      ? inscriptions
          .filter((i) => !organizedIds.has(i.event.id))
          .map((i) => toCalendarEvent(i, { id: String(i.id) }))
      : [];
    return [...inscriptionEvents, ...organizedEvents];
  });

  protected readonly ownInscription = computed(() => {
    const event = this.selectedOrganized();
    return event ? (this.activeInscriptions().find((i) => i.event.id === event.id) ?? null) : null;
  });

  protected readonly canCancel = computed(() => {
    const ins = this.selected();
    return ins !== null && new Date(ins.event.startDateTime) > new Date();
  });

  private load(userId: string): void {
    this.calendarService.getByUser(userId).subscribe({
      next: (list) => this.inscriptions.set(list),
      error: (err: HttpErrorResponse) => this.handleError(err),
    });
  }

  // Un changement de mois rapide annule la requête précédente : une réponse tardive n'écrase pas la plus récente.
  private loadOrganized(range: CalendarRange): void {
    this.organizedRequest?.unsubscribe();
    this.organizedRequest = this.calendarService.getOrganized(range.from, range.to).subscribe({
      next: (list) => this.organized.set(list),
      error: (err: HttpErrorResponse) => this.handleError(err),
    });
  }

  private handleError(err: HttpErrorResponse): void {
    this.errorMessage.set(typeof err.error === 'string' ? err.error : 'Une erreur est survenue');
  }

  constructor() {
    effect(() => {
      const user = this.auth.currentUser();
      if (user) {
        this.load(user.id);
      }
    });

    effect(() => {
      const range = this.range();
      if (range && this.isOrganizer()) {
        this.loadOrganized(range);
      }
    });

    // La boîte de dialogue native suit la sélection : ouverte avec showModal() (focus piégé,
    // fond inerte, Échap pour fermer), fermée quand la sélection est vidée.
    effect(() => {
      const el = this.dialog()?.nativeElement;
      if (!el) return;
      if (this.selected() || this.selectedOrganized()) {
        if (!el.open) el.showModal();
      } else if (el.open) {
        el.close();
      }
    });
  }

  protected setRange(range: CalendarRange): void {
    this.range.set(range);
  }

  protected select(eventId: string): void {
    this.confirming.set(false);
    if (eventId.startsWith(ORGANIZED_ID_PREFIX)) {
      const id = eventId.slice(ORGANIZED_ID_PREFIX.length);
      this.selected.set(null);
      this.selectedOrganized.set(this.organized().find((e) => e.id === id) ?? null);
      return;
    }
    this.selectedOrganized.set(null);
    this.selected.set(this.inscriptions().find((i) => i.id === eventId) ?? null);
  }

  // Appelé par l'évènement natif « close » (touche Échap, bouton de fermeture).
  protected onDialogClosed(): void {
    this.close();
  }

  protected askCancel(): void {
    this.confirming.set(true);
  }

  protected close(): void {
    this.confirming.set(false);
    this.selected.set(null);
    this.selectedOrganized.set(null);
  }

  protected cancel(): void {
    const ins = this.selected();
    const user = this.auth.currentUser();
    if (!ins || !user) return;

    this.calendarService.cancel(ins.id).subscribe({
      next: () => {
        this.close();
        this.load(user.id);
      },
      error: (err: HttpErrorResponse) => {
        this.confirming.set(false);
        this.handleError(err);
      },
    });
  }
}
