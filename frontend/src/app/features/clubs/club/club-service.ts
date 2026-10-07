import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { Observable } from 'rxjs';
import { ClubModel } from './club-model';
import { URL_BACKEND } from '../../../core/api/api.config';
import { PageResponse } from './pageResponse-model';

export interface ClubFilter {
  name: string;
  category: string;
}

@Service()
export class ClubService {
  private readonly http = inject(HttpClient);

  getClub(page: number, size: number, filters?: ClubFilter): Observable<PageResponse<ClubModel>>{
    let params = new HttpParams().set('page', page).set('size', size);

    if(filters?.name){
      params = params.set('name', filters.name);
    }

    if(filters?.category){
      params = params.set('category', filters.category);
    }

    return this.http.get<PageResponse<ClubModel>>(`${URL_BACKEND}/clubs`, { params });
  }
}
