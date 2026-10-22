import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, effect, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { CalendarService } from '../calendar/calendar-service';
import { InscriptionModel } from '../calendar/calendar.models';

@Component({
  imports: [DatePipe, RouterLink],
  selector: 'app-past-events',
  styleUrl: './past-events.scss',
  templateUrl: './past-events.html',
})
export class PastEvents {
  private readonly auth = inject(AuthService);
  private readonly calendarService = inject(CalendarService);

  protected readonly inscriptions = signal<InscriptionModel[]>([]);
  protected readonly loading = signal(true);
  protected readonly errorMessage = signal<string | null>(null);

  constructor() {
    effect(() => {
      const user = this.auth.currentUser();
      if (!user) return;
      this.calendarService.getPastByUser(user.id).subscribe({
        next: (list) => {
          this.inscriptions.set(list);
          this.loading.set(false);
        },
        error: (err: HttpErrorResponse) => {
          this.errorMessage.set(typeof err.error === 'string' ? err.error : 'Une erreur est survenue.');
          this.loading.set(false);
        },
      });
    });
  }
}
