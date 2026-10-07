import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ClubForm } from './club-form';

describe('ClubForm', () => {
  let component: ClubForm;
  let fixture: ComponentFixture<ClubForm>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ClubForm],
    }).compileComponents();

    fixture = TestBed.createComponent(ClubForm);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
