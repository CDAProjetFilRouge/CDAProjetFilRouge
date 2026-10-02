import { Component, inject, signal } from '@angular/core';
import { UserService } from '../user.service';
import { ACCOUNT_STATUS_LABELS, AppUser, ROLE_LABELS } from '../user.models';
import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  imports: [DatePipe],
  selector: 'app-user-list',
  styleUrl: './user-list.scss',
  templateUrl: './user-list.html',
})
export class UserList {

  private readonly appUserService = inject(UserService);

  protected readonly users = signal<AppUser[]>([]);

  protected readonly roleLabels = ROLE_LABELS;

  protected readonly accountStatusLabels = ACCOUNT_STATUS_LABELS;

  protected readonly suspendingId = signal<number | null>(null);

  protected readonly errorMessage = signal<string | null>(null);

  constructor() {
    this.appUserService.getUsers().subscribe({
      next: list => this.users.set(list),
      error: err => this.handleError(err),
    });
  }

  private handleError(err: HttpErrorResponse): void {
    this.errorMessage.set(typeof err.error === 'string' ? err.error : 'Une erreur est survenue.');
  }

  protected onDelete(id: number): void {
    if (!confirm('Supprimer cet utilisateur ?')) {
      return;
    }
    this.appUserService.deleteUser(id).subscribe({
      next: () => {
        this.users.update(list => list.filter(u => u.id !== id));
        this.errorMessage.set(null);
      },
      error: err => this.handleError(err),
    });
  }

  protected onReactivate(id: number): void {
    if (!confirm('Voulez-vous vraiment réhabiliter cet utilisateur ?')) {
      return;
    }
    this.appUserService.reactivateUser(id).subscribe({
      next: updated => {
        this.users.update(list => list.map(u => u.id === id ? updated : u));
        this.errorMessage.set(null);
      },
      error: err => this.handleError(err),
    });
  }

  protected startSuspension(id: number): void {
    this.suspendingId.set(id);
  }

  protected confirmSuspension(id: number, suspensionEndDate: string): void {
    this.appUserService.suspendUser(id, suspensionEndDate || undefined).subscribe({
      next: updated => {
        this.users.update(list => list.map(u => u.id === id ? updated : u));
        this.suspendingId.set(null);
        this.errorMessage.set(null);
      },
      error: err => this.handleError(err),
    });
  }
}

