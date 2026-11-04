import { HttpErrorResponse } from '@angular/common/http';
import { toApiError } from './api-error';

describe('toApiError', () => {
  const err = (status: number, error: unknown) => new HttpErrorResponse({ status, error });

  it('uses the backend message for 409 and marks it for the page', () => {
    const e = toApiError(err(409, { success: false, message: 'Order already shipped' }));
    expect(e).toEqual({ status: 409, message: 'Order already shipped', handledGlobally: false });
  });

  it('marks 401, 403, 5xx and network errors as already handled', () => {
    for (const status of [0, 401, 403, 500, 503]) {
      expect(toApiError(err(status, null)).handledGlobally).toBe(true);
    }
  });

  it('falls back to a readable message when the backend sent none', () => {
    expect(toApiError(err(0, null)).message).toBe("Can't reach the server");
    expect(toApiError(err(500, 'plain text')).message).toBe('Something went wrong');
  });

  it('handles something that is not an HTTP error', () => {
    expect(toApiError(new Error('boom'))).toEqual({ status: 0, message: 'Something went wrong', handledGlobally: false });
  });
});