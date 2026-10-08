import { HttpClient } from '@angular/common/http';
import { inject, Service, signal } from '@angular/core';
import { finalize, Observable, shareReplay, switchMap, tap } from 'rxjs';
import { URL_BACKEND } from '../api/api.config';
import { AppUser } from '../models/user.models';
import { DpopService } from './dpop.service';
import {
  AccountActivationRequest,
  LoginResponse,
  PasswordResetRequest,
  RegisterRequest,
} from './auth.models';

@Service()
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly dpopService = inject(DpopService);

  readonly currentUser = signal<AppUser | null>(null);
  private accessToken: string | null = null;
  private refreshInFlight$: Observable<LoginResponse> | null = null;

  login(email: string, password: string) {
    return this.http.post<LoginResponse>(`${URL_BACKEND}/login`, { email, password }).pipe(
      tap((response) => {
        this.storeTokens(response);
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
    return this.accessToken;
  }

  logout(): void {
    const refreshToken = this.getRefreshToken();
    if (refreshToken) {
      this.http
        .post<void>(`${URL_BACKEND}/auth/logout`, { refreshToken })
        .subscribe({ error: () => {} });
    }
    this.clearSession();
  }

  confirmPasswordChange(token: string): Observable<void> {
    return this.http.get<void>(`${URL_BACKEND}/account/password/confirm`, { params: { token } });
  }

  register(request: RegisterRequest): Observable<AppUser> {
    return this.http.post<AppUser>(`${URL_BACKEND}/users`, request);
  }

  verifyAccount(token: string): Observable<void> {
    return this.http.get<void>(`${URL_BACKEND}/account/verify`, { params: { token } });
  }

  requestPasswordReset(email: string): Observable<void> {
    return this.http.post<void>(
      `${URL_BACKEND}/account/password/forgot?email=${encodeURIComponent(email)}`,
      null,
    );
  }

  resetPassword(request: PasswordResetRequest): Observable<void> {
    return this.http.post<void>(`${URL_BACKEND}/account/password/reset`, request);
  }

  activateAccount(request: AccountActivationRequest): Observable<void> {
    return this.http.post<void>(`${URL_BACKEND}/account/activate`, request);
  }

  refresh(): Observable<LoginResponse> {
    this.refreshInFlight$ ??= this.http
      .post<LoginResponse>(`${URL_BACKEND}/auth/refresh`, {
        refreshToken: this.getRefreshToken(),
      })
      .pipe(
        tap((response) => this.storeTokens(response)),
        finalize(() => (this.refreshInFlight$ = null)),
        shareReplay(1),
      );
    return this.refreshInFlight$;
  }

  getRefreshToken(): string | null {
    return localStorage.getItem('refreshToken');
  }

  clearSession(): void {
    this.accessToken = null;
    localStorage.removeItem('refreshToken');
    this.currentUser.set(null);
    void this.dpopService.clear().catch(() => undefined);
  }

  private storeTokens(response: LoginResponse): void {
    this.accessToken = response.token;
    localStorage.setItem('refreshToken', response.refreshToken);
  }
}
