import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';

@Component({
  imports: [RouterLink],
  selector: 'app-verify-account',
  styleUrl: './verify-account.scss',
  templateUrl: './verify-account.html',
})
export class VerifyAccount {
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

    this.authService.verifyAccount(token).subscribe({
      next: () => this.status.set('success'),
      error: (error: HttpErrorResponse) => {
        this.errorMessage.set(
          typeof error.error === 'string' ? error.error : "Impossible d'activer le compte.",
        );
        this.status.set('error');
      },
    });
  }
}
