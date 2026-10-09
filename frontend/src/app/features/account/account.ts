import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { ACCOUNT_STATUS_LABELS, ROLE_LABELS } from '../../core/models/user.models';
import { AnonymizationDemandService } from '../admin/anonymization-demand/anonymization-demand.service';

@Component({
  imports: [RouterLink],
  selector: 'app-account',
  styleUrl: './account.scss',
  templateUrl: './account.html',
})
export class Account {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  protected readonly user = this.authService.currentUser;
  protected readonly roleLabels = ROLE_LABELS;
  protected readonly statusLabels = ACCOUNT_STATUS_LABELS;
  private readonly anonymizationService = inject(AnonymizationDemandService);
  protected readonly requested = signal(false);
  protected readonly errorMessage = signal<string | null>(null);



  constructor() {
    this.anonymizationService.hasPendingDemand().subscribe({
      next: (pending) => this.requested.set(pending),
    });
  }

  protected logout(): void {
    this.authService.logout();
    void this.router.navigateByUrl('/login');
  }

  protected requestAnonymization() {

    if (!confirm('Voulez-vous vraiment demander l\'anonymisation de vos données ? Cette action est irréversible.')) {
      return;
    }

    this.anonymizationService.requestAnonymization().subscribe({
      next: () => {
        this.errorMessage.set(null);
        this.requested.set(true);
      },
      error: err => this.errorMessage.set(typeof err.error === 'string' && err.error ? err.error : 'Une erreur est survenue.')
    })

  }
}
