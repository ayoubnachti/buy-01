import { CurrencyPipe, DatePipe } from '@angular/common';
import {
  AfterViewInit,
  ChangeDetectorRef,
  Component,
  DestroyRef,
  ElementRef,
  OnDestroy,
  ViewChild,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl } from '@angular/forms';
import { RouterLink } from '@angular/router';
import {
  BarController,
  BarElement,
  CategoryScale,
  Chart,
  LineController,
  LineElement,
  LinearScale,
  PointElement,
  Tooltip,
} from 'chart.js';
import { EMPTY, Subject, catchError, map, merge, startWith, switchMap, tap } from 'rxjs';

import { SellerSalesStatsService } from '../services/seller-sales-stats.service';
import { SellerSalesStats } from '../models/seller-sales-stats.model';
import { AnalyticsDateRange, getAnalyticsDateRange } from './analytics-date-range';
import { DashboardDateFilter } from './dashboard-date-filter';

Chart.register(
  BarController,
  BarElement,
  CategoryScale,
  LinearScale,
  LineController,
  LineElement,
  PointElement,
  Tooltip,
);

@Component({
  selector: 'app-seller-analytics-dashboard',
  standalone: true,
  imports: [CurrencyPipe, DashboardDateFilter, DatePipe, RouterLink],
  templateUrl: './seller-dashboard.html',
  styleUrl: './seller-dashboard.css',
})
export class SellerAnalyticsDashboard implements AfterViewInit, OnDestroy {
  private readonly statsService = inject(SellerSalesStatsService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly changeDetector = inject(ChangeDetectorRef);
  private readonly retry = new Subject<void>();
  private viewInitialized = false;
  private revenueChart?: Chart<'line', number[], string>;
  private bestSellersChart?: Chart<'bar', number[], string>;

  @ViewChild('revenueChart') private readonly revenueCanvas?: ElementRef<HTMLCanvasElement>;
  @ViewChild('bestSellersChart') private readonly bestSellersCanvas?: ElementRef<HTMLCanvasElement>;

  readonly dateRange = new FormControl<AnalyticsDateRange>('30', { nonNullable: true });
  readonly stats = signal<SellerSalesStats | null>(null);
  readonly loading = signal(true);
  readonly error = signal(false);

  constructor() {
    merge(
      this.dateRange.valueChanges.pipe(startWith(this.dateRange.value)),
      this.retry.pipe(map(() => this.dateRange.value)),
    )
      .pipe(
        tap(() => {
          this.loading.set(true);
          this.error.set(false);
          this.stats.set(null);
          this.destroyCharts();
        }),
        switchMap((range) => {
          const dates = getAnalyticsDateRange(range);
          return this.statsService.getStats(dates.from, dates.to).pipe(
            map((response) => {
              if (!response.success || !response.data) {
                throw new Error('The seller sales statistics response was invalid.');
              }
              return response.data;
            }),
            catchError(() => {
              this.error.set(true);
              this.loading.set(false);
              return EMPTY;
            }),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((stats) => {
        this.stats.set(stats);
        this.loading.set(false);
        if (this.viewInitialized) {
          this.changeDetector.detectChanges();
          this.renderCharts();
        }
      });
  }

  ngAfterViewInit(): void {
    this.viewInitialized = true;
    this.renderCharts();
  }

  ngOnDestroy(): void {
    this.destroyCharts();
  }

  retryLoad(): void {
    this.retry.next();
  }

  private renderCharts(): void {
    const stats = this.stats();
    if (!stats || stats.ordersCount === 0) {
      return;
    }

    if (this.revenueCanvas && stats.revenueByDay.length > 0) {
      this.revenueChart?.destroy();
      this.revenueChart = new Chart(this.revenueCanvas.nativeElement, {
        type: 'line',
        data: {
          labels: stats.revenueByDay.map((day) => day.date),
          datasets: [
            {
              data: stats.revenueByDay.map((day) => day.revenue),
              label: 'Revenue',
              borderColor: '#1fae86',
              backgroundColor: 'rgba(31, 174, 134, 0.12)',
              fill: true,
              tension: 0.25,
              pointRadius: 2,
            },
          ],
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            legend: { display: false },
            tooltip: {
              callbacks: {
                label: (context) => `Revenue: ${this.formatCurrency(Number(context.raw))}`,
              },
            },
          },
          scales: {
            y: {
              beginAtZero: true,
              ticks: { callback: (value) => this.formatCurrency(Number(value)) },
            },
          },
        },
      });
    }

    if (this.bestSellersCanvas && stats.bestSellers.length > 0) {
      this.bestSellersChart?.destroy();
      this.bestSellersChart = new Chart(this.bestSellersCanvas.nativeElement, {
        type: 'bar',
        data: {
          labels: stats.bestSellers.map((product) => product.name),
          datasets: [
            {
              data: stats.bestSellers.map((product) => product.units),
              backgroundColor: '#3fa9dc',
              borderRadius: 6,
              maxBarThickness: 36,
            },
          ],
        },
        options: {
          indexAxis: 'y',
          responsive: true,
          maintainAspectRatio: false,
          plugins: { legend: { display: false } },
          scales: { x: { beginAtZero: true, ticks: { precision: 0 } } },
        },
      });
    }
  }

  private formatCurrency(value: number): string {
    return new Intl.NumberFormat(undefined, { style: 'currency', currency: 'USD' }).format(value);
  }

  private destroyCharts(): void {
    this.revenueChart?.destroy();
    this.bestSellersChart?.destroy();
    this.revenueChart = undefined;
    this.bestSellersChart = undefined;
  }
}
