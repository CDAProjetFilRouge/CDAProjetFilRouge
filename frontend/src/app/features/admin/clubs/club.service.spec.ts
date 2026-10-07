import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ClubService } from './club.service';
import { URL_BACKEND } from '../../../core/api/api.config';

describe('ClubService', () => {
  let service: ClubService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ClubService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('getClubs appelle /clubs/admin avec les filtres et la pagination', () => {
    service.getClubs(1, 10, 'CULTURE', 'jazz', 'Toulouse').subscribe();

    const req = httpMock.expectOne((r) => r.url === `${URL_BACKEND}/clubs/admin`);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('name')).toBe('jazz');
    expect(req.request.params.get('category')).toBe('CULTURE');
    expect(req.request.params.get('city')).toBe('Toulouse');
    expect(req.request.params.get('page')).toBe('1');
    expect(req.request.params.get('size')).toBe('10');
    req.flush({ content: [], totalElements: 0, totalPages: 0, page: 1, size: 10 });
  });

  it('getClubs n envoie pas les filtres vides', () => {
    service.getClubs(0, 20).subscribe();

    const req = httpMock.expectOne((r) => r.url === `${URL_BACKEND}/clubs/admin`);
    expect(req.request.params.has('name')).toBe(false);
    expect(req.request.params.has('category')).toBe(false);
    expect(req.request.params.has('city')).toBe(false);
    req.flush({ content: [], totalElements: 0, totalPages: 0, page: 0, size: 20 });
  });

  it('deleteClub appelle DELETE /clubs/{id}', () => {
    service.deleteClub(5).subscribe();

    const req = httpMock.expectOne(`${URL_BACKEND}/clubs/5`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});
