import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';

@Component({
  imports: [ReactiveFormsModule, RouterLink],
  selector: 'app-activate-account',
  styleUrl: './activate-account.scss',
  templateUrl: './activate-account.html',
})
export class ActivateAccount {
  private readonly route = inject(ActivatedRoute);
  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly authService = inject(AuthService);

  private readonly token = this.route.snapshot.queryParamMap.get('token');

  protected readonly hasToken = this.token !== null && this.token !== '';

  readonly form = this.formBuilder.group({
    temporaryPassword: ['', Validators.required],
    newPassword: ['', [Validators.required, Validators.minLength(12)]],
    confirmNewPassword: ['', Validators.required],
  });

  readonly errorMessage = signal('');
  readonly isSubmitting = signal(false);
  readonly accountActivated = signal(false);

  onSubmit(): void {
    if (!this.token) {
      return;
    }

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const { temporaryPassword, newPassword, confirmNewPassword } = this.form.getRawValue();

    if (newPassword !== confirmNewPassword) {
      this.errorMessage.set('Les mots de passe ne correspondent pas.');
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set('');

    this.authService
      .activateAccount({ token: this.token, temporaryPassword, newPassword, confirmNewPassword })
      .subscribe({
        next: () => {
          this.isSubmitting.set(false);
          this.accountActivated.set(true);
        },
        error: (error: HttpErrorResponse) => {
          this.isSubmitting.set(false);
          this.errorMessage.set(
            typeof error.error === 'string' ? error.error : "Impossible d'activer le compte.",
          );
        },
      });
  }
}
