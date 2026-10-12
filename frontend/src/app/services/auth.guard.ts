import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

// Only for navigation. The backend checks the token on every request anyway.
export const authGuard: CanActivateFn = (route, state) => {
  if (inject(AuthService).user()) {
    return true;
  }
  return inject(Router).createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
};
