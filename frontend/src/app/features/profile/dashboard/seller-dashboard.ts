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
} from '@angular/core';
import { FormControl } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Chart } from 'chart.js';
import { EMPTY } from 'rxjs';

import { SellerSalesStatsService } from '../services/seller-sales-stats.service';
import { SellerSalesStats } from '../models/seller-sales-stats.model';
import { AnalyticsDateRange } from './analytics-date-range';
import { createAnalyticsDashboardState } from './analytics-dashboard-state';
import { createHorizontalBarChart } from './analytics-charts';
import { DashboardDateFilter } from './dashboard-date-filter';

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
  private viewInitialized = false;
  private revenueChart?: Chart<'line', number[], string>;
  private bestSellersChart?: Chart<'bar', number[], string>;

  @ViewChild('revenueChart') private readonly revenueCanvas?: ElementRef<HTMLCanvasElement>;
  @ViewChild('bestSellersChart') private readonly bestSellersCanvas?: ElementRef<HTMLCanvasElement>;

  readonly dateRange = new FormControl<AnalyticsDateRange>('30', { nonNullable: true });
  private readonly state = createAnalyticsDashboardState(
    this.dateRange,
    (from, to) => this.statsService.getStats(from, to),
    this.destroyRef,
    () => this.destroyCharts(),
    () => this.onStatsLoaded(),
  );
  readonly stats = this.state.stats;
  readonly loading = this.state.loading;
  readonly error = this.state.error;

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
      this.bestSellersChart = createHorizontalBarChart(
        this.bestSellersCanvas.nativeElement,
        stats.bestSellers.map((product) => product.name),
        stats.bestSellers.map((product) => product.units),
        '#3fa9dc',
      );
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
