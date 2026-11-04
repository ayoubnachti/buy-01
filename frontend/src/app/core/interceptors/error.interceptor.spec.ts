import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { vi } from 'vitest';

import { errorInterceptor } from './error.interceptor';
import { AuthService } from '../services/auth.service';
import { ToastService } from '../services/toast.service';

describe('errorInterceptor', () => {
  let http: HttpClient;
  let backend: HttpTestingController;
  let authService: { logout: ReturnType<typeof vi.fn> };
  let toast: { info: ReturnType<typeof vi.fn>; error: ReturnType<typeof vi.fn> };
  let router: { url: string; navigate: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    authService = { logout: vi.fn() };
    toast = { info: vi.fn(), error: vi.fn() };
    router = { url: '/cart', navigate: vi.fn() };

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: authService },
        { provide: ToastService, useValue: toast },
        { provide: Router, useValue: router },
      ],
    });

    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  /** Sends a request, makes the fake backend answer with `status`, returns what the caller received. */
  function failWith(url: string, status: number): { status?: number } {
    let received: { status?: number } = {};
    http.get(url).subscribe({ error: (e) => (received = e) });
    const request = backend.expectOne(url);
    if (status === 0) {
      request.error(new ProgressEvent('error'));          // network failure: Angular reports status 0
    } else {
      request.flush({ success: false, message: 'backend says no' }, { status, statusText: 'x' });
    }
    return received;
  }

  it('401 on a normal call: logs out, says why, and goes to login with returnUrl', () => {
    failWith('/cart', 401);

    expect(authService.logout).toHaveBeenCalled();
    expect(toast.info).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/login'], { queryParams: { returnUrl: '/cart' } });
  });

  it('401 while already on the login page: does nothing a second time', () => {
    router.url = '/login?returnUrl=%2Fcart';

    failWith('/cart', 401);

    expect(authService.logout).not.toHaveBeenCalled();
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('401 from /auth/login (wrong password): no logout, no redirect, the form handles it', () => {
    failWith('/auth/login', 401);

    expect(authService.logout).not.toHaveBeenCalled();
    expect(router.navigate).not.toHaveBeenCalled();
    expect(toast.info).not.toHaveBeenCalled();
  });

  it('403: shows "not allowed"', () => {
    failWith('/orders/seller', 403);

    expect(toast.error).toHaveBeenCalledWith('You are not allowed to do this.');
    expect(authService.logout).not.toHaveBeenCalled();
  });

  it('5xx: shows a generic server message', () => {
    failWith('/cart', 503);

    expect(toast.error).toHaveBeenCalled();
  });

  it('network error (status 0): shows a connection message', () => {
    failWith('/cart', 0);

    expect(toast.error).toHaveBeenCalled();
  });

  it('409: no toast, the page shows the backend message', () => {
    failWith('/cart/items', 409);

    expect(toast.error).not.toHaveBeenCalled();
    expect(toast.info).not.toHaveBeenCalled();
  });

  it('always passes the original error on to the caller', () => {
    expect(failWith('/cart', 409).status).toBe(409);
    expect(failWith('/cart', 500).status).toBe(500);
  });
});