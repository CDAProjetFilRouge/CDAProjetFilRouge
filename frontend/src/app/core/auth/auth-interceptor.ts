import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';
import { URL_BACKEND } from '../api/api.config';
import { AuthService } from './auth.service';

const AUTH_URLS = new Set([
  `${URL_BACKEND}/login`,
  `${URL_BACKEND}/auth/refresh`,
  `${URL_BACKEND}/auth/logout`,
]);

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!req.url.startsWith(URL_BACKEND) || AUTH_URLS.has(req.url)) {
    return next(req);
  }

  const withToken = (request: HttpRequest<unknown>) => {
    const token = authService.getToken();
    return token ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : request;
  };

  return next(withToken(req)).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status !== 401 || !authService.getRefreshToken()) {
        return throwError(() => error);
      }
      return authService.refresh().pipe(
        catchError((refreshError: HttpErrorResponse) => {
          if (refreshError.status === 401) {
            authService.clearSession();
            void router.navigate(['/login']);
          }
          return throwError(() => refreshError);
        }),
        switchMap(() => next(withToken(req))),
      );
    }),
  );
};
