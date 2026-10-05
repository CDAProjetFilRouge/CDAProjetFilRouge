import { HttpClient } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { URL_BACKEND } from '../../core/api/api.config';
import { AuthService } from '../../core/auth/auth.service';
import { AppUser } from '../../core/models/user.models';
import { ProfileUpdate } from './account.models';

@Service()
export class AccountService {
  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);

  updateProfile(id: number, profile: ProfileUpdate): Observable<AppUser> {
    return this.http
      .put<AppUser>(`${URL_BACKEND}/users/${id}`, profile)
      .pipe(tap((user) => this.authService.currentUser.set(user)));
  }
}
