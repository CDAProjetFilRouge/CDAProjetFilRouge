import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AccountService } from '../account.service';

@Component({
  imports: [ReactiveFormsModule, RouterLink],
  selector: 'app-password-change',
  styleUrl: './password-change.scss',
  templateUrl: './password-change.html',
})
export class PasswordChange {
  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly accountService = inject(AccountService);

  readonly form = this.formBuilder.group({
    currentPassword: ['', Validators.required],
    newPassword: ['', [Validators.required, Validators.minLength(12)]],
    confirmNewPassword: ['', Validators.required],
  });

  readonly errorMessage = signal('');
  readonly isSubmitting = signal(false);
  readonly emailSent = signal(false);

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const { currentPassword, newPassword, confirmNewPassword } = this.form.getRawValue();

    if (newPassword !== confirmNewPassword) {
      this.errorMessage.set('Les mots de passe ne correspondent pas.');
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set('');

    this.accountService.changePassword({ currentPassword, newPassword }).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.emailSent.set(true);
      },
      error: (error: HttpErrorResponse) => {
        this.isSubmitting.set(false);
        this.errorMessage.set(
          typeof error.error === 'string' ? error.error : 'Impossible de demander le changement.',
        );
      },
    });
  }
}
