import { Component, inject, signal } from '@angular/core';
import { UserService } from '../user.service';
import { ACCOUNT_STATUS_LABELS, AppUser, ROLE_LABELS } from '../user.models';
import { DatePipe } from '@angular/common';

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

  constructor() {
    this.appUserService.getUsers().subscribe(list => this.users.set(list));
  }

  protected onDelete(id: number): void {
    if (!confirm('Supprimer cet utilisateur ?')) {
      return;
    }
    this.appUserService.deleteUser(id).subscribe(() => {
      this.users.update(list => list.filter(u => u.id !== id));
    });
  }

  protected onReactivate(id: number): void {
    if (!confirm('Voulez-vous vraiment réhabiliter cet utilisateur ?')) {
      return;
    }
    this.appUserService.reactivateUser(id).subscribe(updated => {
      this.users.update(list => list.map(u => u.id === id ? updated : u))
    });
  }

  protected startSuspension(id: number): void {
    this.suspendingId.set(id);
  }

  protected confirmSuspension(id: number, suspensionEndDate: string): void {
    this.appUserService.suspendUser(id, suspensionEndDate || undefined).subscribe(updated => {
      this.users.update(list => list.map(u => u.id === id ? updated : u));
      this.suspendingId.set(null);
    });
  }
}

