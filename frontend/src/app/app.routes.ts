import { Routes } from '@angular/router';
import { Event } from './features/events/event/event';
import { LoginComponent } from './features/auth/login/login/login.component';

export const routes: Routes = [
  {
    path: 'admin',
    loadChildren: () => import('./features/admin/admin.routes').then((m) => m.ADMIN_ROUTES),
  },
  {
    path: 'login',
    component: LoginComponent,
  },
  {
    path: '',
    component: Event
  },
];
