import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { Observable } from 'rxjs';
import { URL_BACKEND } from '../../../core/api/api.config';
import { AccountStatus, AdminUserCreate, AdminUserUpdate, AppUser, Role } from '../../../core/models/user.models';
import { Page } from '../../../core/models/page.models';

@Service()
export class UserService {
  private readonly http = inject(HttpClient);

  getUsers(page: number, size: number, filters: { q?: string; role?: Role; status?: AccountStatus } = {}): Observable<Page<AppUser>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (filters.q) {
      params = params.set('q', filters.q);
    }
    if (filters.role) {
      params = params.set('role', filters.role);
    }
    if (filters.status) {
      params = params.set('status', filters.status);
    }
    return this.http.get<Page<AppUser>>(`${URL_BACKEND}/users`, { params });
  }

  deleteUser(id: string, deleteComments = false): Observable<void> {
    let params = new HttpParams();
    if (deleteComments) {
      params = params.set('deleteComments', deleteComments = true);
    }
    return this.http.delete<void>(`${URL_BACKEND}/users/${id}`, { params });
  }

  suspendUser(id: string, suspensionEndDate?: string): Observable<AppUser> {
    return this.http.put<AppUser>(`${URL_BACKEND}/users/${id}/suspend`, { suspensionEndDate });
  }

  reactivateUser(id: string): Observable<AppUser> {
    return this.http.put<AppUser>(`${URL_BACKEND}/users/${id}/reactivate`, {});
  }

  getUserByAdmin(id: string): Observable<AppUser> {
    return this.http.get<AppUser>(`${URL_BACKEND}/users/${id}`);
  }

  updateUserByAdmin(id: string, updatedUser: AdminUserUpdate): Observable<AppUser> {
    return this.http.put<AppUser>(`${URL_BACKEND}/users/${id}/admin`, updatedUser);
  }

  createUserByAdmin(user: AdminUserCreate): Observable<AppUser> {
    return this.http.post<AppUser>(`${URL_BACKEND}/users/admin`, user);
  }
}
