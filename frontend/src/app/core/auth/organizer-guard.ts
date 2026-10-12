import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

export const organizerGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const role = authService.currentUser()?.role;
  return role === 'ORGANIZER' || role === 'ADMINISTRATOR' ? true : router.createUrlTree(['/']);
};
