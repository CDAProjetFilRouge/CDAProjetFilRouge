import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth-guard';
import { LoginComponent } from './features/auth/login/login/login.component';
import { Event } from './features/events/event/event';
import { Club } from './features/clubs/club/club';
import { adminGuard } from './core/auth/admin-guard';

export const routes: Routes = [
  {
    path: 'login',
    component: LoginComponent,
  },
  {
    path: 'admin',
    canActivate: [authGuard, adminGuard],
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
    path: 'verify',
    loadComponent: () =>
      import('./features/auth/verify-account/verify-account').then((m) => m.VerifyAccount),
  },
  {
    path: 'clubs',
    component: Club,
  },
  {
    path: 'forgot-password',
    loadComponent: () =>
      import('./features/auth/forgot-password/forgot-password').then((m) => m.ForgotPassword),
  },
  {
    path: 'reset-password',
    loadComponent: () =>
      import('./features/auth/reset-password/reset-password').then((m) => m.ResetPassword),
  },
  {
    path: 'activate-account',
    loadComponent: () =>
      import('./features/auth/activate-account/activate-account').then((m) => m.ActivateAccount),
  },
      {
    path: '',
    component: Event,
  },
];
