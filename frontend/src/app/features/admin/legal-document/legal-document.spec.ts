import { ComponentFixture, TestBed } from '@angular/core/testing';
import { LegalDocumentPage } from './legal-document';

describe('LegalDocument', () => {
  let component: LegalDocumentPage;
  let fixture: ComponentFixture<LegalDocumentPage>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LegalDocumentPage],
    }).compileComponents();

    fixture = TestBed.createComponent(LegalDocumentPage);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
