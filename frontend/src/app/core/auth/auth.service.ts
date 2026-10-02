import { HttpClient } from '@angular/common/http';
import { Service, inject } from '@angular/core';
import { tap } from 'rxjs';
import { URL_BACKEND } from '../api/api.config';

interface LoginResponse {
  token: string;
}

@Service()
export class AuthService {
  private readonly http = inject(HttpClient);

  login(email: string, password: string) {
    return this.http.post<LoginResponse>(`${URL_BACKEND}/login`, { email, password }).pipe(
      tap((response) => {
        sessionStorage.setItem('token', response.token);
      }),
    );
  }

  getToken(): string | null {
    return sessionStorage.getItem('token');
  }

  logout(): void {
    sessionStorage.removeItem('token');
  }
}
