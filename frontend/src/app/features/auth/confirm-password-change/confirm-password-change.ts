import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';

@Component({
  imports: [RouterLink],
  selector: 'app-confirm-password-change',
  styleUrl: './confirm-password-change.scss',
  templateUrl: './confirm-password-change.html',
})
export class ConfirmPasswordChange {
  private readonly route = inject(ActivatedRoute);
  private readonly authService = inject(AuthService);

  protected readonly status = signal<'loading' | 'success' | 'error'>('loading');
  protected readonly errorMessage = signal('');

  constructor() {
    const token = this.route.snapshot.queryParamMap.get('token');

    if (!token) {
      this.errorMessage.set('Lien invalide : le jeton est manquant.');
      this.status.set('error');
      return;
    }

    this.authService.confirmPasswordChange(token).subscribe({
      next: () => this.status.set('success'),
      error: (error: HttpErrorResponse) => {
        this.errorMessage.set(
          typeof error.error === 'string' ? error.error : 'Impossible de confirmer le changement.',
        );
        this.status.set('error');
      },
    });
  }
}
