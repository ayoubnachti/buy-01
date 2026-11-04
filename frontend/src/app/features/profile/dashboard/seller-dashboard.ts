import { CurrencyPipe, DatePipe } from '@angular/common';
import {
  Component,
  ElementRef,
  ViewChild,
  inject,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { Chart } from 'chart.js';

import { SellerSalesStatsService } from '../services/seller-sales-stats.service';
import { SellerSalesStats } from '../models/seller-sales-stats.model';
import { createHorizontalBarChart } from './analytics-charts';
import { AnalyticsDashboardBase } from './analytics-dashboard-base';
import { DashboardDateFilter } from './dashboard-date-filter';
import { DashboardContentState } from './dashboard-content-state';
import { DashboardStatCard } from './dashboard-stat-card';

@Component({
  selector: 'app-seller-analytics-dashboard',
  standalone: true,
  imports: [
    CurrencyPipe,
    DashboardContentState,
    DashboardDateFilter,
    DashboardStatCard,
    DatePipe,
    RouterLink,
  ],
  templateUrl: './seller-dashboard.html',
  styleUrl: './analytics-dashboard.css',
})
export class SellerAnalyticsDashboard extends AnalyticsDashboardBase<SellerSalesStats> {
  private readonly statsService = inject(SellerSalesStatsService);

  @ViewChild('revenueChart') private readonly revenueCanvas?: ElementRef<HTMLCanvasElement>;
  @ViewChild('bestSellersChart') private readonly bestSellersCanvas?: ElementRef<HTMLCanvasElement>;

  constructor() {
    super();
    this.initializeDashboard((from, to) => this.statsService.getStats(from, to));
  }

  protected renderCharts(): void {
    const stats = this.stats();
    if (!stats || stats.ordersCount === 0) {
      return;
    }

    const revenueCanvas = this.revenueCanvas;
    if (revenueCanvas && stats.revenueByDay.length > 0) {
      this.replaceChart(
        'revenue',
        () =>
          new Chart(revenueCanvas.nativeElement, {
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
          }),
      );
    }

    const bestSellersCanvas = this.bestSellersCanvas;
    if (bestSellersCanvas && stats.bestSellers.length > 0) {
      this.replaceChart('best-sellers', () =>
        createHorizontalBarChart(
          bestSellersCanvas.nativeElement,
          stats.bestSellers.map((product) => product.name),
          stats.bestSellers.map((product) => product.units),
          '#3fa9dc',
        ),
      );
    }
  }

  private formatCurrency(value: number): string {
    return new Intl.NumberFormat(undefined, { style: 'currency', currency: 'USD' }).format(value);
  }
}
