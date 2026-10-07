import { Routes } from "@angular/router";

export const ADMIN_ROUTES: Routes = [

  {
    path: '',
    loadComponent: () => import('./admin-layout/admin-layout').then((m) => m.AdminLayout),
    children: [
      {
        path: '',
        redirectTo: 'users', pathMatch: 'full'
      },

      {
        path: 'users',
        loadComponent: () => import('./users/user-list/user-list').then((m) => m.UserList),
      },

      {
        path: 'terms-of-use',
        loadComponent: () => import('./legal-document/legal-document').then((m) => m.LegalDocumentPage),
        data: { documentType: 'TERM_OF_USE' }
      },

      {
        path: 'gdpr-policy',
        loadComponent: () => import('./legal-document/legal-document').then((m) => m.LegalDocumentPage),
        data: { documentType: 'GDPR_POLICY' }
      },

      {
        path: 'anonymisation-demands',
        loadComponent: () => import('./anonymization-demand/anonymization-demand').then((m) => m.AnonymizationDemandList),
      },

      {
        path: 'users/:id/edit',
        loadComponent: () => import('./users/user-edit/user-edit').then((m) => m.UserEdit),
      },

      {
        path: 'users/new',
        loadComponent: () => import('./users/user-create/user-create').then((m) => m.UserCreate),
      },
    ]
  },

]
