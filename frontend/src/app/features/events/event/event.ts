import { Component, ErrorHandler, inject, signal } from '@angular/core';
import { EventService } from './event-service';
import { EventModel } from './event-model';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  imports: [],
  selector: 'app-event',
  styleUrl: './event.scss',
  templateUrl: './event.html',
})
export class Event {
  private readonly eventService = inject(EventService);
  protected readonly events = signal<EventModel[]>([]);
  protected readonly errorMessage = signal<string | null>(null);

  ngOnInit(): void{
    console.log(this.eventService.getEvents)
    this.eventService.getEvents().subscribe({
      next: (list) => this.events.set(list),
      error: (err) => this.handleError(err),
    });
  }

  private handleError(err: HttpErrorResponse): void {
    this.errorMessage.set(typeof err.error === 'string' ? err.error : 'Une erreur est survenue.');
  }

  currentPage = 1;
  eventsPerPages = 6;

  get displayEvents(){
    console.log(this.eventService.getEvents());
    const start = (this.currentPage - 1) * this.eventsPerPages;
    const end = start + this.eventsPerPages;

    return this.events().slice(start, end)
  }

  get totalPages() {
    return Math.ceil(this.events.length / this.eventsPerPages);
  }

  nextPage(){
    if (this.currentPage < this.totalPages) {
      this.currentPage++;
    }
  }

  previousPage(){
    if (this.currentPage >1){
      this.currentPage--
    }
  }

}
