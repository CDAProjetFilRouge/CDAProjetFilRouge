import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, effect, inject, signal } from '@angular/core';
import {
  AbstractControl,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { Router } from '@angular/router';
import { of, switchMap, tap } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import { Category } from '../../../core/models/user.models';
import { EVENT_CATEGORY_LABELS, EventRequest } from './event-form.models';
import { EventFormService } from './event-form.service';

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

  protected readonly categoryLabels = EVENT_CATEGORY_LABELS;
  protected readonly categories = Object.keys(EVENT_CATEGORY_LABELS) as Category[];
  protected readonly clubs = computed(() => this.authService.currentUser()?.clubs ?? []);
  protected readonly errorMessage = signal('');
  protected readonly successMessage = signal('');
  protected readonly isSubmitting = signal(false);

  protected readonly form = this.formBuilder.group(
    {
      title: ['', [Validators.required, Validators.maxLength(200)]],
      description: ['', Validators.required],
      location: this.formBuilder.group({
        street1: ['', Validators.required],
        street2: [''],
        postalCode: ['', Validators.required],
        city: ['', Validators.required],
        country: ['France', Validators.required],
      }),
      category: ['CULTURE' as Category],
      clubId: [null as number | null, Validators.required],
      startDateTime: ['', Validators.required],
      endDateTime: ['', Validators.required],
      maxCapacity: [null as number | null, [Validators.required, Validators.min(1)]],
      affiliatePrice: [null as number | null, [Validators.required, Validators.min(0)]],
      nonAffiliatePrice: [null as number | null, [Validators.required, Validators.min(0)]],
    },
    { validators: endAfterStart },
  );

  constructor() {
    effect(() => {
      const clubs = this.clubs();
      if (clubs.length === 1 && this.form.controls.clubId.value === null) {
        this.form.controls.clubId.setValue(clubs[0].id);
      }
    });
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

    this.eventFormService
      .create(request)
      .pipe(
        tap((created) => (createdId = created.id)),
        switchMap((created) =>
          publish
            ? this.eventFormService.update(created.id, { ...request, status: 'PUBLISHED' })
            : of(created),
        ),
      )
      .subscribe({
        next: () => {
          this.isSubmitting.set(false);
          if (publish) {
            this.router.navigateByUrl('/');
            return;
          }
          this.successMessage.set('Brouillon enregistré.');
          this.form.reset();
        },
        error: (error: HttpErrorResponse) => {
          this.isSubmitting.set(false);
          const reason =
            typeof error.error === 'string' && error.error
              ? error.error
              : "Impossible d'enregistrer l'évènement.";
          this.errorMessage.set(
            createdId !== null
              ? `Le brouillon a été enregistré, mais la publication a échoué : ${reason}`
              : reason,
          );
        },
      });
  }

  private toRequest(): EventRequest {
    const value = this.form.getRawValue();
    return {
      title: value.title,
      description: value.description,
      location: value.location,
      category: value.category,
      startDateTime: value.startDateTime,
      endDateTime: value.endDateTime,
      affiliatePrice: value.affiliatePrice as number,
      nonAffiliatePrice: value.nonAffiliatePrice as number,
      maxCapacity: value.maxCapacity as number,
      idClub: value.clubId as number,
    };
  }
}
