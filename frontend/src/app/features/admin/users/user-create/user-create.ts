import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AdminUserCreate, Role, ROLE_LABELS } from '../../../../core/models/user.models';
import { UserService } from '../user.service';

@Component({
  imports: [ReactiveFormsModule, RouterLink],
  selector: 'app-user-create',
  styleUrl: './user-create.scss',
  templateUrl: './user-create.html',
})
export class UserCreate {
  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly userService = inject(UserService);
  private readonly router = inject(Router);
  protected readonly ROLE_LABELS = ROLE_LABELS;
  protected readonly roles = Object.keys(ROLE_LABELS) as Role[];

  readonly form = this.formBuilder.group({
    firstName: ['', Validators.required],
    lastName: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    role: ['MEMBER' as Role],
    phone: ['', Validators.required],
  });

  readonly errorMessage = signal('');
  readonly isSubmitting = signal(false);

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();

    const newUser: AdminUserCreate = {
      firstName: value.firstName,
      lastName: value.lastName,
      email: value.email,
      phone: value.phone,
      role: value.role,
    };

    this.isSubmitting.set(true);
    this.errorMessage.set('');

    this.userService.createUserByAdmin(newUser).subscribe({
      next: () => this.router.navigateByUrl('/admin/users'),
      error: (error: HttpErrorResponse) => {
        this.isSubmitting.set(false);
        this.errorMessage.set(
          typeof error.error === 'string'
            ? error.error
            : "Impossible de créer un compte.",
        );
      },
    });
  }
}
