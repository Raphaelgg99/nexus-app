import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { Auth } from '../../services/auth';

@Component({
  selector: 'app-login',
  imports: [CommonModule, FormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {
  private readonly auth = inject(Auth);
  private readonly router = inject(Router);
  private readonly changeDetectorRef = inject(ChangeDetectorRef);

  userName = '';
  password = '';
  isSubmitting = false;
  errorMessage = '';

  async submit(event?: Event): Promise<void> {
    event?.preventDefault();

    if (this.isSubmitting) {
      return;
    }

    const userName = this.userName.trim();
    const password = this.password;

    if (!userName || !password) {
      this.errorMessage = 'Preencha usuario e senha para entrar.';
      return;
    }

    this.isSubmitting = true;
    this.errorMessage = '';
    this.changeDetectorRef.detectChanges();

    try {
      await firstValueFrom(this.auth.login({ userName, password }));
      await this.router.navigateByUrl('/dashboard');
    } catch (error: any) {
      const backendMessages = error?.error?.messages;

      if (Array.isArray(backendMessages) && backendMessages.length) {
        this.errorMessage = backendMessages[0];
      } else if (typeof error?.message === 'string' && error.message.trim()) {
        this.errorMessage = error.message;
      } else if (error?.status === 401) {
        this.errorMessage = 'Usuario ou senha invalidos.';
      } else if (error?.status === 0) {
        this.errorMessage = 'Nao foi possivel conectar ao servidor.';
      } else {
        this.errorMessage = 'Nao foi possivel autenticar. Verifique suas credenciais.';
      }
    } finally {
      this.isSubmitting = false;
      this.changeDetectorRef.detectChanges();
    }
  }
}
