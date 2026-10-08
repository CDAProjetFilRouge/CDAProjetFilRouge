import { Component, inject, signal } from '@angular/core';
import { ClubService } from '../club.service';
import { Club, CLUB_CATEGORY_LABELS, ClubCategory } from '../club.models';
import { HttpErrorResponse } from '@angular/common/http';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';

@Component({
  imports: [DatePipe, RouterLink],
  selector: 'app-club-list',
  styleUrl: './club-list.scss',
  templateUrl: './club-list.html',
})
export class ClubList {

  private readonly clubService = inject(ClubService);

  protected readonly clubs = signal<Club[]>([]);
  protected readonly totalElements = signal(0);
  protected readonly totalPages = signal(0);
  protected readonly page = signal(0);
  protected readonly search = signal('');
  protected readonly pageSize = 20;
  protected readonly categories = Object.keys(CLUB_CATEGORY_LABELS) as ClubCategory[];
  protected readonly categoryLabels = CLUB_CATEGORY_LABELS;
  protected readonly categoryFilter = signal<ClubCategory | ''>('');
  protected readonly loading = signal<boolean>(false);
  protected readonly errorMessage = signal<string | null>(null);


  constructor() {
    this.loadClubs();
  }

  private handleError(err: HttpErrorResponse): void {
    this.errorMessage.set(typeof err.error === 'string' ? err.error : 'Une erreur est survenue.');
  }

  private loadClubs(): void {
    this.loading.set(true);
    this.clubService.getClubs(this.page(), this.pageSize, this.categoryFilter() || undefined, this.search()).subscribe({
      next: (result) => {
        this.clubs.set(result.content);
        this.totalElements.set(result.totalElements);
        this.totalPages.set(result.totalPages);
        this.loading.set(false);
      },
      error: (err) => {
        this.handleError(err);
        this.loading.set(false);
      }
    });
  }

  protected onSearch(value: string): void {
    this.search.set(value);
    this.page.set(0);
    this.loadClubs();
  }

  private deleteClub(id: number): void {
    this.clubService.deleteClub(id).subscribe({
      next: () => {
        this.errorMessage.set(null);
        this.loadClubs();
      },
      error: (err) => this.handleError(err),
    });
  }

  protected onDelete(id: number): void {
    if (!confirm('Voulez-vous vraiment désaffilier ce club ?')) {
      return;
    }
    this.deleteClub(id);
  }

  protected onCategoryFilter(value: string): void {
    this.categoryFilter.set(value as ClubCategory | '');
    this.page.set(0);
    this.loadClubs();
  }

  protected goToPage(page: number): void {
    this.page.set(page);
    this.loadClubs();
  }
}
