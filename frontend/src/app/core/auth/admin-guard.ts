import { inject } from "@angular/core"
import { AuthService } from "./auth.service"
import { CanActivateFn, Router } from "@angular/router";

export const adminGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const user = authService.currentUser();
  return user?.role === 'ADMINISTRATOR' ? true : router.createUrlTree(['/']);
};
