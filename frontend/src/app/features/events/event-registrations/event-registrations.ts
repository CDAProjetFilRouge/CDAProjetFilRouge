import { isUuid } from '../../../core/validator/is-uuid';
import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { EventDetailsModel } from '../event-details/event-details-model';
import { EventDetailsService } from '../event-details/event-details-service';
import { EventRegistrationsService, Registration } from './event-registrations.service';

const byArrival = (a: Registration, b: Registration): number =>
  new Date(a.inscriptionDate).getTime() - new Date(b.inscriptionDate).getTime();

@Component({
  imports: [DatePipe, RouterLink],
  selector: 'app-event-registrations',
  styleUrl: './event-registrations.scss',
  templateUrl: './event-registrations.html',
})
export class EventRegistrations {
  private readonly route = inject(ActivatedRoute);
  private readonly registrationsService = inject(EventRegistrationsService);
  private readonly eventDetailsService = inject(EventDetailsService);

  protected readonly event = signal<EventDetailsModel | null>(null);
  protected readonly registrations = signal<Registration[]>([]);
  protected readonly loading = signal(true);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly confirmed = computed(() =>
    this.registrations()
      .filter((r) => r.status === 'CONFIRMED')
      .sort(byArrival),
  );

  protected readonly waiting = computed(() =>
    this.registrations()
      .filter((r) => r.status === 'WAITING_LIST')
      .sort(byArrival),
  );

  constructor() {
    const id = this.route.snapshot.paramMap.get('id');
    if (!isUuid(id)) {
      this.errorMessage.set('Évènement introuvable.');
      this.loading.set(false);
      return;
    }

    this.eventDetailsService.getEventDetail(id).subscribe({
      next: (event) => this.event.set(event),
      error: () => this.errorMessage.set('Évènement introuvable.'),
    });

    this.registrationsService.getAll(id).subscribe({
      next: (list) => {
        this.registrations.set(list);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.errorMessage.set(
          typeof err.error === 'string' && err.error
            ? err.error
            : 'Les inscriptions n’ont pas pu être chargées.',
        );
        this.loading.set(false);
      },
    });
  }
}
