import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, effect, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { switchMap } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { Role } from '../../core/models/user.models';
import { Calendar } from '../calendar/calendar';
import { EventRequest } from '../events/event-form/event-form.models';
import { EventFormService } from '../events/event-form/event-form.service';
import { EventModel } from '../events/event/event-model';
import { EventService } from '../events/event/event-service';
import { Kpi, KpiView, QuickAction } from './dashboard.models';
import { DashboardService } from './dashboard.service';

// Page qui détaille chaque indicateur (les codes viennent de DashboardService côté back).
interface KpiTarget {
  link: string;
  queryParams?: Record<string, string>;
}

const KPI_LINKS: Record<string, KpiTarget> = {
  ACTIVE_ACCOUNTS: { link: '/admin/users', queryParams: { status: 'ACTIVE' } },
  SUSPENDED_ACCOUNTS: { link: '/admin/users', queryParams: { status: 'SUSPENDED' } },
  PENDING_ANONYMIZATIONS: { link: '/admin/anonymization-demands' },
  ACTIVE_CLUBS: { link: '/admin/clubs' },
  UPCOMING_EVENTS: { link: '/admin/events' },
  ORGANIZED_DRAFTS: { link: '/my-events', queryParams: { status: 'DRAFT' } },
};

function toKpiView(kpi: Kpi): KpiView {
  return {
    ...kpi,
    link: KPI_LINKS[kpi.code]?.link ?? null,
    queryParams: KPI_LINKS[kpi.code]?.queryParams ?? null,
    needsAttention: kpi.code === 'PENDING_ANONYMIZATIONS' && kpi.value > 0,
  };
}

const WELCOME_MESSAGES: Record<Role, string> = {
  MEMBER: 'Retrouvez vos prochains événements et vos inscriptions.',
  ORGANIZER: "Suivez vos événements et le remplissage de vos inscriptions.",
  ADMINISTRATOR: "Voici l'état de la plateforme.",
};

const QUICK_ACTIONS: Record<Role, QuickAction[]> = {
  MEMBER: [
    { label: 'Parcourir les événements', link: '/' },
    { label: 'Mon compte', link: '/account' },
  ],
  ORGANIZER: [
    { label: 'Créer un événement', link: '/events/new' },
    { label: 'Parcourir les événements', link: '/' },
    { label: 'Mon calendrier', link: '/calendar' },
    { label: 'Mon compte', link: '/account' },
  ],
  ADMINISTRATOR: [
    { label: 'Traiter les demandes', link: '/admin/anonymization-demands' },
    { label: 'Créer un compte', link: '/admin/users/new' },
    { label: 'Créer un club', link: '/admin/clubs/new' },
    { label: 'Gérer les événements', link: '/admin/events' },
    { label: 'Documents légaux', link: '/admin/terms-of-use' },
  ],
};

@Component({
  imports: [RouterLink, Calendar, DatePipe],
  selector: 'app-dashboard',
  styleUrl: './dashboard.scss',
  templateUrl: './dashboard.html',
})
export class Dashboard {
  private readonly auth = inject(AuthService);
  private readonly dashboardService = inject(DashboardService);
  private readonly eventService = inject(EventService);
  private readonly eventFormService = inject(EventFormService);

  protected readonly user = this.auth.currentUser;
  protected readonly kpis = signal<KpiView[]>([]);
  protected readonly loading = signal(true);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly clubNames = computed(() => (this.user()?.clubs ?? []).map((club) => club.name));

  protected readonly welcomeMessage = computed(() => {
    const role = this.user()?.role;
    return role ? WELCOME_MESSAGES[role] : '';
  });

  protected readonly quickActions = computed(() => {
    const role = this.user()?.role;
    return role ? QUICK_ACTIONS[role] : [];
  });

  protected readonly showCalendar = computed(() => this.user()?.role !== 'ADMINISTRATOR');
  protected readonly showDrafts = computed(() => this.user()?.role === 'ORGANIZER');

  protected readonly drafts = signal<EventModel[]>([]);
  protected readonly draftsTotal = signal(0);
  protected readonly draftsLoading = signal(false);
  protected readonly draftsError = signal<string | null>(null);
  protected readonly publishingId = signal<string | null>(null);

  constructor() {
    effect(() => {
      if (this.user()) {
        this.loadKpis();
      }
      if (this.showDrafts()) {
        this.loadDrafts();
      }
    });
  }

  protected loadDrafts(): void {
    this.draftsLoading.set(true);
    this.eventService
      .search(0, 5, {
        keyword: '',
        category: '',
        club: '',
        city: '',
        startDate: '',
        endDate: '',
        status: 'DRAFT',
        sort: 'asc',
      })
      .subscribe({
        next: (page) => {
          this.drafts.set(page.content);
          this.draftsTotal.set(page.totalElements);
          this.draftsError.set(null);
          this.draftsLoading.set(false);
        },
        error: () => {
          this.draftsError.set('Les brouillons n’ont pas pu être chargés.');
          this.draftsLoading.set(false);
        },
      });
  }

  protected publishDraft(draft: EventModel): void {
    if (this.publishingId() !== null) {
      return;
    }
    this.publishingId.set(draft.id);
    this.draftsError.set(null);

    this.eventFormService
      .getById(draft.id)
      .pipe(
        switchMap((detail) => {
          const request: EventRequest = {
            title: detail.title,
            description: detail.description,
            location: {
              street1: detail.street1,
              street2: detail.street2 ?? '',
              postalCode: detail.postalCode,
              city: detail.city,
              country: detail.country,
            },
            category: detail.category,
            startDateTime: detail.startDate,
            endDateTime: detail.endDate,
            affiliatePrice: detail.affiliatePrice,
            nonAffiliatePrice: detail.nonAffiliatePrice,
            maxCapacity: detail.maxCapacity,
            status: 'PUBLISHED',
          };
          return this.eventFormService.update(draft.id, request);
        }),
      )
      .subscribe({
        next: () => {
          this.publishingId.set(null);
          this.loadDrafts();
          this.loadKpis();
        },
        error: (err: HttpErrorResponse) => {
          this.publishingId.set(null);
          this.draftsError.set(
            typeof err.error === 'string' && err.error
              ? err.error
              : 'La publication a échoué. Ouvrez le brouillon pour corriger les informations.',
          );
        },
      });
  }

  protected loadKpis(): void {
    this.loading.set(true);
    this.dashboardService.getKpis().subscribe({
      next: (kpis) => {
        this.errorMessage.set(null);
        this.kpis.set(kpis.map(toKpiView));
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
