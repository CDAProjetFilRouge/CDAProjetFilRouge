import { Component, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { ACCOUNT_STATUS_LABELS, ROLE_LABELS } from '../../core/models/user.models';

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

  protected logout(): void {
    this.authService.logout();
    void this.router.navigateByUrl('/login');
  }
}
