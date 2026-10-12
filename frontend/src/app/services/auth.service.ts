import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, catchError, finalize, firstValueFrom, map, shareReplay, tap, throwError } from 'rxjs';
import { API_URL } from '../config';
import { Session, User } from '../models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);

  // Access token lives only in memory. The refresh token is an HttpOnly cookie that JS can't read.
  private accessToken: string | null = null;
  private refreshing: Observable<string> | null = null;

  readonly user = signal<User | null>(null);

  get token(): string | null {
    return this.accessToken;
  }

  signup(email: string, username: string, password: string) {
    return this.http.post<User>(`${API_URL}/api/users`, { email, username, password });
  }

  login(login: string, password: string) {
    return this.http
      .post<Session>(`${API_URL}/api/session`, { login, password }, { withCredentials: true })
      .pipe(tap((session) => this.setSession(session)));
  }

  logout() {
    return this.http
      .delete<void>(`${API_URL}/api/session`, { withCredentials: true })
      .pipe(finalize(() => this.clear()));
  }

  // withCredentials so the browser accepts the Set-Cookie that clears the refresh cookie
  changePassword(currentPassword: string, newPassword: string) {
    return this.http
      .put<void>(`${API_URL}/api/users/me/password`, { currentPassword, newPassword }, { withCredentials: true })
      .pipe(tap(() => this.clear()));
  }

  deleteAccount(password: string) {
    return this.http
      .delete<void>(`${API_URL}/api/users/me`, { body: { password }, withCredentials: true })
      .pipe(tap(() => this.clear()));
  }

  // Several requests can fail with 401 at the same time, they all share one refresh call.
  refresh(): Observable<string> {
    if (!this.refreshing) {
      this.refreshing = this.http
        .post<Session>(`${API_URL}/api/session/tokens`, null, { withCredentials: true })
        .pipe(
          tap((session) => this.setSession(session)),
          map((session) => session.accessToken),
          catchError((err) => {
            this.clear();
            return throwError(() => err);
          }),
          finalize(() => (this.refreshing = null)),
          shareReplay(1),
        );
    }
    return this.refreshing;
  }

  // called once on startup to get a session back after a page reload
  restore(): Promise<unknown> {
    return firstValueFrom(this.refresh()).catch(() => null);
  }

  private setSession(session: Session) {
    this.accessToken = session.accessToken;
    this.user.set(session.user);
  }

  private clear() {
    this.accessToken = null;
    this.user.set(null);
  }
}
