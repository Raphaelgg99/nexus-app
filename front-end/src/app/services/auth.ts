import { HttpClient } from '@angular/common/http';
import { computed, Injectable, signal } from '@angular/core';
import { catchError, Observable, tap, throwError, timeout } from 'rxjs';

import { resolveApiUrl } from './api-url';

export interface AuthUser {
  id: number;
  userName: string;
  role: 'ADMIN' | 'USER';
}

export interface AuthResponse {
  tokenType: string;
  accessToken: string;
  expiresIn: number;
  user: AuthUser;
}

export interface LoginRequest {
  userName: string;
  password: string;
}

interface StoredAuthSession {
  tokenType: string;
  accessToken: string;
  expiresIn: number;
  createdAt: number;
  user: AuthUser;
}

@Injectable({
  providedIn: 'root',
})
export class Auth {
  private readonly storageKey = 'ramark.auth.session';
  private readonly loginEndpoint = resolveApiUrl('/api/v1/auth/login');
  private readonly sessionState = signal<StoredAuthSession | null>(this.readSessionFromStorage());

  readonly isLoggedIn = computed(() => !!this.getValidSession());
  readonly currentUser = computed(() => this.getValidSession()?.user ?? null);
  readonly isAdmin = computed(() => this.currentUser()?.role === 'ADMIN');

  constructor(private readonly http: HttpClient) {}

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(this.loginEndpoint, request).pipe(
      timeout(10000),
      tap((response) => this.saveSession(response)),
      catchError((error) => {
        if (error?.name === 'TimeoutError') {
          return throwError(
            () => new Error('A autenticacao demorou demais para responder.')
          );
        }

        return throwError(() => error);
      })
    );
  }

  logout(): void {
    localStorage.removeItem(this.storageKey);
    this.sessionState.set(null);
  }

  isAuthenticated(): boolean {
    return !!this.getValidSession();
  }

  getAuthorizationHeader(): string | null {
    const session = this.getValidSession();

    if (!session) {
      return null;
    }

    return `${session.tokenType} ${session.accessToken}`;
  }

  getCurrentUser(): AuthUser | null {
    return this.getValidSession()?.user ?? null;
  }

  userIsAdmin(): boolean {
    return this.getCurrentUser()?.role === 'ADMIN';
  }

  saveSession(response: AuthResponse): void {
    const session: StoredAuthSession = {
      tokenType: response.tokenType || 'Bearer',
      accessToken: response.accessToken,
      expiresIn: response.expiresIn,
      createdAt: Date.now(),
      user: response.user,
    };

    localStorage.setItem(this.storageKey, JSON.stringify(session));
    this.sessionState.set(session);
  }

  private getValidSession(): StoredAuthSession | null {
    const session = this.sessionState();

    if (!session) {
      return null;
    }

    if (this.isExpired(session)) {
      this.logout();
      return null;
    }

    return session;
  }

  private readSessionFromStorage(): StoredAuthSession | null {
    const rawValue = localStorage.getItem(this.storageKey);

    if (!rawValue) {
      return null;
    }

    try {
      return JSON.parse(rawValue) as StoredAuthSession;
    } catch {
      this.logout();
      return null;
    }
  }

  private isExpired(session: StoredAuthSession): boolean {
    return Date.now() >= session.createdAt + session.expiresIn;
  }
}
