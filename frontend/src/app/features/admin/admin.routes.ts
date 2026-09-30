import { Routes } from "@angular/router";

export const ADMIN_ROUTES: Routes = [

  {
    path: '',
    loadComponent: () => import('./users/user-list/user-list').then((m) => m.UserList),
  },
]
