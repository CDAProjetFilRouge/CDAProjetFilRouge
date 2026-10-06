import { HttpClient } from '@angular/common/http';
import { Service, inject, signal } from '@angular/core';
import { Observable, switchMap, tap } from 'rxjs';
import { URL_BACKEND } from '../api/api.config';
import { AppUser } from '../models/user.models';
import { LoginResponse, RegisterRequest } from './auth.models';

@Service()
export class AuthService {
  private readonly http = inject(HttpClient);

  readonly currentUser = signal<AppUser | null>(null);

  login(email: string, password: string) {
    return this.http.post<LoginResponse>(`${URL_BACKEND}/login`, { email, password }).pipe(
      tap((response) => {
        sessionStorage.setItem('token', response.token);
      }),
      switchMap(() => this.loadCurrentUser()),
    );
  }

  loadCurrentUser() {
    return this.http
      .get<AppUser>(`${URL_BACKEND}/users/me`)
      .pipe(tap((user) => this.currentUser.set(user)));
  }

  getToken(): string | null {
    return sessionStorage.getItem('token');
  }

  logout(): void {
    sessionStorage.removeItem('token');
    this.currentUser.set(null);
  }

  confirmPasswordChange(token: string): Observable<void> {
    return this.http.get<void>(`${URL_BACKEND}/account/password/confirm`, { params: { token } });
  }

  register(request: RegisterRequest): Observable<AppUser> {
    return this.http.post<AppUser>(`${URL_BACKEND}/users`, request);
  }
}
