import { HttpClient } from '@angular/common/http';
import { Service, inject } from '@angular/core';
import { tap } from 'rxjs';

interface LoginResponse {
  token: string;
}

@Service()
export class AuthService {
  private readonly http = inject(HttpClient);

  login(email: string, password: string) {
    return this.http.post<LoginResponse>('/login', { email, password }).pipe(
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
