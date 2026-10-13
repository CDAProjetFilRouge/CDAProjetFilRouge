import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { EventModel } from '../../../events/event/event-model';
import { EventService } from '../../../events/event/event-service';
import { scrollToTop } from '../../../../shared/scroll-to-top';

const STATUS_LABELS: Record<string, string> = {
  DRAFT: 'Brouillon',
  PUBLISHED: 'Publié',
  CANCELLED: 'Annulé',
  FINISHED: 'Terminé',
};

@Component({
  imports: [DatePipe, RouterLink],
  selector: 'app-admin-event-list',
  styleUrl: './event-list.scss',
  templateUrl: './event-list.html',
})
export class EventList {
  private readonly eventService = inject(EventService);

  protected readonly events = signal<EventModel[]>([]);
  protected readonly totalElements = signal(0);
  protected readonly totalPages = signal(0);
  protected readonly page = signal(0);
  protected readonly search = signal('');
  protected readonly statusFilter = signal('');
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly pageSize = 20;

  protected readonly statuses = Object.keys(STATUS_LABELS);
  protected readonly statusLabels = STATUS_LABELS;

  constructor() {
    this.load();
  }

  private load(): void {
    this.eventService
      .search(this.page(), this.pageSize, {
        keyword: this.search(),
        category: '',
        club: '',
        city: '',
        startDate: '',
        endDate: '',
        status: this.statusFilter(),
      })
      .subscribe({
        next: (result) => {
          this.events.set(result.content);
          this.totalElements.set(result.totalElements);
          this.totalPages.set(result.totalPages);
          this.errorMessage.set(null);
        },
        error: (err: HttpErrorResponse) =>
          this.errorMessage.set(
            typeof err.error === 'string' && err.error ? err.error : 'Une erreur est survenue.',
          ),
      });
  }

  protected onSearch(value: string): void {
    this.search.set(value);
    this.page.set(0);
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
}
