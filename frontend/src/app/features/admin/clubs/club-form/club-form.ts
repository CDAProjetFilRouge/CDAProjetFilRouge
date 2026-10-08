import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, Validators, ReactiveFormsModule } from '@angular/forms';
import { ClubService } from '../club.service';
import { Router, ActivatedRoute, RouterLink } from '@angular/router';
import { CLUB_CATEGORY_LABELS, ClubCategory, ClubRequest } from '../club.models';
import { HttpErrorResponse } from '@angular/common/http';


@Component({
  imports: [ReactiveFormsModule, RouterLink],
  selector: 'app-club-form',
  styleUrl: './club-form.scss',
  templateUrl: './club-form.html',
})
export class ClubForm {
  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly clubService = inject(ClubService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly id = Number(this.route.snapshot.paramMap.get('id'));
  protected readonly CLUB_CATEGORY_LABELS = CLUB_CATEGORY_LABELS;
  protected readonly categories = Object.keys(CLUB_CATEGORY_LABELS) as ClubCategory[];
  readonly errorMessage = signal('');
  readonly isSubmitting = signal(false);
  readonly isEdit = this.id !== 0;


  readonly form = this.formBuilder.group({
    name: ['', Validators.required],
    category: ['CULTURE' as ClubCategory],
    email: ['', [Validators.required, Validators.email]],
    phone: ['', Validators.required],
    address: this.formBuilder.group({
      street1: ['', Validators.required],
      street2: [''],
      postalCode: ['', Validators.required],
      city: ['', Validators.required],
      country: ['', Validators.required],
    }),
  });

  constructor() {
    if(this.isEdit) {
      this.clubService.getClubById(this.id).subscribe({
        next: (club) => this.form.patchValue({
          name: club.name,
          category: club.category,
          email: club.email,
          phone: club.phone,
          address: {
            street1: club.address.street1,
            street2: club.address.street2?? '',
            postalCode: club.address.postalCode,
            city: club.address.city,
            country: club.address.country,
          },
        }),
        error: () => this.errorMessage.set('Club introuvable'),
      });
    }
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    const request: ClubRequest = {
      name: value.name,
      category: value.category,
      email: value.email,
      phone: value.phone,
      address: value.address,
    };

    this.isSubmitting.set(true);
    this.errorMessage.set('');

    const call = this.isEdit
      ? this.clubService.updateClub(this.id, request)
      : this.clubService.createClub(request);

    call.subscribe({
      next: () => this.router.navigateByUrl('/admin/clubs'),
      error: (error: HttpErrorResponse) => {
        this.isSubmitting.set(false);
        this.errorMessage.set(
          typeof error.error === 'string'
            ? error.error
            : "Impossible d'enregistrer les modifications"
        )
      },
    });

  }

}
