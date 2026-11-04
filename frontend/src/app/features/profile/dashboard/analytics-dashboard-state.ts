import { DestroyRef, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl } from '@angular/forms';
import { EMPTY, Observable, Subject, catchError, map, merge, startWith, switchMap, tap } from 'rxjs';

import { getAnalyticsDateRange, AnalyticsDateRange } from './analytics-date-range';

interface AnalyticsResponse<T> {
  success: boolean;
  data: T;
}

export function createAnalyticsDashboardState<T>(
  rangeControl: FormControl<AnalyticsDateRange>,
  fetchStats: (from?: string, to?: string) => Observable<AnalyticsResponse<T>>,
  destroyRef: DestroyRef,
  beforeLoad: () => void,
  afterLoad: () => void,
) {
  const stats = signal<T | null>(null);
  const loading = signal(true);
  const error = signal(false);
  const retry = new Subject<void>();

  merge(
    rangeControl.valueChanges.pipe(startWith(rangeControl.value)),
    retry.pipe(map(() => rangeControl.value)),
  )
    .pipe(
      tap(() => {
        loading.set(true);
        error.set(false);
        stats.set(null);
        beforeLoad();
      }),
      switchMap((range) => {
        const dates = getAnalyticsDateRange(range);
        return fetchStats(dates.from, dates.to).pipe(
          map((response) => {
            if (!response.success || !response.data) {
              throw new Error('The analytics response was invalid.');
            }
            return response.data;
          }),
          catchError(() => {
            error.set(true);
            loading.set(false);
            return EMPTY;
          }),
        );
      }),
      takeUntilDestroyed(destroyRef),
    )
    .subscribe((result) => {
      stats.set(result);
      loading.set(false);
      afterLoad();
    });

  return {
    stats: stats.asReadonly(),
    loading: loading.asReadonly(),
    error: error.asReadonly(),
    retryLoad: () => retry.next(),
  };
}
