import { Component, computed, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, ɵInternalFormsSharedModule } from '@angular/forms';
import { ClubModel } from './club-model';
import { ClubFilter, ClubService } from './club-service';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  imports: [ReactiveFormsModule],
  selector: 'app-club',
  styleUrl: './club.scss',
  templateUrl: './club.html',
})
export class Club {
  private readonly clubService = inject(ClubService);

  protected readonly clubs = signal<ClubModel[]>([]);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly sortOrder = signal<'asc' | 'desc'>('desc');

  currentPage = 1;
  clubPerPage = 20;
  totalElements = 0;
  totalPages = 0;

  filters = new FormGroup({
    name: new FormControl('', { nonNullable: true}),
    category: new FormControl('', { nonNullable: true})
  })

  private handleError(err: HttpErrorResponse): void {
    this.errorMessage.set(typeof err.error === "string" ? err.error : "Une erreur est survenue.");
  }

  ngOnInit(): void {
    this.loadClubs();
  }

  loadClubs(): void {
    const filters: ClubFilter = this.filters.getRawValue();
    console.log(this.filters);
    this.clubService.getClub(this.currentPage - 1, this.clubPerPage, filters).subscribe({
      next: (page) => {
        this.clubs.set(page.content);
        this.totalElements = page.totalElements;
        this.totalPages = page.totalPages;
      },
      error: (err) => this.handleError(err)
    });
  }

  resetFilter(): void {
    this.filters.reset({
      name: '',
      category: ''
    });

    this.loadClubs();
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages) {
      this.currentPage++;
      this.loadClubs();
    }
  }

  previousPage(): void {
    if (this.currentPage > 1){
      this.currentPage--;
      this.loadClubs();
    }
  }

  toggleSort(){
    this.sortOrder.update(order => order === 'desc' ? 'asc' : 'desc');
  }

  sortedItems = computed(() => {
    return [...this.clubs()].sort((a, b) => {
      const result = a.name.localeCompare(b.name, 'fr', { sensitivity: 'base'});

      return this.sortOrder() === 'desc' ? result : -result;
    });
  });
}
