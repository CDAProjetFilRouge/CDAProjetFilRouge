import { Component, inject, signal } from '@angular/core';
import { EventDetailsModel } from './event-details-model';
import { EventDetailsService } from './event-details-service';
import { ActivatedRoute } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  imports: [],
  selector: 'app-event-details',
  styleUrl: './event-details.scss',
  templateUrl: './event-details.html',
})
export class EventDetails {
  private readonly eventDetailsService = inject(EventDetailsService);
  private readonly activatedRoute = inject(ActivatedRoute);
  //private readonly id: number = parseInt(this.activatedRoute.snapshot.paramMap.get('id'));
  private readonly id: number = 2;


  protected readonly errorMessage = signal<string | null>(null);
  protected readonly eventDetail = signal<EventDetailsModel | null>(null);
  protected readonly selectedImageIndex = signal(0);

    private handleError(err: HttpErrorResponse): void {
    this.errorMessage.set(typeof err.error === 'string' ? err.error : 'Une erreur est survenue.');
  }

  ngOnInit(): void {
    this.eventDetailsService.getEventDetail(this.id).subscribe({
      next: (e) => {
        this.eventDetail.set(e);
      },
      error: (err) => this.handleError(err)
    });
  }

  selectedImage(index: number): void {
    this.selectedImageIndex.set(index)
  }

  previousImage(): void {
    const images = this.eventDetail()?.imageGallery ?? [];

    if(images.length === 0) {
      return
    }

    this.selectedImageIndex.update(index => (index - 1 + images.length) % images.length);
  }

  nextImage(): void {
    const images = this.eventDetail()?.imageGallery ?? [];

    if(images.length === 0) {
      return
    }

    this.selectedImageIndex.update( index => (index + 1) % images.length);
  }
}
