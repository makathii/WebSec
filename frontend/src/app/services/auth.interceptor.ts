import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';
import { API_URL } from '../config';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  // the token only goes to our own API, and the session endpoints use the cookie instead
  if (!req.url.startsWith(`${API_URL}/api/`) || req.url.startsWith(`${API_URL}/api/session`)) {
    return next(req);
  }

  const hadToken = auth.token !== null;
  return next(withToken(req, auth.token)).pipe(
    catchError((err) => {
      if (!(err instanceof HttpErrorResponse) || err.status !== 401 || !hadToken) {
        return throwError(() => err);
      }
      // access token expired, get a new one and try once more
      return auth.refresh().pipe(
        catchError(() => {
          router.navigate(['/login'], { queryParams: { reason: 'expired', returnUrl: router.url } });
          return throwError(() => err);
        }),
        switchMap((token) => next(withToken(req, token))),
      );
    }),
  );
};

function withToken(req: HttpRequest<unknown>, token: string | null) {
  return token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;
}
