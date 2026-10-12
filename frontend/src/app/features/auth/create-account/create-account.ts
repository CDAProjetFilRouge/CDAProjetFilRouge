import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import { PHONE_PATTERN } from '../../../core/validator/phone';

@Component({
  imports: [ReactiveFormsModule, RouterLink],
  selector: 'app-create-account',
  styleUrl: './create-account.scss',
  templateUrl: './create-account.html',
})
export class CreateAccount {
  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly authService = inject(AuthService);

  readonly form = this.formBuilder.group({
    firstName: ['', Validators.required],
    lastName: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    phone: ['', Validators.pattern(PHONE_PATTERN)],
    password: ['', [Validators.required, Validators.minLength(12)]],
    confirmPassword: ['', Validators.required],
  });

  readonly errorMessage = signal('');
  readonly isSubmitting = signal(false);
  readonly accountCreated = signal(false);

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const { firstName, lastName, email, phone, password, confirmPassword } =
      this.form.getRawValue();

    if (password !== confirmPassword) {
      this.errorMessage.set('Les mots de passe ne correspondent pas.');
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set('');

    this.authService
      .register({ firstName, lastName, email, password, phone: phone.trim() || null })
      .subscribe({
        next: () => {
          this.isSubmitting.set(false);
          this.accountCreated.set(true);
        },
        error: (error: HttpErrorResponse) => {
          this.isSubmitting.set(false);
          this.errorMessage.set(
            typeof error.error === 'string' ? error.error : 'Impossible de créer le compte.',
          );
        },
      });
  }
}
