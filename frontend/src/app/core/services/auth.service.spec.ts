import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { AuthService } from './auth.service';

function base64Url(value: object): string {
  return btoa(JSON.stringify(value)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

function jwt(payload: object): string {
  return `${base64Url({ alg: 'HS256', typ: 'JWT' })}.${base64Url(payload)}.signature`;
}

const inOneHour = () => Math.floor(Date.now() / 1000) + 3600;
const anHourAgo = () => Math.floor(Date.now() / 1000) - 3600;

describe('AuthService', () => {
  let service: AuthService;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(AuthService);
  });

  afterEach(() => localStorage.clear());

  it('reads a token whose payload uses base64url characters (- and _)', () => {
    const token = jwt({ sub: '>>>???', role: 'CLIENT', exp: inOneHour() });
    expect(token.split('.')[1]).toMatch(/[-_]/);      // proves this token would break plain atob

    service.setUserFromToken(token);

    expect(service.user()).toEqual({ id: '>>>???', role: 'CLIENT' });
    expect(localStorage.getItem('jwt')).toBe(token);
  });

  it('treats an expired token as logged out, and does not store it', () => {
    service.setUserFromToken(jwt({ sub: 'u1', role: 'CLIENT', exp: anHourAgo() }));

    expect(service.user()).toBeNull();
    expect(localStorage.getItem('jwt')).toBeNull();
  });

  it('never stores a token it cannot read', () => {
    service.setUserFromToken('not-a-jwt');

    expect(service.user()).toBeNull();
    expect(localStorage.getItem('jwt')).toBeNull();
  });

  it('restores the user on start from a valid stored token (a refresh keeps you logged in)', () => {
    localStorage.setItem('jwt', jwt({ sub: 's1', role: 'SELLER', exp: inOneHour() }));

    service.initializeUser();

    expect(service.user()).toEqual({ id: 's1', role: 'SELLER' });
  });

  it('starts logged out and clears an expired stored token', () => {
    localStorage.setItem('jwt', jwt({ sub: 'u1', role: 'CLIENT', exp: anHourAgo() }));

    service.initializeUser();

    expect(service.user()).toBeNull();
    expect(localStorage.getItem('jwt')).toBeNull();
  });

  it('does nothing on start without a stored token', () => {
    service.initializeUser();

    expect(service.user()).toBeNull();
  });

  it('logout clears the session without navigating', () => {
    service.setUserFromToken(jwt({ sub: 'u1', role: 'CLIENT', exp: inOneHour() }));
    const before = window.location.href;

    service.logout();

    expect(service.user()).toBeNull();
    expect(localStorage.getItem('jwt')).toBeNull();
    expect(window.location.href).toBe(before);       // no full page reload any more
  });
});