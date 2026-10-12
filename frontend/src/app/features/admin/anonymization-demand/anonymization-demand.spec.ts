import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AnonymizationDemandList } from './anonymization-demand';

describe('AnonymizationDemandList', () => {
  let component: AnonymizationDemandList;
  let fixture: ComponentFixture<AnonymizationDemandList>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AnonymizationDemandList],
    }).compileComponents();

    fixture = TestBed.createComponent(AnonymizationDemandList);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
