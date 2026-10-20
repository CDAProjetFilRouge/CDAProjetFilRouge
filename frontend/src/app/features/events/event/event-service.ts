import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Service} from '@angular/core';
import { Observable } from 'rxjs';
import { URL_BACKEND } from '../../../core/api/api.config';
import { EventModel } from './event-model';
import { PageResponse } from './pageReponse-model';


export interface EventFilter {
  keyword: string;
  category: string;
  club: string;
  city: string;
  startDate: string;
  endDate: string;
  status: string;
  hideFinished?: boolean;
  sort?: 'asc' | 'desc';
}


@Service()
export class EventService {
    private readonly http = inject(HttpClient);

  getEvent(page: number, size: number = 20): Observable<PageResponse<EventModel>>{
    const params = new HttpParams().set('page', page).set('size', size);

    return this.http.get<PageResponse<EventModel>>(`${URL_BACKEND}/events`, { params });
  }

  getMine(page: number = 0, size: number = 20, status: string = ''): Observable<PageResponse<EventModel>> {
    let params = new HttpParams().set('page', page).set('size', size);

    if (status) {
      params = params.set('status', status);
    }

    return this.http.get<PageResponse<EventModel>>(`${URL_BACKEND}/events/mine`, { params });
  }

  search(page: number = 0, size: number = 20, filters?: EventFilter): Observable<PageResponse<EventModel>>{
    let params = new HttpParams()
          .set("page", page)
          .set("size", size);

    if(filters?.keyword) {
      params = params.set("keyword", filters.keyword);
    }

    if(filters?.category) {
      params = params.set('category', filters.category);
    }

    if(filters?.club) {
      params = params.set('clubName', filters.club);
    }

    if(filters?.city) {
      params = params.set('city', filters.city);
    }

    if(filters?.startDate) {
      params = params.set('startDate', filters.startDate);
    }

    if(filters?.endDate) {
      params = params.set('endDate', filters.endDate);
    }

    if(filters?.status) {
      params = params.set('status', filters.status);
    }

    if(filters?.hideFinished) {
      params = params.set('hideFinished', true);
    }

    if(filters?.sort) {
      params = params.set('sort', filters.sort.toUpperCase());
    }


    return this.http.get<PageResponse<EventModel>>(`${URL_BACKEND}/events/filter`, { params });
  }
}
