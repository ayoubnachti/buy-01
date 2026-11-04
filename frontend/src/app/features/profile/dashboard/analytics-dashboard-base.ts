import { ChangeDetectorRef, DestroyRef, Directive, inject } from '@angular/core';
import { FormControl } from '@angular/forms';
import { Chart } from 'chart.js';
import { Observable } from 'rxjs';

import { AnalyticsDateRange } from './analytics-date-range';
import { createAnalyticsDashboardState } from './analytics-dashboard-state';

interface AnalyticsResponse<T> {
  success: boolean;
  data: T;
}

@Directive()
export abstract class AnalyticsDashboardBase<T extends { ordersCount: number }> {
  protected readonly destroyRef = inject(DestroyRef);
  private readonly changeDetector = inject(ChangeDetectorRef);
  private readonly charts = new Map<string, Chart>();
  private viewInitialized = false;
  private state!: ReturnType<typeof createAnalyticsDashboardState<T>>;

  readonly dateRange = new FormControl<AnalyticsDateRange>('30', { nonNullable: true });

  readonly stats = () => this.state.stats();
  readonly loading = () => this.state.loading();
  readonly error = () => this.state.error();

  protected initializeDashboard(
    fetchStats: (from?: string, to?: string) => Observable<AnalyticsResponse<T>>,
  ): void {
    this.state = createAnalyticsDashboardState(
      this.dateRange,
      fetchStats,
      this.destroyRef,
      () => this.destroyCharts(),
      () => this.onStatsLoaded(),
    );
  }

  protected replaceChart<C extends Chart>(key: string, create: () => C): C {
    this.charts.get(key)?.destroy();
    const chart = create();
    this.charts.set(key, chart);
    return chart;
  }

  protected abstract renderCharts(): void;

  ngAfterViewInit(): void {
    this.viewInitialized = true;
    this.renderCharts();
  }

  ngOnDestroy(): void {
    this.destroyCharts();
  }

  retryLoad(): void {
    this.state.retryLoad();
  }

  private onStatsLoaded(): void {
    if (this.viewInitialized) {
      this.changeDetector.detectChanges();
      this.renderCharts();
    }
  }

  private destroyCharts(): void {
    this.charts.forEach((chart) => chart.destroy());
    this.charts.clear();
  }
}
