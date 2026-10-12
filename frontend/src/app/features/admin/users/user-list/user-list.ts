import { ConfirmService } from '../../../../core/ui/confirm.service';
import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { ACCOUNT_STATUS_LABELS, AccountStatus, AppUser, Role, ROLE_LABELS } from '../../../../core/models/user.models';
import { UserService } from '../user.service';
import { ActivatedRoute, RouterLink } from '@angular/router';

@Component({
  imports: [DatePipe, RouterLink],
  selector: 'app-user-list',
  styleUrl: './user-list.scss',
  templateUrl: './user-list.html',
})
export class UserList {
  private readonly confirmDialog = inject(ConfirmService);
  private readonly appUserService = inject(UserService);
  private readonly route = inject(ActivatedRoute);

  protected readonly users = signal<AppUser[]>([]);

  protected readonly totalElements = signal(0);
  protected readonly totalPages = signal(0);
  protected readonly page = signal(0);
  protected readonly search = signal('');
  protected readonly pageSize = 20;
  protected readonly roles = Object.keys(ROLE_LABELS) as Role[];
  protected readonly statuses = Object.keys(ACCOUNT_STATUS_LABELS) as AccountStatus[];
  protected readonly roleFilter = signal<Role | ''>('');
  // Pré-filtre possible depuis le tableau de bord : /admin/users?status=SUSPENDED
  protected readonly statusFilter = signal<AccountStatus | ''>(this.initialStatus());

  protected readonly roleLabels = ROLE_LABELS;

  protected readonly accountStatusLabels = ACCOUNT_STATUS_LABELS;

  protected readonly suspendingId = signal<number | null>(null);

  protected readonly errorMessage = signal<string | null>(null);

  constructor() {
    this.loadUsers();
  }

  private initialStatus(): AccountStatus | '' {
    const status = this.route.snapshot.queryParamMap.get('status');
    return status && status in ACCOUNT_STATUS_LABELS ? (status as AccountStatus) : '';
  }

  private handleError(err: HttpErrorResponse): void {
    this.errorMessage.set(typeof err.error === 'string' ? err.error : 'Une erreur est survenue.');
  }

  private loadUsers(): void {
    this.appUserService.getUsers(this.page(), this.pageSize, { q: this.search(), role: this.roleFilter() || undefined, status: this.statusFilter() || undefined }).subscribe({
      next: (result) => {
        this.users.set(result.content);
        this.totalElements.set(result.totalElements);
        this.totalPages.set(result.totalPages);
      },
      error: (err) => this.handleError(err),
    });
  }

  protected onRoleFilter(value: string): void {
    this.roleFilter.set(value as Role | '');
    this.page.set(0);
    this.loadUsers();
  }

  protected onStatusFilter(value: string): void {
    this.statusFilter.set(value as AccountStatus | '');
    this.page.set(0);
    this.loadUsers();
  }

  protected onSearch(value: string): void {
    this.search.set(value);
    this.page.set(0);
    this.loadUsers();
  }

  protected goToPage(page: number): void {
    this.page.set(page);
    this.loadUsers();
  }

  private deleteUser(id: number, deleteComments: boolean): void {
    this.appUserService.deleteUser(id, deleteComments).subscribe({
      next: () => {
        this.loadUsers();
        this.errorMessage.set(null);
      },
      error: async (err: HttpErrorResponse) => {
        const hasComments = err.status === 409 && err.headers.get('X-Error-Code') === 'ACCOUNT_HAS_COMMENTS';
        if (hasComments) {
          const confirmed = await this.confirmDialog.ask({
            title: 'Supprimer le compte et ses commentaires',
            message: 'Ce compte a posté des commentaires. Ils seront supprimés avec le compte.',
            confirmLabel: 'Tout supprimer',
            tone: 'danger',
          });
          if (confirmed) {
            this.deleteUser(id, true);
            return;
          }
        }
        this.handleError(err);
      },
  });
  }

  protected async onDelete(id: number): Promise<void> {
    const confirmed = await this.confirmDialog.ask({
      title: 'Supprimer cet utilisateur',
      message: 'Le compte sera définitivement supprimé.',
      confirmLabel: 'Supprimer',
      tone: 'danger',
    });
    if (!confirmed) {
      return;
    }
    this.deleteUser(id, false);
  }

  protected async onReactivate(id: number): Promise<void> {
    const confirmed = await this.confirmDialog.ask({
      title: 'Réhabiliter cet utilisateur',
      message: 'Le compte pourra de nouveau se connecter.',
      confirmLabel: 'Réhabiliter',
    });
    if (!confirmed) {
      return;
    }
    this.appUserService.reactivateUser(id).subscribe({
      next: (updated) => {
        this.users.update((list) => list.map((u) => (u.id === id ? updated : u)));
        this.errorMessage.set(null);
      },
      error: (err) => this.handleError(err),
    });
  }

  protected startSuspension(id: number): void {
    this.suspendingId.set(id);
  }

  protected async confirmSuspension(id: number, suspensionEndDate: string): Promise<void> {
    const confirmed = await this.confirmDialog.ask({
      title: 'Suspendre cet utilisateur',
      message: suspensionEndDate
        ? "Le compte sera suspendu jusqu'à la date indiquée."
        : 'Le compte sera suspendu sans date de fin.',
      confirmLabel: 'Suspendre',
      tone: 'danger',
    });
    if (!confirmed) {
      return;
    }
    this.appUserService.suspendUser(id, suspensionEndDate || undefined).subscribe({
      next: (updated) => {
        this.users.update((list) => list.map((u) => (u.id === id ? updated : u)));
        this.suspendingId.set(null);
        this.errorMessage.set(null);
      },
      error: (err) => this.handleError(err),
    });
  }
}
