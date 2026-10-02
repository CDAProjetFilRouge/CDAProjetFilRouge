import { Component, inject, signal } from '@angular/core';
import { UserService } from '../user.service';
import { AppUser } from '../user.models';

@Component({
  imports: [],
  selector: 'app-user-list',
  styleUrl: './user-list.scss',
  templateUrl: './user-list.html',
})
export class UserList {

  private readonly appUserService = inject(UserService);

  protected readonly users = signal<AppUser[]>([]);

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

  protected onSuspend(id: number, endDate?: Date): void {
    if (!confirm('Voulez-vous vraiment suspendre cet utilisateur ?')) {
      return;
    }
    this.appUserService.suspendUser(id, endDate).subscribe(updated => {
      this.users.update(list => list.map(u => u.id === id ? updated : u))
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
}

