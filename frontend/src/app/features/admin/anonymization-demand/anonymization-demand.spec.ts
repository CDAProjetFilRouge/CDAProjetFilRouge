import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AnonymizationDemand } from './anonymization-demand';

describe('AnonymizationDemand', () => {
  let component: AnonymizationDemand;
  let fixture: ComponentFixture<AnonymizationDemand>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AnonymizationDemand],
    }).compileComponents();

    fixture = TestBed.createComponent(AnonymizationDemand);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
