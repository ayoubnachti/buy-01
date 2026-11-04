import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';

import { AuthService } from '../services/auth.service';
import { roleGuard } from './role.guard';

describe('roleGuard', () => {
  it('redirects a client attempting to access a seller route', () => {
    const urlTree = {};
    TestBed.configureTestingModule({
      providers: [
        {
          provide: AuthService,
          useValue: { user: () => ({ id: 'client-1', role: 'CLIENT' }) },
        },
        {
          provide: Router,
          useValue: { createUrlTree: vi.fn().mockReturnValue(urlTree) },
        },
      ],
    });

    const result = TestBed.runInInjectionContext(() => roleGuard('SELLER')({} as never, {} as never));

    expect(result).toBe(urlTree);
    expect(TestBed.inject(Router).createUrlTree).toHaveBeenCalledWith(['/']);
  });
});
