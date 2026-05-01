import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';

import { User, UserCreateRequest, UserResponse, UserRole, UserUpdateRequest } from '../../services/user';

type UserFormModel = {
  userName: string;
  role: UserRole;
  password: string;
};

@Component({
  selector: 'app-user-management',
  imports: [CommonModule, FormsModule],
  templateUrl: './user-management.html',
  styleUrl: './user-management.css',
})
export class UserManagement {
  private readonly userService = inject(User);
  private readonly changeDetectorRef = inject(ChangeDetectorRef);

  readonly roles: UserRole[] = ['ADMIN', 'USER'];

  users: UserResponse[] = [];
  selectedUserId: number | null = null;
  form: UserFormModel = this.createEmptyForm();

  isLoading = false;
  isSaving = false;
  isDeleting = false;
  errorMessage = '';
  successMessage = '';

  constructor() {
    this.loadUsers();
  }

  get isEditing(): boolean {
    return this.selectedUserId !== null;
  }

  loadUsers(): void {
    this.isLoading = true;
    this.errorMessage = '';

    this.userService
      .findAll()
      .pipe(
        finalize(() => {
          this.isLoading = false;
          this.changeDetectorRef.detectChanges();
        })
      )
      .subscribe({
        next: (users) => {
          this.users = users;
        },
        error: (error) => {
          this.errorMessage = this.extractErrorMessage(
            error,
            'Nao foi possivel carregar os usuarios.'
          );
        },
      });
  }

  selectUser(user: UserResponse): void {
    this.selectedUserId = user.id;
    this.form = {
      userName: user.userName,
      role: user.role,
      password: '',
    };
    this.errorMessage = '';
    this.successMessage = '';
  }

  startCreate(): void {
    this.selectedUserId = null;
    this.form = this.createEmptyForm();
    this.errorMessage = '';
    this.successMessage = '';
  }

  save(): void {
    if (this.isSaving) {
      return;
    }

    const userName = this.form.userName.trim();
    const password = this.form.password.trim();

    if (!userName) {
      this.errorMessage = 'Informe o nome do usuario.';
      return;
    }

    if (!this.isEditing && !password) {
      this.errorMessage = 'Informe uma senha para criar o usuario.';
      return;
    }

    this.isSaving = true;
    this.errorMessage = '';
    this.successMessage = '';

    const request = this.buildRequest(userName, password);
    const operation = this.isEditing && this.selectedUserId !== null
      ? this.userService.update(this.selectedUserId, request as UserUpdateRequest)
      : this.userService.create(request as UserCreateRequest);

    operation
      .pipe(
        finalize(() => {
          this.isSaving = false;
          this.changeDetectorRef.detectChanges();
        })
      )
      .subscribe({
        next: (savedUser) => {
          this.successMessage = this.isEditing
            ? 'Usuario atualizado com sucesso.'
            : 'Usuario criado com sucesso.';
          this.selectedUserId = savedUser.id;
          this.form = {
            userName: savedUser.userName,
            role: savedUser.role,
            password: '',
          };
          this.loadUsers();
        },
        error: (error) => {
          this.errorMessage = this.extractErrorMessage(
            error,
            'Nao foi possivel salvar o usuario.'
          );
        },
      });
  }

  delete(user: UserResponse): void {
    if (this.isDeleting) {
      return;
    }

    const confirmed = window.confirm(`Deseja realmente excluir o usuario "${user.userName}"?`);

    if (!confirmed) {
      return;
    }

    this.isDeleting = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.userService
      .delete(user.id)
      .pipe(
        finalize(() => {
          this.isDeleting = false;
          this.changeDetectorRef.detectChanges();
        })
      )
      .subscribe({
        next: () => {
          this.successMessage = 'Usuario removido com sucesso.';

          if (this.selectedUserId === user.id) {
            this.startCreate();
          }

          this.loadUsers();
        },
        error: (error) => {
          this.errorMessage = this.extractErrorMessage(
            error,
            'Nao foi possivel excluir o usuario.'
          );
        },
      });
  }

  trackByUserId(_: number, user: UserResponse): number {
    return user.id;
  }

  private buildRequest(userName: string, password: string): UserCreateRequest | UserUpdateRequest {
    if (!this.isEditing) {
      return {
        userName,
        role: this.form.role,
        password,
      };
    }

    return {
      userName,
      role: this.form.role,
      ...(password ? { password } : {}),
    };
  }

  private createEmptyForm(): UserFormModel {
    return {
      userName: '',
      role: 'USER',
      password: '',
    };
  }

  private extractErrorMessage(error: any, fallback: string): string {
    const backendMessages = error?.error?.messages;

    if (Array.isArray(backendMessages) && backendMessages.length) {
      return backendMessages[0];
    }

    return fallback;
  }
}
