import { HttpInterceptorFn } from '@angular/common/http';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = localStorage.getItem('jwt');

  const isPublic =
    req.url.endsWith('/auth/login') ||
    req.url.endsWith('/auth/register') ||
    req.url.endsWith('/actuator/health') ||
    (req.method === 'GET' &&
      (req.url.endsWith('/products') ||
        req.url.endsWith('/products/search') ||
        req.url.endsWith('/products/facets') ||
        /^.*\/products\/[^/]+$/.test(req.url)));

  const request =
    !token || isPublic ? req : req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });

  return next(request);
};
