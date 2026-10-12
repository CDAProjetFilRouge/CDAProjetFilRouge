import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, effect, ElementRef, inject, input, signal, viewChild } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { CalendarEvent } from '../../shared/event-calendar/event-calendar.models';
import { EventCalendar } from '../../shared/event-calendar/event-calendar';
import { CalendarService } from './calendar-service';
import { INSCRIPTION_STATUS_LABELS, InscriptionModel } from './calendar.models';

@Component({
  imports: [EventCalendar, DatePipe, RouterLink],
  selector: 'app-calendar',
  styleUrl: './calendar.scss',
  templateUrl: './calendar.html',
})
export class Calendar {
  private readonly auth = inject(AuthService);
  private readonly calendarService = inject(CalendarService);

  // Intégré au tableau de bord : pas de titre, grille plus basse.
  readonly compact = input(false);

  private readonly dialog = viewChild<ElementRef<HTMLDialogElement>>('detailDialog');

  protected readonly inscriptions = signal<InscriptionModel[]>([]);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly selected = signal<InscriptionModel | null>(null);
  protected readonly confirming = signal(false);
  protected readonly statusLabels = INSCRIPTION_STATUS_LABELS;

  protected readonly calendarEvents = computed<CalendarEvent[]>(() =>
    this.inscriptions()
      .filter((i) => i.status !== 'CANCELED')
      .map((i) => ({
        id: String(i.id),
        title: i.event.title,
        start: i.event.startDateTime,
        end: i.event.endDateTime,
        muted: i.status === 'WAITING_LIST',
        badge: i.status === 'WAITING_LIST' ? "Liste d'attente" : undefined,
      })),
  );

  protected readonly canCancel = computed(() => {
    const ins = this.selected();
    return ins !== null && new Date(ins.event.startDateTime) > new Date();
  });

  private load(userId: number): void {
    this.calendarService.getByUser(userId).subscribe({
      next: (list) => this.inscriptions.set(list),
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

    // La boîte de dialogue native suit la sélection : ouverte avec showModal() (focus piégé,
    // fond inerte, Échap pour fermer), fermée quand la sélection est vidée.
    effect(() => {
      const el = this.dialog()?.nativeElement;
      if (!el) return;
      if (this.selected()) {
        if (!el.open) el.showModal();
      } else if (el.open) {
        el.close();
      }
    });
  }

  protected select(eventId: string): void {
    this.confirming.set(false);
    this.selected.set(this.inscriptions().find((i) => i.id === Number(eventId)) ?? null);
  }

  // Appelé par l'évènement natif « close » (touche Échap, bouton de fermeture).
  protected onDialogClosed(): void {
    this.confirming.set(false);
    this.selected.set(null);
  }

  protected askCancel(): void {
    this.confirming.set(true);
  }

  protected close(): void {
    this.confirming.set(false);
    this.selected.set(null);
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
