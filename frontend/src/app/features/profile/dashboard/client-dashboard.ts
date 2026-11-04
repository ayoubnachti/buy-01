import { CurrencyPipe } from '@angular/common';
import {
  Component,
  ElementRef,
  ViewChild,
  inject,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { Chart } from 'chart.js';

import { UserPurchaseStatsService } from '../services/user-purchase-stats.service';
import { UserPurchaseStats } from '../models/user-purchase-stats.model';
import { createHorizontalBarChart } from './analytics-charts';
import { AnalyticsDashboardBase } from './analytics-dashboard-base';
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
export class ClientDashboard extends AnalyticsDashboardBase<UserPurchaseStats> {
  private readonly statsService = inject(UserPurchaseStatsService);

  @ViewChild('productsChart') private readonly productsCanvas?: ElementRef<HTMLCanvasElement>;
  @ViewChild('categoriesChart') private readonly categoriesCanvas?: ElementRef<HTMLCanvasElement>;

  constructor() {
    super();
    this.initializeDashboard((from, to) => this.statsService.getStats(from, to));
  }

  protected renderCharts(): void {
    const stats = this.stats();
    if (!stats || stats.ordersCount === 0) {
      return;
    }

    const productsCanvas = this.productsCanvas;
    if (productsCanvas && stats.mostBought.length > 0) {
      this.replaceChart('products', () =>
        createHorizontalBarChart(
          productsCanvas.nativeElement,
          stats.mostBought.map((product) => product.name),
          stats.mostBought.map((product) => product.units),
          '#1fae86',
        ),
      );
    }

    const categoriesCanvas = this.categoriesCanvas;
    if (categoriesCanvas && stats.topCategories.length > 0) {
      this.replaceChart(
        'categories',
        () =>
          new Chart(categoriesCanvas.nativeElement, {
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
          }),
      );
    }
  }
}
