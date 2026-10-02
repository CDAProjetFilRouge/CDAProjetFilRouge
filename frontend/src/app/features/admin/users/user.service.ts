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

  deleteUser(id: number): Observable<void> {
    return this.http.delete<void>(`${URL_BACKEND}/users/${id}`);
  }

  suspendUser(id: number, suspensionEndDate?: Date): Observable<AppUser> {
    return this.http.put<AppUser>(`${URL_BACKEND}/users/${id}/suspend`, {suspensionEndDate})
  }

  reactivateUser(id: number): Observable<AppUser> {
    return this.http.put<AppUser>(`${URL_BACKEND}/users/${id}/reactivate`, {})
  }
}
