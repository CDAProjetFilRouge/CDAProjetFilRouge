import { inject } from '@angular/core';
import { CanMatchFn } from '@angular/router';
import { AuthService } from './auth.service';

// Vrai uniquement pour un visiteur non connecté (la session est restaurée par l'initializer de l'app).
export const anonymousGuard: CanMatchFn = () => !inject(AuthService).getToken();
