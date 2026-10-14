import { vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CalendarOptions } from '@fullcalendar/angular';
import { CalendarEvent } from './event-calendar.models';
import { EventCalendar } from './event-calendar';

type EventClickArg = Parameters<NonNullable<CalendarOptions['eventClick']>>[0];
type EventHoveringArg = Parameters<NonNullable<CalendarOptions['eventMouseEnter']>>[0];

function monthStart(): Date {
  const date = new Date();
  return new Date(date.getFullYear(), date.getMonth(), 10, 18, 0, 0);
}

function iso(date: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:00`;
}

const START = monthStart();
const END = new Date(START.getTime() + 2 * 60 * 60 * 1000);

const EVENTS: CalendarEvent[] = [
  { id: '1', title: 'Tournoi de tennis', start: iso(START), end: iso(END) },
  { id: '2', title: 'Soirée jeux', start: iso(START), end: iso(END), muted: true, badge: 'Liste d’attente' },
];

describe('EventCalendar', () => {
  let fixture: ComponentFixture<EventCalendar>;
  let component: EventCalendar;

  const options = () => (component as unknown as { options(): CalendarOptions }).options();
  const hovered = () => (component as unknown as { hovered(): { event: CalendarEvent } | null }).hovered();

  async function render(events: CalendarEvent[] = EVENTS, inputs: Record<string, unknown> = {}) {
    fixture = TestBed.createComponent(EventCalendar);
    fixture.componentRef.setInput('events', events);
    Object.entries(inputs).forEach(([name, value]) => fixture.componentRef.setInput(name, value));
    component = fixture.componentInstance;
    await fixture.whenStable();
    fixture.detectChanges();
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [EventCalendar] }).compileComponents();
  });

  it('affiche les évènements dans la grille du mois', async () => {
    await render();
    await new Promise((resolve) => setTimeout(resolve, 100));
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Tournoi de tennis');
    expect(text).toContain('Soirée jeux');
  });

  it('estompe les évènements « muted » et garde la couleur principale aux autres', async () => {
    await render();

    const colors = (options().events as { id: string; color: string }[]).map((e) => [e.id, e.color]);
    expect(colors).toEqual([
      ['1', 'var(--color-primary)'],
      ['2', 'var(--color-muted)'],
    ]);
  });

  it('émet l’identifiant de l’évènement cliqué', async () => {
    await render();
    const selected: string[] = [];
    component.eventSelected.subscribe((id) => selected.push(id));

    options().eventClick?.({ event: { id: '2' } } as EventClickArg);

    expect(selected).toEqual(['2']);
  });

  it('montre la bulle au survol avec le titre, les dates et le badge', async () => {
    await render();
    const el = document.createElement('div');
    vi.spyOn(el, 'getBoundingClientRect').mockReturnValue({ bottom: 100, left: 40 } as DOMRect);

    options().eventMouseEnter?.({ event: { id: '2' }, el } as unknown as EventHoveringArg);
    fixture.detectChanges();

    const bubble = fixture.nativeElement.querySelector('.bubble') as HTMLElement;
    expect(bubble).not.toBeNull();
    expect(bubble.textContent).toContain('Soirée jeux');
    expect(bubble.textContent).toContain('Liste d’attente');
    expect(bubble.textContent).toContain('→');
    expect(bubble.style.top).toBe('106px');
    expect(bubble.style.left).toBe('40px');
  });

  it('garde la bulle dans la fenêtre quand l’évènement est au bord droit', async () => {
    await render();
    const el = document.createElement('div');
    vi.spyOn(el, 'getBoundingClientRect').mockReturnValue({ bottom: 0, left: window.innerWidth + 500 } as DOMRect);

    options().eventMouseEnter?.({ event: { id: '1' }, el } as unknown as EventHoveringArg);

    expect(hovered()).not.toBeNull();
    expect((component as unknown as { hovered(): { left: number } }).hovered().left).toBe(window.innerWidth - 260);
  });

  it('retire la bulle quand la souris quitte l’évènement', async () => {
    await render();
    const el = document.createElement('div');
    vi.spyOn(el, 'getBoundingClientRect').mockReturnValue({ bottom: 0, left: 0 } as DOMRect);
    options().eventMouseEnter?.({ event: { id: '1' }, el } as unknown as EventHoveringArg);
    expect(hovered()).not.toBeNull();

    options().eventMouseLeave?.({} as EventHoveringArg);

    expect(hovered()).toBeNull();
  });

  it('ignore le survol d’un évènement inconnu', async () => {
    await render();
    const el = document.createElement('div');

    options().eventMouseEnter?.({ event: { id: 'inconnu' }, el } as unknown as EventHoveringArg);

    expect(hovered()).toBeNull();
  });

  it('affiche la barre de navigation intégrée en mode normal, la sienne en mode large', async () => {
    await render();
    expect(options().headerToolbar).toMatchObject({ left: 'prev,next today' });
    expect(fixture.nativeElement.querySelector('.toolbar')).toBeNull();

    fixture.destroy();
    await render(EVENTS, { large: true });
    expect(options().headerToolbar).toBe(false);
    expect(fixture.nativeElement.querySelector('.toolbar')).not.toBeNull();
  });

  it('met en avant l’évènement choisi avec la marque demandée (mode large)', async () => {
    await render(EVENTS, { large: true, highlightedId: '1', highlightedMark: 'check' });
    const eventClass = options().eventClass as (info: unknown) => string;

    expect(eventClass({ event: { id: '1' }, isStart: true })).toBe('agenda-event is-highlighted mark-check');
    expect(eventClass({ event: { id: '1' }, isStart: false })).toBe('agenda-event is-highlighted');
    expect(eventClass({ event: { id: '2' }, isStart: true })).toBe('agenda-event');
  });

  it('ouvre le calendrier sur la date demandée (mode large)', async () => {
    await render(EVENTS, { large: true, initialDate: '2031-04-15' });

    expect(options().initialDate).toBe('2031-04-15');
  });

  it('abaisse la grille en mode compact', async () => {
    await render(EVENTS, { compact: true });

    expect(options().height).toBe(400);
  });
});
