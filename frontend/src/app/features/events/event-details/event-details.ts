import { Component, computed, inject, signal } from '@angular/core';
import { EventDetailsModel } from './event-details-model';
import { EventDetailsService } from './event-details-service';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { DatePipe } from '@angular/common';
import { URL_IMAGES } from '../../../core/api/api.config';
import { AuthService } from '../../../core/auth/auth.service';

@Component({
  imports: [DatePipe, RouterLink],
  selector: 'app-event-details',
  styleUrl: './event-details.scss',
  templateUrl: './event-details.html',
})
export class EventDetails {
  private readonly eventDetailsService = inject(EventDetailsService);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);

  protected readonly errorMessage = signal<string | null>(null);
  protected readonly eventDetail = signal<EventDetailsModel | null>(null);
  protected readonly selectedImageIndex = signal(0);

  protected imageUrl(path: string): string {
    return `${URL_IMAGES}${path}`;
  }

  protected readonly canEdit = computed(() => {
    const event = this.eventDetail();
    const user = this.authService.currentUser();
    return (
      event !== null && user !== null && (user.role === 'ADMINISTRATOR' || user.id === event.organizerId)
    );
  });

    private handleError(err: HttpErrorResponse): void {
    this.errorMessage.set(typeof err.error === 'string' ? err.error : 'Une erreur est survenue.');
  }

  ngOnInit(): void {
    const idParam = this.activatedRoute.snapshot.paramMap.get('id');

    if(idParam == null){
      this.router.navigate(['/']);
      return
    }

    const id = parseInt(idParam);

    this.eventDetailsService.getEventDetail(id).subscribe({
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
