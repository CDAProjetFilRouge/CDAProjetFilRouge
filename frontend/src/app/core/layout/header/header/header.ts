import { Component, computed, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../auth/auth.service';
import { ROLE_LABELS } from '../../../models/user.models';

@Component({
  imports: [RouterLink],
  selector: 'app-header',
  styleUrl: './header.scss',
  templateUrl: './header.html',
})
export class Header {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  protected readonly user = this.authService.currentUser;
  protected readonly roleLabels = ROLE_LABELS;
  protected readonly isAffiliated = computed(() => (this.user()?.clubs.length ?? 0) > 0);

  protected logout(): void {
    this.authService.logout();
    this.router.navigateByUrl('/login');
  }
}
