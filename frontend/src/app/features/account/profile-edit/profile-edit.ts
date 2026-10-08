import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import { PHONE_PATTERN } from '../../../core/validator/phone';
import { ProfileUpdate } from '../account.models';
import { AccountService } from '../account.service';

@Component({
  imports: [ReactiveFormsModule, RouterLink],
  selector: 'app-profile-edit',
  styleUrl: './profile-edit.scss',
  templateUrl: './profile-edit.html',
})
export class ProfileEdit {
  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly authService = inject(AuthService);
  private readonly accountService = inject(AccountService);
  private readonly router = inject(Router);

  private readonly user = this.authService.currentUser();

  readonly form = this.formBuilder.group({
    firstName: [this.user?.firstName ?? '', Validators.required],
    lastName: [this.user?.lastName ?? '', Validators.required],
    email: [this.user?.email ?? '', [Validators.required, Validators.email]],
    phone: [this.user?.phone ?? '', Validators.pattern(PHONE_PATTERN)],
    street1: [this.user?.address?.street1 ?? ''],
    street2: [this.user?.address?.street2 ?? ''],
    postalCode: [this.user?.address?.postalCode ?? ''],
    city: [this.user?.address?.city ?? ''],
    country: [this.user?.address?.country ?? ''],
  });

  readonly errorMessage = signal('');
  readonly isSubmitting = signal(false);

  onSubmit(): void {
    if (this.form.invalid || !this.user) {
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

    const profile: ProfileUpdate = {
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
    };

    this.isSubmitting.set(true);
    this.errorMessage.set('');

    this.accountService.updateProfile(this.user.id, profile).subscribe({
      next: () => this.router.navigateByUrl('/account'),
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
}
