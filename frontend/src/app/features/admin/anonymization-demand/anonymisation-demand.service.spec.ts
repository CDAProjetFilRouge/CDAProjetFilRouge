import { TestBed } from '@angular/core/testing';
import { AnonymisationDemandService } from '../../../frontend/src/app/features/admin/anonymization-demand/anonymisation-demand.service';

describe('AnonymisationDemandService', () => {
  let service: AnonymisationDemandService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(AnonymisationDemandService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
