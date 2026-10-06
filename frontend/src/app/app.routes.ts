import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth-guard';
import { LoginComponent } from './features/auth/login/login/login.component';
import { Event } from './features/events/event/event';

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
    path: 'confirm-password-change',
    loadComponent: () =>
      import('./features/auth/confirm-password-change/confirm-password-change').then(
        (m) => m.ConfirmPasswordChange,
      ),
  },
  {
    path: 'account/password',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/account/password-change/password-change').then((m) => m.PasswordChange),
  },
  {
    path: 'createAccount',
    loadComponent: () =>
      import('./features/auth/create-account/create-account').then((m) => m.CreateAccount),
  },
  {
    path: '',
    component: Event,
  },
  {
    path: 'verify',
    loadComponent: () =>
      import('./features/auth/verify-account/verify-account').then((m) => m.VerifyAccount),
  },
];
