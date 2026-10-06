import { Component, ErrorHandler, inject, signal } from '@angular/core';
import { EventService } from './event-service';
import { EventModel } from './event-model';
import { HttpErrorResponse } from '@angular/common/http';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';

@Component({
  imports: [ReactiveFormsModule],
  selector: 'app-event',
  styleUrl: './event.scss',
  templateUrl: './event.html',
})
export class Event {
  private readonly eventService = inject(EventService);
  protected readonly events = signal<EventModel[]>([]);
  protected readonly errorMessage = signal<string | null>(null);

  filters = new FormGroup({
    keyword: new FormControl('', { nonNullable: true}),
    category: new FormControl('', { nonNullable: true}),
    organizer: new FormControl('', {nonNullable: true}),
    city: new FormControl('', {nonNullable: true}),
    startDate: new FormControl('', {nonNullable: true}),
    endDate: new FormControl('', {nonNullable: true}),
    status: new FormControl('PUBLISHED', {nonNullable: true})
  })


    ngOnInit(): void{
    this.eventService.getEvents().subscribe({
      next: (list) => this.events.set(list),
      error: (err) => this.handleError(err),
    });
  }

  onSubmit(): void {
    console.log("SUBMIT GONE");
    console.log(this.filters.getRawValue());
    this.search();
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

  search(): void {
    const filters = this.filters.getRawValue();

    this.currentPage = 1;

    this.eventService.searchEvent(filters).subscribe({
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
    return Math.ceil(this.events().length / this.eventsPerPages);
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
