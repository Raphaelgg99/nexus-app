import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';

import { Auth } from '../services/auth';
import { isBackendRequestUrl } from '../services/api-url';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const authService = inject(Auth);
  const authorizationHeader = authService.getAuthorizationHeader();

  if (!authorizationHeader || !isBackendRequestUrl(request.url)) {
    return next(request);
  }

  return next(
    request.clone({
      setHeaders: {
        Authorization: authorizationHeader,
      },
    })
  );
};
