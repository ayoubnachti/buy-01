import { describe, it, expect, beforeEach, afterEach } from 'vitest';

import {
  HttpClient,
  provideHttpClient,
  withInterceptors,
} from '@angular/common/http';

import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';

import { TestBed } from '@angular/core/testing';

import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(
          withInterceptors([authInterceptor])
        ),
        provideHttpClientTesting(),
      ],
    });

    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('should not add Authorization header when JWT does not exist', () => {
    http.get('/test').subscribe();

    const req = httpMock.expectOne('/test');

    expect(
      req.request.headers.has('Authorization')
    ).toBe(false);

    req.flush({});
  });

  it('should add Authorization header when JWT exists', () => {
    localStorage.setItem('jwt', 'my-jwt-token');

    http.get('/test').subscribe();

    const req = httpMock.expectOne('/test');

    expect(
      req.request.headers.get('Authorization')
    ).toBe('Bearer my-jwt-token');

    req.flush({});
  });
});
