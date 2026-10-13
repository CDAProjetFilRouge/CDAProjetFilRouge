import { ChangeDetectionStrategy, Component, computed, ElementRef, forwardRef, inject, input, signal } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

interface ParsedValue {
  year: number;
  month: number; // 0-11
  day: number;
  hour: number;
  minute: number;
}

interface CalendarCell {
  iso: string;
  day: number;
  outside: boolean;
  today: boolean;
  selected: boolean;
}

const POPUP_WIDTH = 288;
const POPUP_HEIGHT = 400;
const POPUP_GAP = 4;
const VIEWPORT_MARGIN = 8;

const pad = (n: number): string => String(n).padStart(2, '0');
const toIsoDate = (year: number, month: number, day: number): string =>
  `${year}-${pad(month + 1)}-${pad(day)}`;

/**
 * Sélecteur de date (`date`) ou de date et heure (`datetime`) qui remplace la fenêtre native
 * des `<input type="date">` / `<input type="datetime-local">`.
 *
 * La valeur échangée avec le formulaire a le même format que celle du champ natif
 * (`YYYY-MM-DD` ou `YYYY-MM-DDTHH:mm`), donc les validateurs et les appels API restent inchangés.
 */
@Component({
  selector: 'app-date-picker',
  templateUrl: './date-picker.html',
  styleUrl: './date-picker.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    { provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => DatePicker), multi: true },
  ],
  host: {
    '(document:mousedown)': 'onDocumentMouseDown($event)',
    '(window:resize)': 'close()',
    '(window:scroll)': 'close()',
    '(keydown.escape)': 'onEscape($event)',
  },
})
export class DatePicker implements ControlValueAccessor {
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement;

  readonly mode = input<'date' | 'datetime'>('date');
  readonly inputId = input('');
  readonly placeholder = input('');

  protected readonly weekdays = ['L', 'M', 'M', 'J', 'V', 'S', 'D'];
  protected readonly hours = Array.from({ length: 24 }, (_, i) => i);
  protected readonly minutes = Array.from({ length: 60 }, (_, i) => i);
  protected readonly pad = pad;

  protected readonly value = signal('');
  protected readonly disabled = signal(false);
  protected readonly open = signal(false);
  protected readonly viewYear = signal(new Date().getFullYear());
  protected readonly viewMonth = signal(new Date().getMonth());
  protected readonly hour = signal(0);
  protected readonly minute = signal(0);
  protected readonly popupStyle = signal<Record<string, string>>({});

  protected readonly isDateTime = computed(() => this.mode() === 'datetime');

  protected readonly placeholderText = computed(
    () => this.placeholder() || (this.isDateTime() ? 'jj/mm/aaaa --:--' : 'jj/mm/aaaa'),
  );

  protected readonly display = computed(() => {
    const parsed = this.parse(this.value());
    if (!parsed) {
      return '';
    }
    const date = `${pad(parsed.day)}/${pad(parsed.month + 1)}/${parsed.year}`;
    return this.isDateTime() ? `${date} ${pad(parsed.hour)}:${pad(parsed.minute)}` : date;
  });

  protected readonly monthLabel = computed(() =>
    new Intl.DateTimeFormat('fr-FR', { month: 'long', year: 'numeric' }).format(
      new Date(this.viewYear(), this.viewMonth(), 1),
    ),
  );

  protected readonly cells = computed<CalendarCell[]>(() => {
    const year = this.viewYear();
    const month = this.viewMonth();
    const selected = this.parse(this.value());
    const selectedIso = selected ? toIsoDate(selected.year, selected.month, selected.day) : '';
    const now = new Date();
    const todayIso = toIsoDate(now.getFullYear(), now.getMonth(), now.getDate());

    // La semaine commence le lundi : on recule jusqu'au lundi qui précède le 1er du mois.
    const offset = (new Date(year, month, 1).getDay() + 6) % 7;
    return Array.from({ length: 42 }, (_, i) => {
      const date = new Date(year, month, 1 - offset + i);
      const iso = toIsoDate(date.getFullYear(), date.getMonth(), date.getDate());
      return {
        iso,
        day: date.getDate(),
        outside: date.getMonth() !== month,
        today: iso === todayIso,
        selected: iso === selectedIso,
      };
    });
  });

  private onChange: (value: string) => void = () => {};
  private onTouched: () => void = () => {};

  writeValue(value: string | null): void {
    const normalized = value ?? '';
    this.value.set(normalized);
    const parsed = this.parse(normalized);
    if (parsed) {
      this.viewYear.set(parsed.year);
      this.viewMonth.set(parsed.month);
      this.hour.set(parsed.hour);
      this.minute.set(parsed.minute);
    }
  }

  registerOnChange(fn: (value: string) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled.set(isDisabled);
    if (isDisabled) {
      this.open.set(false);
    }
  }

  protected toggle(): void {
    if (this.open()) {
      this.close();
      return;
    }
    const parsed = this.parse(this.value());
    const reference = parsed ?? this.today();
    this.viewYear.set(reference.year);
    this.viewMonth.set(reference.month);
    this.placePopup();
    this.open.set(true);
  }

  protected close(): void {
    if (this.open()) {
      this.open.set(false);
      this.onTouched();
    }
  }

  protected onEscape(event: Event): void {
    if (this.open()) {
      // Évite que Échap ferme aussi une <dialog> parente.
      event.stopPropagation();
      event.preventDefault();
      this.close();
      this.host.querySelector<HTMLElement>('.trigger')?.focus();
    }
  }

  protected onDocumentMouseDown(event: MouseEvent): void {
    if (this.open() && !this.host.contains(event.target as Node)) {
      this.close();
    }
  }

  protected shiftMonth(delta: number): void {
    const date = new Date(this.viewYear(), this.viewMonth() + delta, 1);
    this.viewYear.set(date.getFullYear());
    this.viewMonth.set(date.getMonth());
  }

  protected shiftYear(delta: number): void {
    this.viewYear.update((year) => year + delta);
  }

  protected selectDay(cell: CalendarCell): void {
    const [year, month, day] = cell.iso.split('-').map(Number);
    this.commit(year, month - 1, day);
    this.viewYear.set(year);
    this.viewMonth.set(month - 1);
    if (!this.isDateTime()) {
      this.close();
      this.host.querySelector<HTMLElement>('.trigger')?.focus();
    }
  }

  protected selectToday(): void {
    const today = this.today();
    this.commit(today.year, today.month, today.day);
    this.viewYear.set(today.year);
    this.viewMonth.set(today.month);
    if (!this.isDateTime()) {
      this.close();
    }
  }

  protected clear(): void {
    this.value.set('');
    this.onChange('');
    this.close();
  }

  protected setHour(event: Event): void {
    this.hour.set(Number((event.target as HTMLSelectElement).value));
    this.recommit();
  }

  protected setMinute(event: Event): void {
    this.minute.set(Number((event.target as HTMLSelectElement).value));
    this.recommit();
  }

  private commit(year: number, month: number, day: number): void {
    const date = toIsoDate(year, month, day);
    const next = this.isDateTime() ? `${date}T${pad(this.hour())}:${pad(this.minute())}` : date;
    this.value.set(next);
    this.onChange(next);
  }

  // Un changement d'heure ne fait rien tant qu'aucun jour n'est choisi.
  private recommit(): void {
    const parsed = this.parse(this.value());
    if (parsed) {
      this.commit(parsed.year, parsed.month, parsed.day);
    }
  }

  private today(): ParsedValue {
    const now = new Date();
    return {
      year: now.getFullYear(),
      month: now.getMonth(),
      day: now.getDate(),
      hour: 0,
      minute: 0,
    };
  }

  private parse(value: string): ParsedValue | null {
    const match = /^(\d{4})-(\d{2})-(\d{2})(?:T(\d{2}):(\d{2}))?/.exec(value);
    if (!match) {
      return null;
    }
    return {
      year: Number(match[1]),
      month: Number(match[2]) - 1,
      day: Number(match[3]),
      hour: Number(match[4] ?? 0),
      minute: Number(match[5] ?? 0),
    };
  }

  // La fenêtre est en `position: fixed` pour ne pas être rognée par un conteneur
  // (ex. la <dialog> de suspension), donc on calcule sa position à l'ouverture.
  private placePopup(): void {
    const trigger = this.host.querySelector<HTMLElement>('.trigger');
    if (!trigger) {
      return;
    }
    const rect = trigger.getBoundingClientRect();
    const left = Math.max(
      VIEWPORT_MARGIN,
      Math.min(rect.left, window.innerWidth - POPUP_WIDTH - VIEWPORT_MARGIN),
    );
    const fitsBelow = rect.bottom + POPUP_GAP + POPUP_HEIGHT <= window.innerHeight;
    if (fitsBelow || rect.top < POPUP_HEIGHT) {
      this.popupStyle.set({ left: `${left}px`, top: `${rect.bottom + POPUP_GAP}px` });
    } else {
      this.popupStyle.set({
        left: `${left}px`,
        bottom: `${window.innerHeight - rect.top + POPUP_GAP}px`,
      });
    }
  }
}
