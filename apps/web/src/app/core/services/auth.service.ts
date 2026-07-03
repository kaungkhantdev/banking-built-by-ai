import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { tap } from 'rxjs';
import { TokenPair, UserView } from '../models/api.models';

const ACCESS_KEY = 'bc_access';
const REFRESH_KEY = 'bc_refresh';

function decodePerms(token: string | null): Set<string> {
  if (!token) return new Set();
  try {
    const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
    return new Set<string>(Array.isArray(payload['perms']) ? payload['perms'] : []);
  } catch {
    return new Set();
  }
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly _accessToken = signal<string | null>(
    localStorage.getItem(ACCESS_KEY)
  );
  private readonly _refreshToken = signal<string | null>(
    localStorage.getItem(REFRESH_KEY)
  );

  readonly accessToken = this._accessToken.asReadonly();
  readonly isAuthenticated = computed(() => !!this._accessToken());
  readonly permissions = computed(() => decodePerms(this._accessToken()));

  register(email: string, password: string) {
    return this.http.post<UserView>('/v1/auth/register', { email, password });
  }

  login(email: string, password: string) {
    return this.http
      .post<TokenPair>('/v1/auth/login', { email, password })
      .pipe(tap(tokens => this.persist(tokens)));
  }

  refresh() {
    return this.http
      .post<TokenPair>('/v1/auth/refresh', { refreshToken: this._refreshToken() })
      .pipe(tap(tokens => this.persist(tokens)));
  }

  logout(): void {
    localStorage.removeItem(ACCESS_KEY);
    localStorage.removeItem(REFRESH_KEY);
    this._accessToken.set(null);
    this._refreshToken.set(null);
    this.router.navigate(['/login']);
  }

  private persist(tokens: TokenPair): void {
    localStorage.setItem(ACCESS_KEY, tokens.accessToken);
    localStorage.setItem(REFRESH_KEY, tokens.refreshToken);
    this._accessToken.set(tokens.accessToken);
    this._refreshToken.set(tokens.refreshToken);
  }
}
