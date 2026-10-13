import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { EventModel } from '../event/event-model';
import { EventService } from '../event/event-service';
import { scrollToTop } from '../../../shared/scroll-to-top';

const STATUS_LABELS: Record<string, string> = {
  DRAFT: 'Brouillon',
  PUBLISHED: 'Publié',
  CANCELLED: 'Annulé',
  FINISHED: 'Terminé',
};

@Component({
  imports: [DatePipe, RouterLink],
  selector: 'app-my-events',
  styleUrl: './my-events.scss',
  templateUrl: './my-events.html',
})
export class MyEvents {
  private readonly eventService = inject(EventService);

  protected readonly events = signal<EventModel[]>([]);
  protected readonly totalElements = signal(0);
  protected readonly totalPages = signal(0);
  protected readonly page = signal(0);
  protected readonly statusFilter = signal('');
  protected readonly loading = signal(true);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly pageSize = 20;

  protected readonly statuses = Object.keys(STATUS_LABELS);
  protected readonly statusLabels = STATUS_LABELS;

  constructor() {
    this.load();
  }

  protected onStatusFilter(value: string): void {
    this.statusFilter.set(value);
    this.page.set(0);
    this.load();
  }

  protected goToPage(page: number): void {
    this.page.set(page);
    this.load();
    scrollToTop();
  }

  private load(): void {
    this.loading.set(true);
    this.eventService.getMine(this.page(), this.pageSize, this.statusFilter()).subscribe({
      next: (result) => {
        this.events.set(result.content);
        this.totalElements.set(result.totalElements);
        this.totalPages.set(result.totalPages);
        this.errorMessage.set(null);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.errorMessage.set(
          typeof err.error === 'string' && err.error ? err.error : 'Une erreur est survenue.',
        );
        this.loading.set(false);
      },
    });
  }
}
