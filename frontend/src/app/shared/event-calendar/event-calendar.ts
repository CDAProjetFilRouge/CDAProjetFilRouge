import { DatePipe } from '@angular/common';
import { Component, computed, input, output, signal, viewChild } from '@angular/core';
import { CalendarOptions, FullCalendarComponent, FullCalendarModule } from '@fullcalendar/angular';
import dayGridPlugin from '@fullcalendar/angular/daygrid';
import listPlugin from '@fullcalendar/angular/list';
import localesAll from '@fullcalendar/angular/locales-all';
import classicTheme from '@fullcalendar/angular/themes/classic';
import timeGridPlugin from '@fullcalendar/angular/timegrid';
import { CalendarEvent, CalendarRange } from './event-calendar.models';

// Date locale sans fuseau (AAAA-MM-JJTHH:MM:SS), le format attendu par le back pour un LocalDateTime.
function toLocalIso(date: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}

@Component({
  imports: [FullCalendarModule, DatePipe],
  host: { '[class.large]': 'large()', '[class.compact]': 'compact()' },
  selector: 'app-event-calendar',
  styleUrl: './event-calendar.scss',
  templateUrl: './event-calendar.html',
})
export class EventCalendar {
  readonly events = input.required<CalendarEvent[]>();
  // Intégré au tableau de bord : la grille remplit la hauteur de son conteneur.
  readonly compact = input(false);
  readonly large = input(false);
  readonly initialDate = input<string | undefined>(undefined);
  readonly highlightedId = input<string | undefined>(undefined);
  readonly highlightedMark = input<'plus' | 'check' | 'none'>('none');
  readonly eventSelected = output<string>();
  // Plage réellement affichée (jours des mois voisins compris), à chaque changement de mois ou de vue.
  readonly rangeChanged = output<CalendarRange>();

  private readonly calendar = viewChild(FullCalendarComponent);
  protected readonly title = signal('');

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
    ...(this.compact() ? { height: '100%', dayMaxEvents: true } : {}),
    ...(this.large()
      ? {
          height: 'auto',
          dayMaxEvents: false,
          eventDisplay: 'block',
          displayEventTime: false,
          initialDate: this.initialDate(),
          dayCellClass: (info) =>
            [
              'agenda-cell',
              info.isOther ? 'is-other' : '',
              info.isToday ? 'is-today' : '',
              info.dow === 0 || info.dow === 6 ? 'is-weekend' : '',
            ]
              .filter(Boolean)
              .join(' '),
          dayCellTopInnerClass: (info) =>
            info.isToday ? 'agenda-day-number is-today' : 'agenda-day-number',
          dayHeaderClass: 'agenda-head',
          eventClass: (info) =>
            info.event.id === this.highlightedId()
              ? `agenda-event is-highlighted${info.isStart ? ` mark-${this.highlightedMark()}` : ''}`
              : 'agenda-event',
          eventInnerClass: 'agenda-event-inner',
          eventTitleClass: 'agenda-event-title',
        }
      : {}),
    headerToolbar: this.large()
      ? false
      : {
          left: 'prev,next today',
          center: 'title',
          right: 'dayGridMonth,timeGridWeek,listMonth',
        },
    datesSet: (info) => {
      this.title.set(info.view.title);
      this.rangeChanged.emit({ from: toLocalIso(info.view.activeStart), to: toLocalIso(info.view.activeEnd) });
    },
    events: this.events().map((e) => ({
      id: e.id,
      title: e.title,
      start: e.start,
      end: e.end,
      // Couleurs explicites : fond plein + texte blanc, y compris dans la vue semaine.
      color: e.organized ? 'var(--color-pine-soft)' : e.muted ? 'var(--color-muted)' : 'var(--color-primary)',
      contrastColor: e.organized ? 'var(--color-pine)' : '#fff',
    })),
    ...(this.large()
      ? {}
      : {
          eventClass: (info: { event: { id: string } }) => {
            const event = this.events().find((e) => e.id === info.event.id);
            return event?.organized ? `organized-event status-${event.status?.toLowerCase()}` : '';
          },
        }),
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

  protected previous(): void {
    this.calendar()?.getApi().prev();
  }

  protected next(): void {
    this.calendar()?.getApi().next();
  }

  protected today(): void {
    this.calendar()?.getApi().today();
  }
}
