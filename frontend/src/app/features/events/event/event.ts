import { Component, inject } from '@angular/core';
import { EventService } from './event-service';
import { EventModel } from './event-model';

@Component({
  imports: [],
  selector: 'app-event',
  styleUrl: './event.scss',
  templateUrl: './event.html',
})
export class Event {
  private readonly eventService = inject(EventService);
  events: EventModel[] = [];

  ngOnInit(): void{
    console.log(this.eventService.getEvents)
    this.eventService.getEvents().subscribe({
      next: (events) => {
        this.events = this.events;
      },
      error: (error) => {
        console.error('Failed to load events', error);
      }
    })
  }

  currentPage = 1;
  eventsPerPages = 6;

  get displayEvents(){
    const start = (this.currentPage - 1) * this.eventsPerPages;
    const end = start + this.eventsPerPages;

    return this.events.slice(start, end)
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
