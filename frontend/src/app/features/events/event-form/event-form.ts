import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, effect, inject, signal } from '@angular/core';
import {
  AbstractControl,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Observable, of, switchMap, tap } from 'rxjs';
import { URL_IMAGES } from '../../../core/api/api.config';
import { AuthService } from '../../../core/auth/auth.service';
import { Category } from '../../../core/models/user.models';
import {
  EVENT_CATEGORY_LABELS,
  EVENT_STATUS_STEPS,
  EventDetail,
  EventRequest,
  EventSaved,
  EventStatus,
} from './event-form.models';
import { EventFormService } from './event-form.service';

const MAX_PRICE = 99999999.99;
const MAX_IMAGE_BYTES = 5 * 1024 * 1024;
const MAX_IMAGES = 10;

function yearInRange(control: AbstractControl): ValidationErrors | null {
  const value = control.value as string;
  if (!value) {
    return null;
  }
  const year = Number(value.slice(0, 4));
  return year >= 1900 && year <= 2100 ? null : { yearRange: true };
}

function endAfterStart(group: AbstractControl): ValidationErrors | null {
  const start = group.get('startDateTime')?.value as string;
  const end = group.get('endDateTime')?.value as string;
  return start && end && end <= start ? { endBeforeStart: true } : null;
}

@Component({
  imports: [ReactiveFormsModule],
  selector: 'app-event-form',
  styleUrl: './event-form.scss',
  templateUrl: './event-form.html',
})
export class EventForm {
  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly eventFormService = inject(EventFormService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly eventId = Number(this.route.snapshot.paramMap.get('id')) || null;
  protected readonly isEdit = this.eventId !== null;
  protected readonly statusSteps = EVENT_STATUS_STEPS;
  protected readonly categoryLabels = EVENT_CATEGORY_LABELS;
  protected readonly categories = Object.keys(EVENT_CATEGORY_LABELS) as Category[];

  protected readonly detail = signal<EventDetail | null>(null);
  protected readonly loadError = signal('');
  protected readonly errorMessage = signal('');
  protected readonly successMessage = signal('');
  protected readonly isSubmitting = signal(false);
  protected readonly confirmingDelete = signal(false);

  protected readonly clubs = computed(() => this.authService.currentUser()?.clubs ?? []);
  protected readonly status = computed<EventStatus>(() => this.detail()?.status ?? 'DRAFT');
  protected readonly isPast = computed(() => {
    const detail = this.detail();
    return detail !== null && new Date(detail.startDate) < new Date();
  });
  protected readonly isOwner = computed(() => {
    if (!this.isEdit) {
      return true;
    }
    const detail = this.detail();
    const user = this.authService.currentUser();
    return (
      detail !== null &&
      user !== null &&
      (user.role === 'ADMINISTRATOR' || detail.organizerId === user.id)
    );
  });
  protected readonly canAddImage = computed(
    () => this.isOwner() && (this.detail()?.imageGallery.length ?? 0) < MAX_IMAGES,
  );
  protected readonly canEditFields = computed(() => this.isOwner() && !this.isPast());
  protected readonly canPublish = computed(
    () => this.isEdit && this.canEditFields() && this.status() === 'DRAFT',
  );

  protected readonly form = this.formBuilder.group(
    {
      title: ['', [Validators.required, Validators.maxLength(200)]],
      description: ['', [Validators.required, Validators.maxLength(10000)]],
      location: this.formBuilder.group({
        street1: ['', [Validators.required, Validators.maxLength(255)]],
        street2: ['', Validators.maxLength(255)],
        postalCode: ['', [Validators.required, Validators.maxLength(10)]],
        city: ['', [Validators.required, Validators.maxLength(100)]],
        country: ['France', [Validators.required, Validators.maxLength(100)]],
      }),
      category: ['CULTURE' as Category],
      clubId: [null as number | null, Validators.required],
      startDateTime: ['', [Validators.required, yearInRange]],
      endDateTime: ['', [Validators.required, yearInRange]],
      maxCapacity: [null as number | null, [Validators.required, Validators.min(1)]],
      affiliatePrice: [
        null as number | null,
        [Validators.required, Validators.min(0), Validators.max(MAX_PRICE)],
      ],
      nonAffiliatePrice: [
        null as number | null,
        [Validators.required, Validators.min(0), Validators.max(MAX_PRICE)],
      ],
    },
    { validators: endAfterStart },
  );

  constructor() {
    effect(() => {
      const clubs = this.clubs();
      if (!this.isEdit && clubs.length === 1 && this.form.controls.clubId.value === null) {
        this.form.controls.clubId.setValue(clubs[0].id);
      }
    });

    if (this.isEdit) {
      this.form.controls.clubId.disable();
      this.load();
    }
  }

  protected imageUrl(path: string): string {
    return `${URL_IMAGES}${path}`;
  }

  protected showError(path: string): boolean {
    const control = this.form.get(path);
    return !!control && control.touched && control.invalid;
  }

  protected saveDraft(): void {
    this.submit(false);
  }

  protected publish(): void {
    this.submit(true);
  }

  protected askDelete(): void {
    this.confirmingDelete.set(true);
  }

  protected cancelDelete(): void {
    this.confirmingDelete.set(false);
  }

  protected confirmDelete(): void {
    if (this.eventId === null) {
      return;
    }
    this.isSubmitting.set(true);
    this.eventFormService.delete(this.eventId).subscribe({
      next: () => this.router.navigateByUrl('/'),
      error: (error: HttpErrorResponse) => {
        this.isSubmitting.set(false);
        this.confirmingDelete.set(false);
        this.errorMessage.set(this.reasonOf(error, "Impossible de supprimer l'évènement."));
      },
    });
  }

  protected onFileSelected(input: HTMLInputElement): void {
    const file = input.files?.[0];
    if (!file || this.eventId === null) {
      return;
    }
    this.errorMessage.set('');
    if (file.size > MAX_IMAGE_BYTES) {
      input.value = '';
      this.errorMessage.set("L'image ne peut pas dépasser 5 Mo.");
      return;
    }
    if (!file.type.startsWith('image/')) {
      input.value = '';
      this.errorMessage.set('Le fichier doit être une image.');
      return;
    }
    if ((this.detail()?.imageGallery.length ?? 0) >= MAX_IMAGES) {
      input.value = '';
      this.errorMessage.set('Un évènement ne peut pas avoir plus de 10 images.');
      return;
    }
    this.eventFormService.uploadImage(this.eventId, file).subscribe({
      next: (image) => {
        this.detail.update((d) => (d ? { ...d, imageGallery: [...d.imageGallery, image] } : d));
        input.value = '';
      },
      error: (error: HttpErrorResponse) => {
        input.value = '';
        this.errorMessage.set(this.reasonOf(error, "Impossible d'ajouter l'image."));
      },
    });
  }

  protected removeImage(imageId: number): void {
    if (this.eventId === null) {
      return;
    }
    this.errorMessage.set('');
    this.eventFormService.deleteImage(this.eventId, imageId).subscribe({
      next: () =>
        this.detail.update((d) =>
          d ? { ...d, imageGallery: d.imageGallery.filter((i) => i.id !== imageId) } : d,
        ),
      error: (error: HttpErrorResponse) =>
        this.errorMessage.set(this.reasonOf(error, "Impossible de supprimer l'image.")),
    });
  }

  private load(): void {
    if (this.eventId === null) {
      return;
    }
    this.eventFormService.getById(this.eventId).subscribe({
      next: (event) => {
        this.detail.set(event);
        this.form.patchValue({
          title: event.title,
          description: event.description,
          location: {
            street1: event.street1,
            street2: event.street2 ?? '',
            postalCode: event.postalCode,
            city: event.city,
            country: event.country,
          },
          category: event.category,
          startDateTime: event.startDate.slice(0, 16),
          endDateTime: event.endDate.slice(0, 16),
          maxCapacity: event.maxCapacity,
          affiliatePrice: event.affiliatePrice,
          nonAffiliatePrice: event.nonAffiliatePrice,
        });
        if (!this.canEditFields()) {
          this.form.disable();
        }
      },
      error: () => this.loadError.set('Évènement introuvable.'),
    });
  }

  private submit(publish: boolean): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const request = this.toRequest();
    let createdId: number | null = null;

    this.isSubmitting.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');

    let call: Observable<EventSaved>;
    if (this.eventId === null) {
      call = this.eventFormService.create(request).pipe(
        tap((created) => (createdId = created.id)),
        switchMap((created) =>
          publish
            ? this.eventFormService.update(created.id, { ...request, status: 'PUBLISHED' })
            : of(created),
        ),
      );
    } else {
      call = this.eventFormService.update(this.eventId, {
        ...request,
        status: publish ? 'PUBLISHED' : this.status(),
      });
    }

    call.subscribe({
      next: (saved) => {
        this.isSubmitting.set(false);
        this.onSaved(saved, publish);
      },
      error: (error: HttpErrorResponse) => {
        this.isSubmitting.set(false);
        const reason = this.reasonOf(error, "Impossible d'enregistrer l'évènement.");
        this.errorMessage.set(
          createdId !== null
            ? `Le brouillon a été enregistré, mais la publication a échoué : ${reason}`
            : reason,
        );
      },
    });
  }

  private onSaved(saved: EventSaved, publish: boolean): void {
    if (!this.isEdit) {
      if (publish) {
        this.router.navigateByUrl('/');
      } else {
        this.router.navigate(['/events', saved.id, 'edit']);
      }
      return;
    }

    this.detail.update((d) => (d ? { ...d, status: saved.status ?? d.status } : d));
    this.successMessage.set(publish ? 'Évènement publié.' : 'Modifications enregistrées.');
  }

  private reasonOf(error: HttpErrorResponse, fallback: string): string {
    if (typeof error.error === 'string' && error.error) {
      return error.error;
    }
    if (error.status === 0) {
      return 'Le serveur est injoignable. Vérifiez votre connexion et réessayez.';
    }
    if (error.status === 400) {
      return `${fallback} Le serveur refuse certaines valeurs : vérifiez les tarifs, les dates et le nombre de places.`;
    }
    if (error.status === 401 || error.status === 403) {
      return "Vous n'avez pas le droit d'effectuer cette action.";
    }
    if (error.status >= 500) {
      return `${fallback} Le serveur a rencontré une erreur (${error.status}). Réessayez, et prévenez l'équipe si cela continue.`;
    }
    return fallback;
  }

  private toRequest(): EventRequest {
    const value = this.form.getRawValue();
    const request: EventRequest = {
      title: value.title,
      description: value.description,
      location: value.location,
      category: value.category,
      startDateTime: value.startDateTime,
      endDateTime: value.endDateTime,
      affiliatePrice: value.affiliatePrice as number,
      nonAffiliatePrice: value.nonAffiliatePrice as number,
      maxCapacity: value.maxCapacity as number,
    };
    if (!this.isEdit) {
      request.idClub = value.clubId as number;
    }
    return request;
  }
}
