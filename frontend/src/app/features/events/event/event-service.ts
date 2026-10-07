import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Service} from '@angular/core';
import { Observable } from 'rxjs';
import { URL_BACKEND } from '../../../core/api/api.config';
import { EventModel } from './event-model';
import { PageResponse } from './pageReponse-model';


export interface EventFilter {
  keyword: string;
  category: string;
  organizer: string;
  city: string;
  startDate: string;
  endDate: string;
  status: string
}


@Service()
export class EventService {
    private readonly http = inject(HttpClient);

  getEvent(page: number, size: number = 20): Observable<PageResponse<EventModel>>{
    const params = new HttpParams().set('page', page).set('size', size);

    return this.http.get<PageResponse<EventModel>>(`${URL_BACKEND}/events`, { params });
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

    if(filters?.organizer) {
      params = params.set('organizerFirstName', filters.organizer);
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


    return this.http.get<PageResponse<EventModel>>(`${URL_BACKEND}/events/filter`, { params });
  }
}
