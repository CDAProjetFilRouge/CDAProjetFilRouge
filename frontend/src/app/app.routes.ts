import { Routes } from '@angular/router';
import { Event } from './features/events/event/event';
import { authGuard } from './core/auth/auth-guard';
import { LoginComponent } from './features/auth/login/login/login.component';

export const routes: Routes = [
  {
    path: 'login',
    component: LoginComponent,
  },
  {
    path: 'admin',
    canActivate: [authGuard],
    loadChildren: () => import('./features/admin/admin.routes').then((m) => m.ADMIN_ROUTES),
  },
  {
    path: 'account',
    canActivate: [authGuard],
    loadComponent: () => import('./features/account/account').then((m) => m.Account),
  },
  {
    path: 'account/edit',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/account/profile-edit/profile-edit').then((m) => m.ProfileEdit),
  },
  {
    path: '',
    component: Event
  }
];
