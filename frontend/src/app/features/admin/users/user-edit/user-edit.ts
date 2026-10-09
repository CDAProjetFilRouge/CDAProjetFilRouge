import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AdminUserUpdate, AppUser, Role, ROLE_LABELS } from '../../../../core/models/user.models';
import { PHONE_PATTERN } from '../../../../core/validator/phone';
import { UserService } from '../user.service';
import { ClubService } from '../../clubs/club.service';
import { Club } from '../../clubs/club.models';

@Component({
  imports: [ReactiveFormsModule, RouterLink],
  selector: 'app-user-edit',
  styleUrl: './user-edit.scss',
  templateUrl: './user-edit.html',
})
export class UserEdit {
  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly userService = inject(UserService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly id = Number(this.route.snapshot.paramMap.get('id'));
  protected readonly ROLE_LABELS = ROLE_LABELS;
  protected readonly roles = Object.keys(ROLE_LABELS) as Role[];
  private readonly clubService = inject(ClubService);
  protected readonly clubs = signal<Club[]>([]);
  protected readonly displayedClubs = computed(() => {
    const memberIds = this.user()?.clubs.map((c) => c.id) ?? [];
    return this.clubs().filter((c) => !c.endValidityDate || memberIds.includes(c.id));
  })
  protected readonly selectedClubIds= signal<number[]>([]);

  protected readonly user = signal<AppUser | null>(null);

  readonly form = this.formBuilder.group({
    firstName: ['', Validators.required],
    lastName: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    role: ['MEMBER' as Role],
    phone: ['', Validators.pattern(PHONE_PATTERN)],
    street1: [''],
    street2: [''],
    postalCode: [''],
    city: [''],
    country: [''],
  });

  readonly errorMessage = signal('');
  readonly isSubmitting = signal(false);

  constructor() {
    this.userService.getUserByAdmin(this.id).subscribe({
      next: (user) => {
        this.user.set(user);
        this.form.patchValue({
          firstName: user.firstName,
          lastName: user.lastName,
          email: user.email,
          phone: user.phone ?? '',
          street1: user.address?.street1 ?? '',
          street2: user.address?.street2 ?? '',
          postalCode: user.address?.postalCode ?? '',
          city: user.address?.city ?? '',
          country: user.address?.country ?? '',
          role: user.role,
        });
        this.selectedClubIds.set(user.clubs.map((c) => c.id));
      },
      error: () => this.errorMessage.set('Utilisateur introuvable'),
    });

    this.clubService.getClubs(0, 100).subscribe({
      next: (result) => this.clubs.set(result.content),
      error: (error: HttpErrorResponse) => this.errorMessage.set(typeof error.error === 'string' ? error.error : 'Aucun club trouvé.'),
    });
  }

  onSubmit(): void {
    if (this.form.invalid || !this.user()) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    const requiredAddressFields = [value.street1, value.postalCode, value.city, value.country];
    const filled = requiredAddressFields.filter((field) => field.trim() !== '').length;

    if (filled > 0 && filled < requiredAddressFields.length) {
      this.errorMessage.set("Complète toute l'adresse, ou laisse-la vide.");
      return;
    }

    const profile: AdminUserUpdate = {
      firstName: value.firstName,
      lastName: value.lastName,
      email: value.email,
      phone: value.phone.trim() || null,
      address:
        filled === 0
          ? null
          : {
              street1: value.street1,
              street2: value.street2.trim() || null,
              postalCode: value.postalCode,
              city: value.city,
              country: value.country,
            },
      role: value.role,
      clubIds: this.selectedClubIds(),
    };

    this.isSubmitting.set(true);
    this.errorMessage.set('');

    this.userService.updateUserByAdmin(this.id, profile).subscribe({
      next: () => this.router.navigateByUrl('/admin/users'),
      error: (error: HttpErrorResponse) => {
        this.isSubmitting.set(false);
        this.errorMessage.set(
          typeof error.error === 'string'
            ? error.error
            : "Impossible d'enregistrer les modifications.",
        );
      },
    });
  }

  protected toggleClub(id: number, checked: boolean): void {
    this.selectedClubIds.update((ids) => checked ? [...ids, id] : ids.filter((i) => i!== id));
  }
}
