import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, from, map, Observable, of, switchMap, throwError } from 'rxjs';
import { URL_BACKEND } from '../api/api.config';
import { AuthService } from './auth.service';
import { DpopService } from './dpop.service';

const PROOF_ONLY_URLS = new Set([`${URL_BACKEND}/login`, `${URL_BACKEND}/auth/refresh`]);
const UNSIGNED_URLS = new Set([`${URL_BACKEND}/auth/logout`]);

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const dpopService = inject(DpopService);
  const router = inject(Router);

  if (!req.url.startsWith(URL_BACKEND) || UNSIGNED_URLS.has(req.url)) {
    return next(req);
  }

  if (PROOF_ONLY_URLS.has(req.url)) {
    return from(dpopService.createProof(req.method, req.url)).pipe(
      map((proof) => (proof ? req.clone({ setHeaders: { DPoP: proof } }) : req)),
      switchMap((request) => next(request)),
    );
  }

  const authorize = (request: HttpRequest<unknown>): Observable<HttpRequest<unknown>> => {
    const token = authService.getToken();
    if (!token) {
      return of(request);
    }
    return from(dpopService.createProof(request.method, request.url, token)).pipe(
      map((proof) =>
        request.clone({
          setHeaders: proof
            ? { Authorization: `Bearer ${token}`, DPoP: proof }
            : { Authorization: `Bearer ${token}` },
        }),
      ),
    );
  };

  return authorize(req).pipe(
    switchMap((request) => next(request)),
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
        switchMap(() => authorize(req)),
        switchMap((request) => next(request)),
      );
    }),
  );
};
