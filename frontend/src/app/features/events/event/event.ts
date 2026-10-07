import { Component, inject, signal } from '@angular/core';
import { EventFilter, EventService } from './event-service';
import { EventModel } from './event-model';
import { HttpErrorResponse } from '@angular/common/http';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';

@Component({
  imports: [ReactiveFormsModule, DatePipe],
  selector: 'app-event',
  styleUrl: './event.scss',
  templateUrl: './event.html',
})
export class Event {
  private readonly eventService = inject(EventService);

  protected readonly events = signal<EventModel[]>([]);
  protected readonly errorMessage = signal<string | null>(null);

  currentPage = 1;
  eventsPerPage = 20;
  totalElements = 0;
  totalPages = 0

  filters = new FormGroup({
    keyword: new FormControl('', { nonNullable: true}),
    category: new FormControl('', { nonNullable: true}),
    organizer: new FormControl('', {nonNullable: true}),
    city: new FormControl('', {nonNullable: true}),
    startDate: new FormControl('', {nonNullable: true}),
    endDate: new FormControl('', {nonNullable: true}),
    status: new FormControl('PUBLISHED', {nonNullable: true})
  })

  private handleError(err: HttpErrorResponse): void {
    this.errorMessage.set(typeof err.error === 'string' ? err.error : 'Une erreur est survenue.');
  }

  ngOnInit(): void {
    this.loadEvents();
  }

  loadEvents(): void {
    const filters: EventFilter = this.filters.getRawValue();

    this.eventService.search(this.currentPage - 1, this.eventsPerPage, filters).subscribe({
      next: (page) => {
        this.events.set(page.content);
        this.totalElements = page.totalElements;
        this.totalPages = page.totalPages;
      },
      error: (err) => this.handleError(err)
    });
  }

  search(): void {
    this.currentPage = 1;
    this.loadEvents();
  }

  resetFilter(): void {
    this.filters.reset({
      keyword: '',
      category: '',
      organizer: '',
      city: '',
      startDate: '',
      endDate: '',
      status: 'PUBLISHED'
    });

    this.search();
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages) {
      this.currentPage++;
      this.loadEvents();
    }
  }

  previousPage(): void {
    if (this.currentPage > 1) {
      this.currentPage--;
      this.loadEvents();
    }
  }
}
