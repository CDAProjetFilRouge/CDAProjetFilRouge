import { HttpClient } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { Observable } from 'rxjs';
import { AppUser } from './user.models';
import { URL_BACKEND } from '../../../core/api/api.config';

@Service()
export class UserService {
  private readonly http = inject(HttpClient);

  getUsers(): Observable<AppUser[]> {
    return this.http.get<AppUser[]>(`${URL_BACKEND}/users`);
  }
}
