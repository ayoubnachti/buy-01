import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { SellerAnalyticsDashboard } from './seller-dashboard';
import { SellerSalesStatsService } from '../services/seller-sales-stats.service';
import { SellerSalesStats } from '../models/seller-sales-stats.model';

vi.mock('chart.js', () => {
  class ChartMock {
    static register = vi.fn();
    destroy = vi.fn();
  }

  return {
    ArcElement: {},
    BarController: {},
    BarElement: {},
    CategoryScale: {},
    Chart: ChartMock,
    DoughnutController: {},
    Legend: {},
    LinearScale: {},
    LineController: {},
    LineElement: {},
    PointElement: {},
    Tooltip: {},
  };
});

describe('SellerAnalyticsDashboard', () => {
  let fixture: ComponentFixture<SellerAnalyticsDashboard>;
  let component: SellerAnalyticsDashboard;
  let statsService: { getStats: ReturnType<typeof vi.fn> };

  const mockStats: SellerSalesStats = {
    revenue: 275.5,
    unitsSold: 9,
    ordersCount: 4,
    bestSellers: [
      { productId: 'product-1', name: 'Coffee maker', units: 5, revenue: 175.5 },
      { productId: 'product-2', name: 'Tea set', units: 4, revenue: 100 },
    ],
    revenueByDay: [
      { date: '2026-11-03', revenue: 100.5 },
      { date: '2026-11-04', revenue: 175 },
    ],
  };

  function createDashboard(): void {
    fixture = TestBed.createComponent(SellerAnalyticsDashboard);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  beforeEach(async () => {
    statsService = {
      getStats: vi.fn().mockReturnValue(
        of({
          success: true,
          message: 'Statistics retrieved',
          data: mockStats,
        }),
      ),
    };

    await TestBed.configureTestingModule({
      imports: [SellerAnalyticsDashboard],
      providers: [
        provideRouter([]),
        { provide: SellerSalesStatsService, useValue: statsService },
      ],
    }).compileComponents();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('shows seller totals and accessible chart data from the stats response', () => {
    createDashboard();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('$275.50');
    expect(text).toContain('Units sold');
    expect(text).toContain('9');
    expect(text).toContain('Coffee maker');
    expect(text).toContain('2026');
    expect(fixture.nativeElement.querySelectorAll('canvas[tabindex="-1"][aria-label]'))
      .toHaveLength(2);
    expect(text).toContain('Daily revenue for the last 30 days');
    expect(text).toContain('Best-selling products by units sold');
  });

  it('shows the no-sales state and a link to product management when stats are empty', () => {
    statsService.getStats.mockReturnValue(
      of({
        success: true,
        message: 'Statistics retrieved',
        data: {
          revenue: 0,
          unitsSold: 0,
          ordersCount: 0,
          bestSellers: [],
          revenueByDay: [],
        },
      }),
    );
    createDashboard();

    expect(fixture.nativeElement.textContent).toContain('No sales yet');
    expect(fixture.nativeElement.querySelector('a[routerLink="/dashboard"]')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('canvas')).toBeNull();
  });

  it('requests the selected date range and shows a retryable error', () => {
    vi.setSystemTime(new Date('2026-11-04T13:00:00Z'));
    createDashboard();

    component.dateRange.setValue('90');
    expect(statsService.getStats).toHaveBeenLastCalledWith('2026-08-07', '2026-11-04');

    statsService.getStats.mockReturnValue(throwError(() => new Error('offline')));
    component.retryLoad();
    fixture.detectChanges();

    expect(component.error()).toBe(true);
    expect(fixture.nativeElement.textContent).toContain('couldn’t load your sales statistics');
  });
});
