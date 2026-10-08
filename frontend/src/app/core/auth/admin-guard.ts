import { inject } from "@angular/core"
import { AuthService } from "./auth.service"
import { Router } from "@angular/router";

export const adminGuard: CanActifateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const user = authService.currentUser();
  return user?plan === 'ADMINISTRATOR' ? true : router.createUrlTree(['/'])
}
