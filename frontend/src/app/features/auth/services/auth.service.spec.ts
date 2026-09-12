import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';

import { AuthService } from './auth.service';

describe('AuthService', () => {

  let service: AuthService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        AuthService,
        provideHttpClient()
      ]
    });

    service = TestBed.inject(AuthService);

    localStorage.clear();
  });

  afterEach(() => {
    localStorage.clear();
    vi.restoreAllMocks();
  });

  it('should remove the JWT from localStorage on logout', () => {

    localStorage.setItem('jwt', 'fake-jwt-token');

    service.logout();

    expect(localStorage.getItem('jwt')).toBeNull();
  });

});