import { HttpInterceptorFn } from '@angular/common/http';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = localStorage.getItem('jwt');

  const isPublic =
    req.url.endsWith('/auth/login') ||
    req.url.endsWith('/auth/register') ||
    req.url.endsWith('/actuator/health') ||
    (
      req.method === 'GET' &&
      /^.*\/products(\/[^/]+)?$/.test(req.url)
    );

  if (!token || isPublic) {
    return next(req);
  }

  return next(
    req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`,
      },
    })
  );
};