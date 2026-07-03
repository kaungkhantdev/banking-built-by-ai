import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { ToastService } from '../services/toast.service';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const toast = inject(ToastService);

  return next(req).pipe(
    catchError(err => {
      if (err.status === 401) {
        auth.logout();
      } else if (err.status === 403) {
        toast.error('Permission denied.');
      } else if (err.status >= 500) {
        toast.error('Server error — please try again.');
      } else if (err.error?.message) {
        toast.error(err.error.message);
      }
      return throwError(() => err);
    })
  );
};
