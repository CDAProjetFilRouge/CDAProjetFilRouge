import { TestBed } from '@angular/core/testing';
import { AnonymizationDemandService } from './anonymization-demand.service';

describe('AnonymizationDemandService', () => {
  let service: AnonymizationDemandService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(AnonymizationDemandService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
