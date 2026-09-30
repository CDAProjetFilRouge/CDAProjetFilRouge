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
}

