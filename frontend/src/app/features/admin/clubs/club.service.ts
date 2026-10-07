import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { Observable } from 'rxjs';
import { URL_BACKEND } from '../../../core/api/api.config';
import { Club, ClubCategory } from './club.models';
import { Page } from '../../../core/models/page.models';


@Service()
export class ClubService {
  private readonly http = inject(HttpClient);

  getClubs(page: number, size: number, category?: ClubCategory, name?: string, city?: string): Observable<Page<Club>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (category) params = params.set('category', category);
    if (name) params = params.set('name', name);
    if (city) params = params.set('city', city);

    return this.http.get<Page<Club>>(`${URL_BACKEND}/clubs/admin`, { params });
  }

  getClubById(id: number): Observable<Club> {
    return this.http.get<Club>(`${URL_BACKEND}/clubs/${id}`);
  }

  deleteClub(id: number): Observable<void> {
    return this.http.delete<void>(`${URL_BACKEND}/clubs/${id}`);
  }
}
