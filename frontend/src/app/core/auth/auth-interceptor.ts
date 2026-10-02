import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { URL_BACKEND } from '../api/api.config';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = inject(AuthService).getToken();

  if (!token || !req.url.startsWith(URL_BACKEND)) {
    return next(req);
  }

  return next(req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
};
