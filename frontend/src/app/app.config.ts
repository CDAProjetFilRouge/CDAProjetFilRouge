import { provideHttpClient, withInterceptors } from '@angular/common/http';
import {
  ApplicationConfig,
  inject,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import { provideRouter } from '@angular/router';
import { firstValueFrom, switchMap } from 'rxjs';
import { routes } from './app.routes';
import { authInterceptor } from './core/auth/auth-interceptor';
import { AuthService } from './core/auth/auth.service';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor])),
    provideAppInitializer(() => {
      const authService = inject(AuthService);
      if (!authService.getRefreshToken()) {
        return;
      }
      return firstValueFrom(
        authService.refresh().pipe(switchMap(() => authService.loadCurrentUser())),
      ).catch((error) => {
        if (error.status === 401) {
          authService.clearSession();
        }
      });
    }),
  ],
};
