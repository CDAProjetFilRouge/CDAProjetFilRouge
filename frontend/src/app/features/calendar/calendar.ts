import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, effect, inject, signal } from '@angular/core';
import { CalendarOptions, FullCalendarModule } from '@fullcalendar/angular';
import dayGridPlugin from '@fullcalendar/angular/daygrid';
import listPlugin from '@fullcalendar/angular/list';
import localesAll from '@fullcalendar/angular/locales-all';
import classicTheme from '@fullcalendar/angular/themes/classic';
import timeGridPlugin from '@fullcalendar/angular/timegrid';
import { AuthService } from '../../core/auth/auth.service';
import { CalendarService } from './calendar-service';
import { INSCRIPTION_STATUS_LABELS, InscriptionModel } from './calendar.models';

@Component({
  imports: [FullCalendarModule, DatePipe],
  selector: 'app-calendar',
  styleUrl: './calendar.scss',
  templateUrl: './calendar.html',
})
export class Calendar {
  private readonly auth = inject(AuthService);
  private readonly calendarService = inject(CalendarService);

  protected readonly inscriptions = signal<InscriptionModel[]>([]);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly selected = signal<InscriptionModel | null>(null);
  protected readonly statusLabels = INSCRIPTION_STATUS_LABELS;

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
  }

  protected readonly options = computed<CalendarOptions>(() => ({
    plugins: [classicTheme, dayGridPlugin, timeGridPlugin, listPlugin],
    locales: localesAll,
    locale: 'fr',
    firstDay: 1,
    initialView: 'dayGridMonth',
    headerToolbar: {
      left: 'prev,next today',
      center: 'title',
      right: 'dayGridMonth,timeGridWeek,listMonth',
    },
    events: this.inscriptions()
      .filter((i) => i.status !== 'CANCELED')
      .map((i) => ({
        id: String(i.id),
        title: i.event.title,
        start: i.event.startDateTime,
        end: i.event.endDateTime,
        classNames: i.status === 'WAITING_LIST' ? ['waiting'] : [],
      })),
    eventClick: (info) => {
      const found = this.inscriptions().find((i) => i.id === Number(info.event.id));
      this.selected.set(found ?? null);
    },
  }));

  protected cancel(): void {
    const ins = this.selected();
    const user = this.auth.currentUser();
    if (!ins || !user) return;

    this.calendarService.cancel(ins.id).subscribe({
      next: () => {
        this.selected.set(null);
        this.load(user.id);
      },
      error: (err: HttpErrorResponse) => this.handleError(err),
    });
  }
}
