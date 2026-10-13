import { DatePipe } from '@angular/common';
import { Component, DestroyRef, computed, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { URL_IMAGES } from '../../core/api/api.config';
import { EventModel } from '../events/event/event-model';
import { EventService } from '../events/event/event-service';

const AUTOPLAY_DELAY_MS = 6000;
const CAROUSEL_SIZE = 5;

const CATEGORY_LABELS: Record<string, string> = {
  CULTURE: 'Culture',
  SPORT: 'Sport',
  DIVERTISSEMENT: 'Divertissement',
};

@Component({
  imports: [DatePipe, RouterLink],
  selector: 'app-home',
  styleUrl: './home.scss',
  templateUrl: './home.html',
})
export class Home implements OnInit {
  private readonly eventService = inject(EventService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly events = signal<EventModel[]>([]);
  protected readonly index = signal(0);
  protected readonly paused = signal(false);
  protected readonly slideCount = computed(() => this.events().length);

  protected readonly steps = [
    {
      title: 'Créez votre compte',
      text: 'Quelques informations suffisent. Un e-mail de confirmation active votre compte.',
    },
    {
      title: 'Rejoignez un club',
      text: 'Les adhérents d’un club affilié bénéficient du tarif affilié sur les évènements.',
    },
    {
      title: 'Inscrivez-vous aux évènements',
      text: 'Réservez votre place, ou rejoignez la liste d’attente quand l’évènement est complet.',
    },
  ];

  ngOnInit(): void {
    this.eventService.search(0, 20, this.publishedFilter()).subscribe({
      next: (page) => {
        const now = Date.now();
        const upcoming = page.content
          .filter((event) => new Date(event.endDateTime).getTime() >= now)
          .sort(
            (a, b) => new Date(a.startDateTime).getTime() - new Date(b.startDateTime).getTime(),
          );
        this.events.set(upcoming.slice(0, CAROUSEL_SIZE));
      },
      // La page reste utile sans le carrousel : on masque simplement la section.
      error: () => this.events.set([]),
    });

    this.startAutoplay();
  }

  protected imageUrl(event: EventModel): string | null {
    const first = [...event.imageGallery].sort((a, b) => a.displayOrder - b.displayOrder)[0];
    return first ? `${URL_IMAGES}${first.path}` : null;
  }

  protected categoryLabel(category: string): string {
    return CATEGORY_LABELS[category] ?? category;
  }

  protected goTo(position: number): void {
    const count = this.slideCount();
    if (count > 0) {
      this.index.set((position + count) % count);
    }
  }

  protected previous(): void {
    this.goTo(this.index() - 1);
  }

  protected next(): void {
    this.goTo(this.index() + 1);
  }

  private publishedFilter() {
    return {
      keyword: '',
      category: '',
      club: '',
      city: '',
      startDate: '',
      endDate: '',
      status: 'PUBLISHED',
    };
  }

  private startAutoplay(): void {
    // Pas de défilement automatique pour les personnes qui ont demandé moins d'animations.
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
      return;
    }
    const timer = setInterval(() => {
      if (!this.paused() && this.slideCount() > 1) {
        this.next();
      }
    }, AUTOPLAY_DELAY_MS);
    this.destroyRef.onDestroy(() => clearInterval(timer));
  }
}
