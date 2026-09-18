import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { vi } from 'vitest';

import { AuthService } from '../services/auth.service';
import { clientGuard } from './client.guard';

describe('clientGuard', () => {
  let authService: AuthService;
  let router: Router;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        AuthService,
        {
          provide: Router,
          useValue: {
            createUrlTree: vi.fn(),
          },
        },
      ],
    });

    authService = TestBed.inject(AuthService);
    router = TestBed.inject(Router);
  });

  it('should redirect unauthenticated user to login', () => {
    const loginUrlTree = {} as any;

    vi.spyOn(router, 'createUrlTree').mockReturnValue(loginUrlTree);

    const result = TestBed.runInInjectionContext(() => clientGuard({} as any, {} as any));

    expect(router.createUrlTree).toHaveBeenCalledWith(['/login']);

    expect(result).toBe(loginUrlTree);
  });

  it('should allow CLIENT user', () => {
    authService['userSignal'].set({
      id: '1',
      name: 'Ayoub',
      email: 'ayoub@gmail.com',
      role: 'CLIENT',
    });

    const result = TestBed.runInInjectionContext(() => clientGuard({} as any, {} as any));

    expect(result).toBe(true);
  });

  it('should redirect SELLER user to home', () => {
    const homeUrlTree = {} as any;

    vi.spyOn(router, 'createUrlTree').mockReturnValue(homeUrlTree);

    authService['userSignal'].set({
      id: '2',
      name: 'Seller',
      email: 'seller@gmail.com',
      role: 'SELLER',
    });

    const result = TestBed.runInInjectionContext(() => clientGuard({} as any, {} as any));

    expect(router.createUrlTree).toHaveBeenCalledWith(['/']);

    expect(result).toBe(homeUrlTree);
  });
});
