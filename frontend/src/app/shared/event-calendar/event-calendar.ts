import { DatePipe } from '@angular/common';
import { Component, computed, input, output, signal } from '@angular/core';
import { CalendarOptions, FullCalendarModule } from '@fullcalendar/angular';
import dayGridPlugin from '@fullcalendar/angular/daygrid';
import listPlugin from '@fullcalendar/angular/list';
import localesAll from '@fullcalendar/angular/locales-all';
import classicTheme from '@fullcalendar/angular/themes/classic';
import timeGridPlugin from '@fullcalendar/angular/timegrid';
import { CalendarEvent } from './event-calendar.models';

@Component({
  imports: [FullCalendarModule, DatePipe],
  selector: 'app-event-calendar',
  styleUrl: './event-calendar.scss',
  templateUrl: './event-calendar.html',
})
export class EventCalendar {
  readonly events = input.required<CalendarEvent[]>();
  // Intégré au tableau de bord : grille plus basse.
  readonly compact = input(false);
  readonly eventSelected = output<string>();

  protected readonly hovered = signal<{
    event: CalendarEvent;
    top: number;
    left: number;
  } | null>(null);

  protected readonly options = computed<CalendarOptions>(() => ({
    plugins: [classicTheme, dayGridPlugin, timeGridPlugin, listPlugin],
    locales: localesAll,
    locale: 'fr',
    firstDay: 1,
    initialView: 'dayGridMonth',
    ...(this.compact() ? { height: 400, dayMaxEvents: true } : {}),
    headerToolbar: {
      left: 'prev,next today',
      center: 'title',
      right: 'dayGridMonth,timeGridWeek,listMonth',
    },
    events: this.events().map((e) => ({
      id: e.id,
      title: e.title,
      start: e.start,
      end: e.end,
      // Couleurs explicites : fond plein + texte blanc, y compris dans la vue semaine.
      color: e.muted ? 'var(--color-muted)' : 'var(--color-primary)',
      contrastColor: '#fff',
    })),
    eventClick: (info) => this.eventSelected.emit(info.event.id),
    eventMouseEnter: (info) => {
      const event = this.events().find((e) => e.id === info.event.id);
      if (!event) return;
      const rect = info.el.getBoundingClientRect();
      this.hovered.set({
        event,
        top: rect.bottom + 6,
        left: Math.min(rect.left, window.innerWidth - 260),
      });
    },
    eventMouseLeave: () => this.hovered.set(null),
  }));
}
