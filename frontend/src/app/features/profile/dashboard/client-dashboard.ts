import { CurrencyPipe } from '@angular/common';
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

import { UserPurchaseStatsService } from '../services/user-purchase-stats.service';
import { UserPurchaseStats } from '../models/user-purchase-stats.model';
import { AnalyticsDateRange } from './analytics-date-range';
import { createAnalyticsDashboardState } from './analytics-dashboard-state';
import { createHorizontalBarChart } from './analytics-charts';
import { DashboardDateFilter } from './dashboard-date-filter';
import { DashboardContentState } from './dashboard-content-state';
import { DashboardStatCard } from './dashboard-stat-card';

@Component({
  selector: 'app-client-dashboard',
  standalone: true,
  imports: [CurrencyPipe, DashboardContentState, DashboardDateFilter, DashboardStatCard, RouterLink],
  templateUrl: './client-dashboard.html',
  styleUrl: './analytics-dashboard.css',
})
export class ClientDashboard implements AfterViewInit, OnDestroy {
  private readonly statsService = inject(UserPurchaseStatsService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly changeDetector = inject(ChangeDetectorRef);
  private viewInitialized = false;
  private productChart?: Chart<'bar', number[], string>;
  private categoryChart?: Chart<'doughnut', number[], string>;

  @ViewChild('productsChart') private readonly productsCanvas?: ElementRef<HTMLCanvasElement>;
  @ViewChild('categoriesChart') private readonly categoriesCanvas?: ElementRef<HTMLCanvasElement>;

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

    if (this.productsCanvas && stats.mostBought.length > 0) {
      this.productChart?.destroy();
      this.productChart = createHorizontalBarChart(
        this.productsCanvas.nativeElement,
        stats.mostBought.map((product) => product.name),
        stats.mostBought.map((product) => product.units),
        '#1fae86',
      );
    }

    if (this.categoriesCanvas && stats.topCategories.length > 0) {
      this.categoryChart?.destroy();
      this.categoryChart = new Chart(this.categoriesCanvas.nativeElement, {
        type: 'doughnut',
        data: {
          labels: stats.topCategories.map((category) => category.category),
          datasets: [
            {
              data: stats.topCategories.map((category) => category.units),
              backgroundColor: ['#1fae86', '#3fa9dc', '#f5a623', '#748ffc', '#f06595'],
              borderWidth: 0,
            },
          ],
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: { legend: { position: 'bottom' } },
        },
      });
    }
  }

  private destroyCharts(): void {
    this.productChart?.destroy();
    this.categoryChart?.destroy();
    this.productChart = undefined;
    this.categoryChart = undefined;
  }
}
