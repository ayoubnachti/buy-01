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
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import {
  ArcElement,
  BarController,
  BarElement,
  CategoryScale,
  Chart,
  DoughnutController,
  Legend,
  LinearScale,
  Tooltip,
} from 'chart.js';
import { EMPTY, Subject, catchError, map, merge, startWith, switchMap, tap } from 'rxjs';

import { UserPurchaseStatsService } from '../services/user-purchase-stats.service';
import { UserPurchaseStats } from '../models/user-purchase-stats.model';

type DateRange = '30' | '90' | 'all';

Chart.register(
  ArcElement,
  BarController,
  BarElement,
  CategoryScale,
  DoughnutController,
  Legend,
  LinearScale,
  Tooltip,
);

@Component({
  selector: 'app-client-dashboard',
  standalone: true,
  imports: [CurrencyPipe, ReactiveFormsModule, RouterLink],
  templateUrl: './client-dashboard.html',
  styleUrl: './client-dashboard.css',
})
export class ClientDashboard implements AfterViewInit, OnDestroy {
  private readonly statsService = inject(UserPurchaseStatsService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly changeDetector = inject(ChangeDetectorRef);
  private readonly retry = new Subject<void>();
  private viewInitialized = false;
  private productChart?: Chart<'bar', number[], string>;
  private categoryChart?: Chart<'doughnut', number[], string>;

  @ViewChild('productsChart') private readonly productsCanvas?: ElementRef<HTMLCanvasElement>;
  @ViewChild('categoriesChart') private readonly categoriesCanvas?: ElementRef<HTMLCanvasElement>;

  readonly dateRange = new FormControl<DateRange>('30', { nonNullable: true });
  readonly stats = signal<UserPurchaseStats | null>(null);
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
          const dates = this.getDateRange(range);
          return this.statsService.getStats(dates.from, dates.to).pipe(
            map((response) => {
              if (!response.success || !response.data) {
                throw new Error('The purchase statistics response was invalid.');
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

  private getDateRange(range: DateRange): { from?: string; to?: string } {
    if (range === 'all') {
      return {};
    }

    const today = new Date();
    const to = new Date(Date.UTC(today.getUTCFullYear(), today.getUTCMonth(), today.getUTCDate()));
    const from = new Date(to);
    from.setUTCDate(from.getUTCDate() - Number(range) + 1);

    return {
      from: from.toISOString().slice(0, 10),
      to: to.toISOString().slice(0, 10),
    };
  }

  private renderCharts(): void {
    const stats = this.stats();
    if (!stats || stats.ordersCount === 0) {
      return;
    }

    if (this.productsCanvas && stats.mostBought.length > 0) {
      this.productChart?.destroy();
      this.productChart = new Chart(this.productsCanvas.nativeElement, {
        type: 'bar',
        data: {
          labels: stats.mostBought.map((product) => product.name),
          datasets: [
            {
              data: stats.mostBought.map((product) => product.units),
              backgroundColor: '#1fae86',
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
