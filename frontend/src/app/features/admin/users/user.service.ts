import { HttpClient } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { Observable } from 'rxjs';
import { URL_BACKEND } from '../../../core/api/api.config';
import { AppUser } from '../../../core/models/user.models';

@Service()
export class UserService {
  private readonly http = inject(HttpClient);

  getUsers(): Observable<AppUser[]> {
    return this.http.get<AppUser[]>(`${URL_BACKEND}/users`);
  }

  deleteUser(id: number): Observable<void> {
    return this.http.delete<void>(`${URL_BACKEND}/users/${id}`);
  }

  suspendUser(id: number, suspensionEndDate?: string): Observable<AppUser> {
    return this.http.put<AppUser>(`${URL_BACKEND}/users/${id}/suspend`, { suspensionEndDate });
  }

  reactivateUser(id: number): Observable<AppUser> {
    return this.http.put<AppUser>(`${URL_BACKEND}/users/${id}/reactivate`, {});
  }
}
