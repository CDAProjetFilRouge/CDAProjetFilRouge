import { TestBed } from '@angular/core/testing';
import { LegalDocumentService } from './legal.document.service';

describe('LegalDocumentService', () => {
  let service: LegalDocumentService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(LegalDocumentService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
