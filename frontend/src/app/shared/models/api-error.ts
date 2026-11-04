import { HttpErrorResponse } from '@angular/common/http';

export function isHandledGlobally(status: number): boolean {
  return status === 0 || status === 401 || status === 403 || status >= 500;
}

export interface ApiError {
  status: number;
  message: string;
  handledGlobally: boolean;
}

export function toApiError(error: unknown): ApiError {
  if (error instanceof HttpErrorResponse) {
    const backendMessage = typeof error.error?.message === 'string' ? error.error.message : null;
    return {
      status: error.status,
      message: backendMessage ?? (error.status === 0 ? "Can't reach the server" : 'Something went wrong'),
      handledGlobally: isHandledGlobally(error.status),
    };
  }
  return { status: 0, message: 'Something went wrong', handledGlobally: false };
}